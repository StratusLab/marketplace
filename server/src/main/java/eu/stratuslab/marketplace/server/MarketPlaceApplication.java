package eu.stratuslab.marketplace.server;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import java.sql.Connection ;
import java.sql.DriverManager ;
import java.sql.SQLException ;

import com.hp.hpl.jena.sdb.Store ;
import com.hp.hpl.jena.sdb.StoreDesc ;
import com.hp.hpl.jena.sdb.sql.JDBC ;
import com.hp.hpl.jena.sdb.sql.SDBConnection ;
import com.hp.hpl.jena.sdb.store.DatabaseType ;
import com.hp.hpl.jena.sdb.store.LayoutType ;
import com.hp.hpl.jena.sdb.store.StoreFactory ;

import eu.stratuslab.marketplace.server.resources.ImagesResource;
import eu.stratuslab.marketplace.server.resources.ImageResource;
import eu.stratuslab.marketplace.server.resources.EndorsersResource;

public class MarketPlaceApplication extends Application {

    /** The image metadata is stored in a database. */
    private Store images = null;   
 
    public MarketPlaceApplication() {
        setName("StratusLab Market-Place");
        setDescription("Market-Place for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");

        String jdbcURL = "jdbc:mysql://localhost/sdb";
        JDBC.loadDriverMySQL();
        Connection jdbcConnection = null;

           try {
               jdbcConnection = DriverManager.getConnection(jdbcURL, "sdb", "sdb");
           } catch(SQLException sql){}

           if (jdbcConnection != null) {
               SDBConnection conn = new SDBConnection(jdbcConnection);
               StoreDesc storeDesc = new StoreDesc(LayoutType.LayoutTripleNodesHash,DatabaseType.MySQL);
               this.images = StoreFactory.create(storeDesc, conn);
               this.images.getTableFormatter().create();
           }

           this.images.close();
    }

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */     
    @Override
    public Restlet createInboundRoot() {
        // Create a router Restlet that defines routes.
        Router router = new Router(getContext());
        // Defines a route for the resource "list of images"
        router.attach("/images", ImagesResource.class);
        // Defines a route for the resource "image"
        router.attach("/images/{identifier}", ImageResource.class);
        // Defines a route for the resource "endorsers"
        router.attach("/endorsers", EndorsersResource.class);
       
        return router;
    }

    /**
     * Returns the list of registered images.
     * 
     * @return the list of registered images.
    */
    public Store getImageStore() {
           return this.images;
    }
}
