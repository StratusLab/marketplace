package eu.stratuslab.marketplace.server.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
		String base = getRequest().getResourceRef().toString();
    	if(base.endsWith("/")){
    		base = base.substring(0, base.length() - 1);
    	}
		ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>)query(queryString);
		
    	StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<table border=\"0\">");
        for ( Iterator<HashMap<String, String>> resultsIter = results.listIterator(); resultsIter.hasNext(); ){
        	HashMap<String, String> resultRow = resultsIter.next();
        	
            stringBuilder.append("<tr>");
            stringBuilder.append("<td>");
            stringBuilder.append("<a href=\"" + base + "/" + resultRow.get("email") + "\">" 
            		+ resultRow.get("email") + "</a>");
            stringBuilder.append("</td>");
            stringBuilder.append("</tr>");
        }
        stringBuilder.append("</table>");
        
        Map<String, String> data = new HashMap<String, String>();
        data.put("title", "Endorsers");
        data.put("content", stringBuilder.toString());
        
        // Load the FreeMarker template
    	Representation listFtl = new ClientResource(LocalReference.createClapReference("/List.ftl")).get();
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
