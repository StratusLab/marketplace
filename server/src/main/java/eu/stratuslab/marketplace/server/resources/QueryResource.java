package eu.stratuslab.marketplace.server.resources;

import java.util.logging.Level;

import org.openrdf.query.QueryLanguage;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

/**
 * This resource represents a query on the rdf data
 */
public class QueryResource extends BaseResource {
	
	/**
     * Returns a listing of all endorsers.
     */
	@Get("xml")
	public Representation toXml() {
		Form queryForm = getRequest().getResourceRef().getQueryAsForm();
		String queryString = queryForm.getFirstValue("query");
		String queryLanguage = queryForm.getFirstValue("language", "sparql");
		
		logger.log(Level.INFO, queryString);
		
		// Generate the right representation according to its media type.
		String results = "";
		if(queryLanguage.equals("sparql")){
		    results = query(queryString, QueryLanguage.SPARQL);
		} else if (queryLanguage.equals("serql")){
			results = query(queryString, QueryLanguage.SERQL);
		}
		
		StringRepresentation representation = new StringRepresentation(results, 
				MediaType.APPLICATION_XML);
		
		// Returns the XML representation of this document.
		return representation;
     }
	
}
    