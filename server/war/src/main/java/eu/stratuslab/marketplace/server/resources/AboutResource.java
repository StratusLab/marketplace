// Created as part of the StratusLab project (http://stratuslab.eu),
package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

public class AboutResource extends BaseResource {

    @Get("html")
    public Representation toHtml() throws IOException {

        Map<String, Object> info = createInfoStructure(NO_TITLE);

//        return new StringRepresentation("<html><body>hello</body></html>", MediaType.TEXT_HTML);
        TemplateRepresentation rep = createTemplateRepresentation("about.ftl", info, MediaType.TEXT_HTML);
        getLogger().info(rep.getText());
        return new StringRepresentation(rep.getText(), MediaType.TEXT_HTML);
    } 

}