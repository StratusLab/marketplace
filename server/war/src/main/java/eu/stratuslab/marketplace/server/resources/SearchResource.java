package eu.stratuslab.marketplace.server.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Iterator;

import org.restlet.data.Form;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserUtil;

import java.util.logging.Logger;
import java.util.logging.Level;

import eu.stratuslab.marketplace.server.utils.Schema;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

public class SearchResource extends BaseResource {

	private final static String BASE_QUERY = "SELECT DISTINCT ?identifier ?email ?created " +
			"WHERE { ?description <http://purl.org/dc/terms/identifier>  ?identifier; " +
			"<http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
			"?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser; " +
			"<http://purl.org/dc/terms/created> ?created . " +
			"?endorser <http://mp.stratuslab.eu/slreq#email> ?email .";
	
	@Get("html")
    public Representation toHtml() {
		String query = createQuery();
    	
		List<Map<String, String>> results = query(query);
		HashMap<String, HashMap<String, Object>> root = new HashMap<String, HashMap<String, Object>>();

		for (Map<String, String> resultRow : results) {

			String identifier = resultRow.get("identifier");
			String endorser = resultRow.get("email");
			String created = resultRow.get("created");
			logger.log(Level.INFO, identifier + "  " + endorser + " " + created);

			HashMap<String, Object> endorserMap;
			if (root.containsKey(identifier)) {
				endorserMap = root.get(identifier);
			} else {
				endorserMap = new HashMap<String, Object>();
			}

			endorserMap.put(endorser, created);
			root.put(identifier, endorserMap);

		}

		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("title", "Metadata");
		data.put("content", root);

		// Load the FreeMarker template
		Representation listFtl = new ClientResource(
				LocalReference.createClapReference("/metadata.ftl")).get();
		// Wraps the bean with a FreeMarker representation
		Representation representation = new TemplateRepresentation(listFtl,
				data, MediaType.TEXT_HTML);

		return representation;
	}
	
	/**
     * Returns a listing of all registered metadata or a particular entry if
     * specified.
     */
    @Get("xml")
    public Representation toXml() {
    	String query = createQuery();
    	
		List<Map<String, String>> results = query(query);
		List<String> uris = new ArrayList<String>();
        for (Map<String, String> resultRow : results) {

            String iri = resultRow.get("identifier") + "/"
                    + resultRow.get("email") + "/" + resultRow.get("created");
            uris.add(iri);
        }

        StringBuffer output = new StringBuffer(XML_HEADER);

        for (String uri : uris) {
            String datum = getMetadatum(getDataDir() + File.separatorChar + uri
                    + ".xml");
            if (datum.startsWith(XML_HEADER)) {
                datum = datum.substring(XML_HEADER.length());
            }
            output.append(datum);
        }

        // Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(output,
                MediaType.APPLICATION_XML);

        return representation;
    }
	
	private String createQuery(){
		String query = BASE_QUERY;
		StringBuffer where = new StringBuffer();
		StringBuffer filter = new StringBuffer();
		
    	Form queryForm = getRequest().getResourceRef().getQueryAsForm();
    	String queryString = queryForm.getFirstValue("q");
    	
    	if(queryString != null && queryString != ""){
    		StringTokenizer st=new StringTokenizer(queryString,"+");
    		while(st.hasMoreTokens()){
    			String searchTerm = st.nextToken();
    			StringTokenizer term = new StringTokenizer(searchTerm, ":");
    			
    			if(term.countTokens() == 2){
    			    String qname = term.nextToken();
    			    String value = term.nextToken();
    			
    			    for(Schema s: Schema.values()){
    				    if(s.getQName().equals(qname)){
    					    where.append(" " + s.getWhere());
    					    filter.append(" " + s.getFilter(value));
    				    }
    			   }
    			}
    		}
    		
    		query += where.toString() + filter.toString();
    	}
    	    	
    	query = query + "}";
    	
    	return query;
	}
	
}
