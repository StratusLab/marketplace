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
    private String email = null;
    
    private static final String HISTORY_QUERY_TEMPLATE = //
    	"SELECT ?identifier ?created ?description ?location ?deprecated ?email " +
    	"WHERE { ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
    	"OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#location> ?location . } " +
    	"OPTIONAL { ?x <http://purl.org/dc/terms/description> ?description . } " +
    	"OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated . } " +
    	"?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
    	"?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser; " +
    	"<http://purl.org/dc/terms/created> ?created . " +
    	"?endorser <http://mp.stratuslab.eu/slreq#email> ?email . " +
    	"FILTER (?email = \"%s\") }";
    
    @Override
    protected void doInit() {
        this.email = (String) getRequest().getAttributes().get("email");
        queryString = String.format(HISTORY_QUERY_TEMPLATE, email);
    }

    @Get("html")
    public Representation toHtml() {
        List<Map<String, String>> results = query(queryString);

        Map<String, Object> data = createInfoStructure("History for " + this.email);
        data.put("content", results);

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
