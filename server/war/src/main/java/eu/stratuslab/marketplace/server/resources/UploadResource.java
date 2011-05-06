package eu.stratuslab.marketplace.server.resources;

import java.util.HashMap;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * This resource allows a new metadata entry to be uploaded from the web
 * interface.
 */
public class UploadResource extends BaseResource {

    @Get("html")
    public Representation toHtml() {

        return createTemplateRepresentation("/upload.ftl",
                new HashMap<String, Object>(), MediaType.TEXT_HTML);
    }

}
