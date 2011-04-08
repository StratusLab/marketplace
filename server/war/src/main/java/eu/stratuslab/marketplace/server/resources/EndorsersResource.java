package eu.stratuslab.marketplace.server.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.query.QueryLanguage;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ClientResource;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.data.LocalReference;

/**
 * This resource represents a list of endorsers
 */
public class EndorsersResource extends BaseResource {
    
	private String queryString = "SELECT DISTINCT ?email " +
    " WHERE {" +
    " ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
    " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
    " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . " +
    " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . }";
	
	@Get("html")
    public Representation toHtml() {
		ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>)query(queryString);
		    	        
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", "Endorsers");
        data.put("content", results);
        
        // Load the FreeMarker template
    	Representation listFtl = new ClientResource(LocalReference.createClapReference("/endorsers.ftl")).get();
    	// Wraps the bean with a FreeMarker representation
    	Representation representation = new TemplateRepresentation(listFtl, 
    			data, MediaType.TEXT_HTML);
    	              
        return representation;
    }
	
    /**
     * Returns a listing of all endorsers.
     */
	@Get("xml")
	public Representation toXml() {
		// Generate the right representation according to its media type.
		String results = query(queryString, QueryLanguage.SPARQL);
		StringRepresentation representation = new StringRepresentation(results, 
				MediaType.APPLICATION_XML);
		
		// Returns the XML representation of this document.
		return representation;

	}
}
