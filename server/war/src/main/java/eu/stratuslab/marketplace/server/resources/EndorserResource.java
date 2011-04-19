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
import org.restlet.resource.ResourceException;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.data.LocalReference;
import org.restlet.resource.ClientResource;

/**
 * This resource represents a single endorser
 */
public class EndorserResource extends BaseResource {
    
    private String email = null;
    private String queryString = null;
    
   @Override
    protected void doInit() throws ResourceException {
        this.email = (String) getRequest().getAttributes().get("email");
        this.queryString = "SELECT DISTINCT ?email ?subject ?issuer " +
        " WHERE {" +
        " ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
        " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
        " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#subject> ?subject . " +
        " ?endorser <http://mp.stratuslab.eu/slreq#issuer> ?issuer . " +
        " FILTER (?email = \"" + this.email + "\"). }";
    } 

   @Get("html")
   public Representation toHtml() {
	   ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>)query(queryString);

	  Map<String, Object> data = new HashMap<String, Object>();
	   
	   for ( Iterator<HashMap<String, String>> resultsIter = results.listIterator(); resultsIter.hasNext(); ){
		   HashMap<String, String> resultRow = resultsIter.next();

		   data.put("email", resultRow.get("email"));
		   data.put("issuer", resultRow.get("issuer"));
		   data.put("subject", resultRow.get("subject"));
	   }
  
	   // Load the FreeMarker template
	   Representation listFtl = new ClientResource(LocalReference.createClapReference("/endorser.ftl")).get();
	   // Wraps the bean with a FreeMarker representation
	   Representation representation = new TemplateRepresentation(listFtl, 
			   data, MediaType.TEXT_HTML);

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