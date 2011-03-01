package eu.stratuslab.marketplace.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.rdbms.mysql.MySqlStore;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import eu.stratuslab.marketplace.server.resources.EndorserResource;
import eu.stratuslab.marketplace.server.resources.EndorsersResource;
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.resources.MDatumResource;
import eu.stratuslab.marketplace.server.resources.QueryResource;

public class MarketPlaceApplication extends Application {

    /** The image metadata is stored in a database. */
    private Repository metadata = null;   
    private SailBase store = null;    
    private String dataDir = null;
    protected Logger logger = getLogger();
    
    public MarketPlaceApplication() {
        setName("StratusLab Market-Place");
        setDescription("Market-Place for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");      

        InputStream input = null;
        
        // Read properties file.
        Properties properties = new Properties();
        try {
            properties.load(input = new FileInputStream("/etc/stratuslab/marketplace.cfg"));
            input.close();
        } catch (IOException e) {
        	try {
				input.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
        }
      
        String storeType = properties.getProperty("store.type", "memory");
        this.dataDir = properties.getProperty("data.dir", "/var/lib/stratuslab/metadata");
        
        if(storeType.equals("memory")){
        	this.store = new MemoryStore();	
        } else {
        	String mysqlDb = properties.getProperty("mysql.dbname", "marketplace"); 
        	String mysqlHost = properties.getProperty("mysql.host", "localhost");
        	int mysqlPort = Integer.parseInt(properties.getProperty("mysql.port", "3306"));
        	String mysqlUser = properties.getProperty("mysql.dbuser", "sesame"); 
        	String mysqlPass = properties.getProperty("mysql.dbpass", "sesame");

        	store = new MySqlStore();
        	((MySqlStore)store).setDatabaseName(mysqlDb);
        	((MySqlStore)store).setServerName(mysqlHost);
        	((MySqlStore)store).setPortNumber(mysqlPort);
        	((MySqlStore)store).setUser(mysqlUser);
        	((MySqlStore)store).setPassword(mysqlPass);
        }
        
        this.metadata = new SailRepository(store);
        try {
            this.metadata.initialize();
        } catch(RepositoryException r){
            r.printStackTrace();
        }
    }

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */     
    @Override
    public Restlet createInboundRoot() {
        // Create a router Restlet that defines routes.
        Router router = new Router(getContext());

        // Defines a route for the resource "list of metadata entries"
        router.attach("/metadata", MDataResource.class);
        router.attach("/metadata/", MDataResource.class);
        router.attach("/metadata/{arg1}", MDataResource.class);
        router.attach("/metadata/{arg1}/{arg2}", MDataResource.class);
        // Defines a route for the resource "metadatum"
        router.attach("/metadata/{identifier}/{email}/{date}", MDatumResource.class);
        // Defines a route for the resource "endorsers"
        router.attach("/endorsers", EndorsersResource.class);
        router.attach("/endorsers/", EndorsersResource.class);
        
        // Defines a route for the resource "endorser"
        router.attach("/endorsers/{email}", EndorserResource.class); 
        
        //Defines a route for queries
        router.attach("/query", QueryResource.class);
        router.attach("/query/", QueryResource.class);

        return router;
    }

    @Override
    public void stop(){
        if(this.store != null){
        	try {
				this.store.shutDown();
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

}
