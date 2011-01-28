package eu.stratuslab.marketplace.server.resources;

import org.restlet.resource.ServerResource;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;

import com.hp.hpl.jena.query.DataSource;

/**
 *  Base resource class that supports common behaviours or attributes shared by
 *  all resources.
 */
public abstract class BaseResource extends ServerResource {

    /**
     * Returns the map of images managed by this application.
     * @return the map of images managed by this application.
    */
    protected DataSource getImages() {
           return ((MarketPlaceApplication) getApplication()).getImages();
    }
}
