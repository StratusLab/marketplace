package eu.stratuslab.marketplace.server;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_DBNAME;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_DBPASS;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_DBUSER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_HOST;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MYSQL_PORT;
import static eu.stratuslab.marketplace.server.cfg.Parameter.STORE_TYPE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.TIME_RANGE;

import java.util.logging.Logger;

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
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.resources.MDatumResource;
import eu.stratuslab.marketplace.server.resources.QueryResource;
import eu.stratuslab.marketplace.server.resources.SearchResource;
import eu.stratuslab.marketplace.server.resources.UploadResource;
import eu.stratuslab.marketplace.server.routers.ActionRouter;

public class MarketPlaceApplication extends Application {

    /** The image metadata is stored in a database. */
    private Repository metadata = null;
    private SailBase store = null;
    private String dataDir = null;
    private long timeRange = 60000;
    protected Logger logger = getLogger();

    private freemarker.template.Configuration freeMarkerConfiguration = null;

    public MarketPlaceApplication() {

        setName("StratusLab Market-Place");
        setDescription("Market-Place for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");
        
        getTunnelService().setUserAgentTunnel(true);
        
        dataDir = Configuration.getParameterValue(DATA_DIR);

        timeRange = Configuration.getParameterValueAsLong(TIME_RANGE) * 60000;

        String storeType = Configuration.getParameterValue(STORE_TYPE);
        if (storeType.equals("memory")) {
            store = new MemoryStore();
        } else {
            store = createMysqlStore();
        }

        metadata = new SailRepository(store);
        try {
            metadata.initialize();
        } catch (RepositoryException r) {
            r.printStackTrace();
        }

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

        Directory indexDir = new Directory(getContext(), "war:///");
        indexDir.setNegotiatingContent(false);
        indexDir.setIndexName("index.html");

        // Defines a route for the resource "list of metadata entries"
        router.attach("/metadata", MDataResource.class);
        router.attach("/metadata/", MDataResource.class);
        router.attach("/metadata/{arg1}", MDataResource.class);
        router.attach("/metadata/{arg1}/{arg2}", MDataResource.class);

        // Defines a route for the resource "metadatum"
        router.attach("/metadata/{identifier}/{email}/{date}",
                MDatumResource.class);

        // Defines a route for the resource "endorsers"
        router.attach("/endorsers", EndorsersResource.class);
        router.attach("/endorsers/", EndorsersResource.class);

        // Defines a route for the resource "endorser"
        router.attach("/endorsers/{email}", EndorserResource.class);

        // Defines a route for queries
        router.attach("/query", QueryResource.class);
        router.attach("/query/", QueryResource.class);

        // Defines a route for the upload form
        router.attach("/upload", UploadResource.class);
        router.attach("/upload/", UploadResource.class);

        // Defines a route for the search resource
        router.attach("/search", SearchResource.class);
        router.attach("/search/", SearchResource.class);

        // Defines a router for actions
        TemplateRoute route;
        route = router.attach("/action/", new ActionRouter());
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

        router.attach("/", indexDir);

        return router;
    }

    @Override
    public void stop() {
        if (store != null) {
            try {
                store.shutDown();
            } catch (SailException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the list of registered metadata.
     * 
     * @return the list of registered metadata.
     */
    public Repository getMetadataStore() {
        return this.metadata;
    }

    public String getDataDir() {
        return this.dataDir;
    }

    public long getTimeRange() {
        return this.timeRange;
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
