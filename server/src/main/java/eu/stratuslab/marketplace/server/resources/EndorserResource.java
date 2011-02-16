package eu.stratuslab.marketplace.server.resources;

import org.openrdf.query.QueryLanguage;
import org.restlet.data.Form;
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
        // Generate the right representation according to its media type.
    	Form form = getRequest().getResourceRef().getQueryAsForm();
		String format = (form.getFirstValue("format") != null) ? 
				form.getFirstValue("format") : "xml";
    	    	    	
    	String queryString = "SELECT ?email ?subject ?issuer " +
        " WHERE {" +
        " ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
        " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
        " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#subject> ?subject . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#issuer> ?issuer . " +
        " FILTER (?email = \"" + this.email + "\"). }";
    	
        String results = query(queryString, QueryLanguage.SPARQL, format);
    	StringRepresentation representation;
    	if(format.equals("json")){
    	    representation = new StringRepresentation(results, MediaType.APPLICATION_JSON);
    	} else {
    	    representation = new StringRepresentation(results, MediaType.APPLICATION_XML);
    	}

    	// Returns the XML representation of this document.
    	return representation;
    }
}
