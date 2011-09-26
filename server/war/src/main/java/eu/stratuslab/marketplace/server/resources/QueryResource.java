package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
import org.restlet.resource.ResourceException;

import com.hp.hpl.jena.sparql.lang.ParserSPARQL11;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;

import java.util.logging.Logger;

/**
 * This resource represents a query on the rdf data
 */
public class QueryResource extends BaseResource {
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	@Get("html")
    public Representation toHtml() {
        Representation representation = null;

        Form queryForm = getRequest().getResourceRef().getQueryAsForm();
        String queryString = queryForm.getFirstValue("query");
        String resultString = "";
        try {
        	
            if (queryString != null && !"".equals(queryString)) {
                
                //check that query is allowed.
        	    if(!validQuery(queryString)){
        	    			throw new ResourceException(
                                    Status.CLIENT_ERROR_BAD_REQUEST,
                                    "query not allowed.");
        	    }
                
                List<Map<String, String>> results = query(queryString);
                int noOfResults = results.size();
                if (noOfResults > 0) {
                    StringBuilder stringBuilder = new StringBuilder();

                    stringBuilder.append("<table cellpadding=\"0\" cellspacing=\"0\" " +
                    		"border=\"0\" class=\"display\" id=\"resultstable\">");
                    stringBuilder.append("<thead><tr>");
                    for (String key : results.get(0).keySet()) {
                        stringBuilder.append("<th>" + key + "</th>");
                    }
                    stringBuilder.append("</tr></thead>");
                    stringBuilder.append("<tbody>");

                    for (Map<String, String> resultRow : results) {
                        stringBuilder.append("<tr>");
                        for (Map.Entry<String, String> entry : resultRow
                                .entrySet()) {
                            stringBuilder.append("<td>" + entry.getValue()
                                    + "</td>");
                        }
                        stringBuilder.append("</tr>");
                    }
                    stringBuilder.append("</tbody>");
                    stringBuilder.append("</table>");
                    resultString = stringBuilder.toString();
                } else {
                    resultString = "";
                }
                                
            } else {
                queryString = "";
                resultString = "";
            }
            // return representation;
            Map<String, Object> query = createInfoStructure("Query");
            query.put("query", queryString);
            query.put("results", resultString);

            // Load the FreeMarker template
            // Wraps the bean with a FreeMarker representation
            representation = createTemplateRepresentation("Query.ftl", query,
                    MediaType.TEXT_HTML);
        } catch (MalformedQueryException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e
                    .getMessage());
        }

        return representation;
    }

    @Get("xml")
    public Representation toXml() {
        Representation representation = null;

        Form queryForm = getRequest().getResourceRef().getQueryAsForm();
        String queryString = queryForm.getFirstValue("query");

        try {
            String results = "";

            if (queryString != null && !"".equals(queryString)) {
            	//check that query is allowed.
        	    if(!validQuery(queryString)){
        	    			throw new ResourceException(
                                    Status.CLIENT_ERROR_BAD_REQUEST,
                                    "query not allowed.");
        	    }
                
                results = getResults(queryString, "sparql");
            }

            representation = new StringRepresentation(results,
                    MediaType.APPLICATION_XML);
        } catch (MalformedQueryException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e
                    .getMessage());
        }
        // Returns the XML representation of this document.
        return representation;
    }

    private String getResults(String query, String queryLanguage) {
        // Generate the right representation according to its media type.
        String results = "";
        if (queryLanguage.equals("sparql")) {
            results = query(query, QueryLanguage.SPARQL);
        } else if (queryLanguage.equals("serql")) {
            results = query(query, QueryLanguage.SERQL);
        }

        return results;
    }
    
    private boolean validQuery(String queryString)
    throws MalformedQueryException {
    	boolean valid = true;

    	//make sure query is valid for sesame.
        QueryParser parser = QueryParserUtil
                .createParser(QueryLanguage.SPARQL);
        parser.parseQuery(queryString, MARKETPLACE_URI);
    	
    	ParserSPARQL11 jenaParser = new ParserSPARQL11();
    	Query selectQuery = jenaParser.parse(new Query(), queryString);

    	if(!selectQuery.isSelectType() || 
    			selectQuery.hasDatasetDescription()){
    		valid = false;
    	} else {
    		Element queryElement = selectQuery.getQueryPattern();
    		if(queryElement.getClass() == ElementGroup.class){
    			List<Element> elements = ((ElementGroup)queryElement).getElements();
    			for(Element e: elements){
    				if(e.getClass() == ElementNamedGraph.class){
    					valid = false;
    				}
    			}
    		}
    	}

    	return valid;
    }

}
