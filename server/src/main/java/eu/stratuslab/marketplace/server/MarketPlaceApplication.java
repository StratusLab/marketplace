package eu.stratuslab.marketplace.server;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.DataSource;

import eu.stratuslab.marketplace.server.resources.ImagesResource;
import eu.stratuslab.marketplace.server.resources.ImageResource;

public class MarketPlaceApplication extends Application {

    /** The list of images is stored in memory. */
    private final DataSource images = DatasetFactory.create();   

 
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
               
        return router;
    }

    /**
     * Returns the list of registered images.
     * 
     * @return the list of registered images.
    */
    public DataSource getImages() {
           return images;
    }
}
