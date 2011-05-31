package eu.stratuslab.marketplace.server.resources;

import java.util.List;
import java.util.Map;

import org.openrdf.query.QueryLanguage;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

/**
 * This resource represents a single endorser
 */
public class EndorserResource extends BaseResource {

    private String queryString = null;

    private static final String EMAIL_QUERY_TEMPLATE = //
    "SELECT DISTINCT ?email ?subject ?issuer "
            + " WHERE {"
            + " ?x <http://purl.org/dc/terms/identifier>  ?identifier . "
            + " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . "
            + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . "
            + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . "
            + " ?endorser <http://mp.stratuslab.eu/slreq#subject> ?subject . "
            + " ?endorser <http://mp.stratuslab.eu/slreq#issuer> ?issuer . "
            + " FILTER (?email = \"%s\"). }";

    @Override
    protected void doInit() {
        String email = (String) getRequest().getAttributes().get("email");
        queryString = String.format(EMAIL_QUERY_TEMPLATE, email);
    }

    @Get("html")
    public Representation toHtml() {
        List<Map<String, String>> results = query(queryString);

        Map<String, Object> data = createInfoStructure("Endorser");

        for (Map<String, String> resultRow : results) {
            data.put("email", resultRow.get("email"));
            data.put("issuer", resultRow.get("issuer"));
            data.put("subject", resultRow.get("subject"));
        }

        // Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "endorser.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }

    /**
     * Returns endorser details.
     */
    @Get("xml")
    public Representation toXml() {
        String results = query(queryString, QueryLanguage.SPARQL);
        StringRepresentation representation = new StringRepresentation(results,
                MediaType.APPLICATION_XML);

        // Returns the XML representation of this document.
        return representation;
    }
}
