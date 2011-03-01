package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserUtil;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

/**
 * This resource represents a query on the rdf data
 */
public class QueryResource extends BaseResource {
	
	@Get("html")
    public Representation toHtml() {
		Representation representation = null;
		
		Form queryForm = getRequest().getResourceRef().getQueryAsForm();
		String queryString = queryForm.getFirstValue("query");
		
		try {
			QueryParser parser = QueryParserUtil.createParser(QueryLanguage.SPARQL);
			parser.parseQuery(queryString, MARKETPLACE_URI);
			
			ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>)query(queryString);

			StringBuilder stringBuilder = new StringBuilder();

			stringBuilder.append("<html>");
			stringBuilder
			.append("<head><title>Results</title></head>");
			stringBuilder.append("<body bgcolor=white>");
			stringBuilder.append("<table border=\"1\">");
			stringBuilder.append("<tr>");
			for(String key : results.get(0).keySet()){
				stringBuilder.append("<td>" + key + "</td>");
			}
			stringBuilder.append("</tr>");

			for ( Iterator<HashMap<String, String>> resultsIter = results.listIterator(); resultsIter.hasNext(); ){
				stringBuilder.append("<tr>");
				HashMap<String, String> resultRow = resultsIter.next();
				for(Map.Entry<String, String> entry : resultRow.entrySet()){
					stringBuilder.append("<td>" + entry.getValue() + "</td>");
				}
				stringBuilder.append("</tr>");

			}
			stringBuilder.append("</table>");
			stringBuilder.append("</body>");
			stringBuilder.append("</html>");

			representation = (new StringRepresentation(stringBuilder
					.toString(), MediaType.TEXT_HTML));

		} catch(MalformedQueryException e){
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            representation = generateErrorRepresentation(e.getMessage(), "1");
		}

		return representation;
	}
	
	@Get("xml")
	public Representation toXml() {
		Representation representation = null;
		
		Form queryForm = getRequest().getResourceRef().getQueryAsForm();
		String queryString = queryForm.getFirstValue("query");
				
		try {
			QueryParser parser = QueryParserUtil.createParser(QueryLanguage.SPARQL);
			parser.parseQuery(queryString, MARKETPLACE_URI);

			String results = getResults(queryString, "sparql");

			representation = new StringRepresentation(results, 
					MediaType.APPLICATION_XML);
		} catch(MalformedQueryException e){
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			representation = generateErrorRepresentation(e.getMessage(), "1");
		}
		// Returns the XML representation of this document.
		return representation;
     }
	
	private String getResults(String query, String queryLanguage){
		// Generate the right representation according to its media type.
		String results = "";
		if(queryLanguage.equals("sparql")){
		    results = query(query, QueryLanguage.SPARQL);
		} else if (queryLanguage.equals("serql")){
			results = query(query, QueryLanguage.SERQL);
		}
		
		return results;
	}
	
}
    