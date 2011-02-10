package eu.stratuslab.marketplace.server;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.TemplateRoute;

import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.resources.MDatumResource;
import eu.stratuslab.marketplace.server.resources.EndorsersResource;
import eu.stratuslab.marketplace.server.resources.EndorserResource;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class MarketPlaceApplication extends Application {

    /** The image metadata is stored in a database. */
    private Repository metadata = null;   
 
    public MarketPlaceApplication() {
        setName("StratusLab Market-Place");
        setDescription("Market-Place for StratusLab images");
        setOwner("StratusLab");
        setAuthor("Stuart Kenny");

        metadata = new SailRepository(new MemoryStore());
        try {
            metadata.initialize();
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
        TemplateRoute routeQ1 = router.attach("/metadata/?{query}", MDataResource.class);
        routeQ1.setMatchingQuery(true);

        TemplateRoute routeQ2 = router.attach("/metadata?{query}", MDataResource.class);
        routeQ2.setMatchingQuery(true);

        // Defines a route for the resource "metadatum"
        router.attach("/metadata/{identifier}/{email}/{date}", MDatumResource.class);
        // Defines a route for the resource "endorsers"
        router.attach("/endorsers", EndorsersResource.class);
        // Defines a route for the resource "endorser"
        router.attach("/endorsers/{email}", EndorserResource.class); 

        return router;
    }

    /**
     * Returns the list of registered images.
     * 
     * @return the list of registered images.
    */
    public Repository getMetadataStore() {
           return metadata;
    }

}
