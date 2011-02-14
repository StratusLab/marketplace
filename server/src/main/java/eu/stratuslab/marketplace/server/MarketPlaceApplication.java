package eu.stratuslab.marketplace.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.SailException;
import org.openrdf.sail.rdbms.mysql.MySqlStore;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import eu.stratuslab.marketplace.server.resources.EndorserResource;
import eu.stratuslab.marketplace.server.resources.EndorsersResource;
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.resources.MDatumResource;

public class MarketPlaceApplication extends Application {

    /** The image metadata is stored in a database. */
    private Repository metadata = null;   
    private MySqlStore sqlStore = null;    
    protected Logger logger = getLogger();
    
    public MarketPlaceApplication() {
        setName("StratusLab Market-Place");
        setDescription("Market-Place for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");
        /*
        String mysqlDb = getCurrent().getContext().getParameters().getFirstValue("mysql.dbname");
        String mysqlHost = getContext().getParameters().getFirstValue("mysql.host");
        int mysqlPort = Integer.parseInt(getContext().getParameters().getFirstValue("mysql.port"));
        String mysqlUser = getContext().getParameters().getFirstValue("mysql.dbuser");
        String mysqlPass = getContext().getParameters().getFirstValue("mysql.dbpass");
        */
                
        this.sqlStore = new MySqlStore();
		sqlStore.setDatabaseName("marketplace");
		sqlStore.setServerName("localhost");
		sqlStore.setPortNumber(3306);
		sqlStore.setUser("sesame");
		sqlStore.setPassword("sesame");
	    
        this.metadata = new SailRepository(sqlStore);
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
        
        // Defines a route for the resource "metadatum"
        router.attach("/metadata/{identifier}/{email}/{date}", MDatumResource.class);
        // Defines a route for the resource "endorsers"
        router.attach("/endorsers", EndorsersResource.class);
        router.attach("/endorsers/", EndorsersResource.class);
        
        // Defines a route for the resource "endorser"
        router.attach("/endorsers/{email}", EndorserResource.class); 

        return router;
    }

    @Override
    public void stop(){
        if(this.sqlStore != null){
        	try {
				this.sqlStore.shutDown();
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

}
