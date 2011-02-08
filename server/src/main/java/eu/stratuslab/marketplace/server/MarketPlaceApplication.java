package eu.stratuslab.marketplace.server;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import com.hp.hpl.jena.sdb.Store ;
import com.hp.hpl.jena.sdb.SDBFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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

        images = SDBFactory.connectStore("/tmp/sdb-mysql-innodb.ttl");
        images.getTableFormatter().create();
        Model sdbModelDefault = SDBFactory.connectDefaultModel(this.images);
        sdbModelDefault.add((ModelFactory.createMemModelMaker()).createDefaultModel());
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
           return images;
    }

}
