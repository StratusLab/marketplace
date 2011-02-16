package eu.stratuslab.marketplace.server.resources;

import org.openrdf.query.QueryLanguage;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;


/**
 * This resource represents a list of endorsers
 */
public class EndorsersResource extends BaseResource {
    
    /**
     * Returns a listing of all endorsers.
     */
	@Get("xml")
	public Representation toXml() {
		// Generate the right representation according to its media type.
		Form form = getRequest().getResourceRef().getQueryAsForm();
		String format = (form.getFirstValue("format") != null) ? 
				form.getFirstValue("format") : "xml";

		String queryString = "SELECT ?email " +
                " WHERE {" +
                " ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
                " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
                " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . " +
                " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . }";
		
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
