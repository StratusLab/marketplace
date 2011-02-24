package eu.stratuslab.marketplace.server.resources;

import org.openrdf.query.QueryLanguage;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * This resource represents a single endorser
 */
public class EndorserResource extends BaseResource {
    
    private String email = null;

   @Override
    protected void doInit() throws ResourceException {
        this.email = (String) getRequest().getAttributes().get("email");
    } 

    /**
     * Returns endorser details.
     */
    @Get("xml")
    public Representation toXml() {
        String queryString = "SELECT DISTINCT ?email ?subject ?issuer " +
        " WHERE {" +
        " ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
        " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
        " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#subject> ?subject . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#issuer> ?issuer . " +
        " FILTER (?email = \"" + this.email + "\"). }";
    	
        String results = query(queryString, QueryLanguage.SPARQL);
    	StringRepresentation representation = new StringRepresentation(results, 
    			MediaType.APPLICATION_XML);
    	
    	// Returns the XML representation of this document.
    	return representation;
    }
}
