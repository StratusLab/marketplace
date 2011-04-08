package eu.stratuslab.marketplace.server.resources;

import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;

/**
 * This resource allows a new metadata entry to be uploaded from the web
 * interface.
 */
public class UploadResource extends BaseResource {

    @Get("html")
    public Representation toHtml() {

        LocalReference ref = LocalReference.createClapReference("/upload.ftl");
        Representation uploadFtl = new ClientResource(ref).get();

        return new TemplateRepresentation(uploadFtl, MediaType.TEXT_HTML);
    }

}
