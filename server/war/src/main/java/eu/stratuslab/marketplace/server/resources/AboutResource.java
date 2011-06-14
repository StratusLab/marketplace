// Created as part of the StratusLab project (http://stratuslab.eu),
package eu.stratuslab.marketplace.server.resources;

import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

public class AboutResource extends BaseResource {

    @Get("html")
    public Representation toHtml() {

        Map<String, Object> info = createInfoStructure(NO_TITLE);

        return createTemplateRepresentation("about.ftl", info, MediaType.TEXT_HTML);
    }

}