package eu.stratuslab.marketplace.server.resources;

import org.restlet.resource.ServerResource;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;

import com.hp.hpl.jena.rdf.model.ModelMaker;

/**
 *  Base resource class that supports common behaviours or attributes shared by
 *  all resources.
 */
public abstract class BaseResource extends ServerResource {

    /**
     * Returns the map of images managed by this application.
     * @return the map of images managed by this application.
    */
    protected ModelMaker getImages() {
           return ((MarketPlaceApplication) getApplication()).getImages();
    }
}
