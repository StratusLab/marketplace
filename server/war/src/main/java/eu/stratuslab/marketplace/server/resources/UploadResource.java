package eu.stratuslab.marketplace.server.resources;

import java.util.Map;

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

        Map<String, Object> map = createInfoStructure("Upload");

        return createTemplateRepresentation("upload.ftl",
                map, MediaType.TEXT_HTML);
    }

}
