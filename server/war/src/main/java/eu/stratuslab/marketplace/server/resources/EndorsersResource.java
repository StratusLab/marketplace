package eu.stratuslab.marketplace.server.resources;

import java.util.List;
import java.util.Map;

import org.openrdf.query.QueryLanguage;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

/**
 * This resource represents a list of endorsers
 */
public class EndorsersResource extends BaseResource {

    /*private String queryString = "SELECT DISTINCT ?email "
            + " WHERE {"
            + " ?x <http://purl.org/dc/terms/identifier>  ?identifier . "
            + " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . "
            + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . "
            + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . }";
	*/
    private static final String EMAIL_QUERY = //
        "SELECT DISTINCT ?email ?subject ?issuer "
                + " WHERE {"
                + " ?x <http://purl.org/dc/terms/identifier>  ?identifier . "
                + " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . "
                + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . "
                + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . "
                + " ?endorser <http://mp.stratuslab.eu/slreq#subject> ?subject . "
                + " ?endorser <http://mp.stratuslab.eu/slreq#issuer> ?issuer . }";
                
    @Get("html")
    public Representation toHtml() {
        List<Map<String, String>> results = query(EMAIL_QUERY);

        Map<String, Object> data = createInfoStructure("Endorsers");
        data.put("content", results);

        // Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "endorsers.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }

    /**
     * Returns a listing of all endorsers.
     */
    @Get("xml")
    public Representation toXml() {
        // Generate the right representation according to its media type.
        String results = query(EMAIL_QUERY, QueryLanguage.SPARQL);
        StringRepresentation representation = new StringRepresentation(results,
                MediaType.APPLICATION_XML);

        // Returns the XML representation of this document.
        return representation;

    }
}
