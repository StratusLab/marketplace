package eu.stratuslab.marketplace.server;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_DBNAME;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_DBPASS;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_DBUSER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_HOST;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_PORT;
import static eu.stratuslab.marketplace.server.cfg.Parameter.STORE_TYPE;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.openrdf.OpenRDFException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.rdbms.mysql.MySqlStore;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.resources.EndorserResource;
import eu.stratuslab.marketplace.server.resources.EndorsersResource;
import eu.stratuslab.marketplace.server.resources.HomeResource;
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.resources.MDatumResource;
import eu.stratuslab.marketplace.server.resources.QueryResource;
import eu.stratuslab.marketplace.server.resources.SearchResource;
import eu.stratuslab.marketplace.server.resources.UploadResource;
import eu.stratuslab.marketplace.server.resources.AboutResource;
import eu.stratuslab.marketplace.server.routers.ActionRouter;

public class MarketPlaceApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger("org.restlet");

    private static final String MYSQL_URL_MESSAGE = "using mysql datastore: mysql://%s:xxxxxx@%s:%d/%s";

    private static final String MEMORY_STORE_WARNING = "memory store being used; data is NOT persistent";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private ScheduledFuture pingerHandle;
    
    private Repository metadata = null;
    private SailBase store = null;
    private String dataDir = null;
    
    private boolean repositoryLock = false;
    
    private freemarker.template.Configuration freeMarkerConfiguration = null;

    public MarketPlaceApplication() {

        setName("StratusLab Market-Place");
        setDescription("Market-Place for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");

        getTunnelService().setUserAgentTunnel(true);

        dataDir = Configuration.getParameterValue(DATA_DIR);
       
        String storeType = Configuration.getParameterValue(STORE_TYPE);
        if (storeType.equals("memory")) {
            LOGGER.warning(MEMORY_STORE_WARNING);
            store = new MemoryStore();
        } else {
            store = createMysqlStore();
        }

        metadata = new SailRepository(store);
        try {
            metadata.initialize();
        } catch (RepositoryException r) {
            LOGGER.severe("error initializing repository: " + r.getMessage());
        }

        /*
         * Set up a task to check the repository connection is alive
         */
        final Runnable pinger = new Runnable() {
            public void run() { 
            	lockRepository();
            	reInitialize();
            	unlockRepository();
            }
          };
          
          /*
           * Ping the repository once an hour to make sure MySQL does not close the connection
           */
          pingerHandle =
              scheduler.scheduleAtFixedRate(pinger, 3600, 3600, TimeUnit.SECONDS);
    }

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public Restlet createInboundRoot() {

        Context context = getContext();

        // Create the FreeMarker configuration.
        freeMarkerConfiguration = MarketPlaceApplication
                .createFreeMarkerConfig(context);

        // Create a router Restlet that defines routes.
        Router router = new Router(context);

        // Defines a route for the resource "list of metadata entries"
        router.attach("/metadata", MDataResource.class);
        router.attach("/metadata/", MDataResource.class);
        router.attach("/metadata/{arg1}", MDataResource.class);
        router.attach("/metadata/{arg1}/", MDataResource.class);
        router.attach("/metadata/{arg1}/{arg2}", MDataResource.class);
        router.attach("/metadata/{arg1}/{arg2}/", MDataResource.class);

        // Defines a route for the resource "metadatum"
        router.attach("/metadata/{identifier}/{email}/{date}",
                MDatumResource.class);
        router.attach("/metadata/{identifier}/{email}/{date}/",
		MDatumResource.class);

        // Defines a route for the resource "endorsers"
        router.attach("/endorsers", EndorsersResource.class);
        router.attach("/endorsers/", EndorsersResource.class);

        // Defines a route for the resource "endorser"
        router.attach("/endorsers/{email}", EndorserResource.class);
	router.attach("/endorsers/{email}/", EndorserResource.class);

        // Defines a route for queries
        router.attach("/query", QueryResource.class);
        router.attach("/query/", QueryResource.class);

        // Defines a route for the upload form
        router.attach("/upload", UploadResource.class);
        router.attach("/upload/", UploadResource.class);

        // Defines a route for the search resource
        router.attach("/search", SearchResource.class);
        router.attach("/search/", SearchResource.class);

        // Define a route for the about page
        router.attach("/about", AboutResource.class);
        router.attach("/about/", AboutResource.class);
        
        // Defines a router for actions
        TemplateRoute route;
        route = router.attach("/action/", new ActionRouter());
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

        Directory cssDir = new Directory(getContext(), "war:///css");
        cssDir.setNegotiatingContent(false);
        cssDir.setIndexName("index.html");
        router.attach("/css/", cssDir);

        // Unknown root pages get the home page.
        router.attachDefault(HomeResource.class);

        return router;
    }

    private void reInitialize(){
    	if (store != null) {
            try {
                store.shutDown();
            } catch (SailException e) {
                LOGGER.warning("error shutting down repository: "
                        + e.getMessage());
            }
        }
    	
    	try {
            metadata.initialize();
        } catch (RepositoryException r) {
            LOGGER.severe("error initializing repository: " + r.getMessage());
        }
    }
    
    private void lockRepository(){
    	this.repositoryLock = true;
    }
    
    private void unlockRepository(){
    	this.repositoryLock = false;
    }
    
    @Override
    public void stop() {
        if (store != null) {
            try {
                store.shutDown();
            } catch (SailException e) {
                LOGGER.warning("error shutting down repository: "
                        + e.getMessage());
            }
        }
        
        pingerHandle.cancel(true);
    }

    public Repository getMetadataStore() {
    	while(this.repositoryLock){
    		try {
    		    Thread.currentThread().sleep(1000L);
    		} catch(InterruptedException e){}
    	}
        return this.metadata;
    }

    public String getDataDir() {
        return this.dataDir;
    }

    public freemarker.template.Configuration getFreeMarkerConfiguration() {
        return freeMarkerConfiguration;
    }

    private static MySqlStore createMysqlStore() {

        String mysqlDb = Configuration.getParameterValue(MYSQL_DBNAME);
        String mysqlHost = Configuration.getParameterValue(MYSQL_HOST);
        int mysqlPort = Configuration.getParameterValueAsInt(MYSQL_PORT);
        String mysqlUser = Configuration.getParameterValue(MYSQL_DBUSER);
        String mysqlPass = Configuration.getParameterValue(MYSQL_DBPASS);

        LOGGER.info(String.format(MYSQL_URL_MESSAGE, mysqlUser, mysqlHost,
                mysqlPort, mysqlDb));

        MySqlStore mysqlStore = new MySqlStore();
        mysqlStore.setDatabaseName(mysqlDb);
        mysqlStore.setServerName(mysqlHost);
        mysqlStore.setPortNumber(mysqlPort);
        mysqlStore.setUser(mysqlUser);
        mysqlStore.setPassword(mysqlPass);

        return mysqlStore;
    }

    private static freemarker.template.Configuration createFreeMarkerConfig(
            Context context) {

        freemarker.template.Configuration cfg = new freemarker.template.Configuration();
        cfg.setLocalizedLookup(false);

        LocalReference fmBaseRef = LocalReference
                .createClapReference("/freemarker/");
        cfg.setTemplateLoader(new ContextTemplateLoader(context, fmBaseRef));

        return cfg;
    }

}
