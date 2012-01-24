package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.lang.ParserSPARQL11;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;

import eu.stratuslab.marketplace.server.MarketplaceException;

/**
 * This resource represents a query on the rdf data
 */
public class QueryResource extends BaseResource {
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	@Get("html")
    public Representation toHtml() {
        Representation representation = null;

        Form queryForm = getRequest().getResourceRef().getQueryAsForm();
        String queryInSparql = queryForm.getFirstValue("query");
        String resultsInHtml = "";
        try {
        	
            if (queryInSparql != null && !"".equals(queryInSparql)) {
                
                //check that query is allowed.
        	    if(!validQuery(queryInSparql)){
        	    			throw new ResourceException(
                                    Status.CLIENT_ERROR_BAD_REQUEST,
                                    "query not allowed.");
        	    }
                
                List<Map<String, String>> results = new ArrayList<Map<String, String>>();
                
                try {	
                	results = query(queryInSparql);
                } catch(MarketplaceException e){
                	LOGGER.severe(e.getMessage());
                }
                
                int noOfResults = results.size();
                if (noOfResults > 0) {
                	resultsInHtml = buildResultsHtml(results);                	
                    
                } else {
                    resultsInHtml = "";
                }
                                
            } else {
                queryInSparql = "";
                resultsInHtml = "";
            }
            // return representation;
            Map<String, Object> query = createInfoStructure("Query");
            query.put("query", queryInSparql);
            query.put("results", resultsInHtml);

            // Load the FreeMarker template
            // Wraps the bean with a FreeMarker representation
            representation = createTemplateRepresentation("query.ftl", query,
                    MediaType.TEXT_HTML);
        } catch (MalformedQueryException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e
                    .getMessage());
        }

        return representation;
    }

    private String buildResultsHtml(List<Map<String, String>> results) {
    	StringBuilder html = new StringBuilder();

        html.append("<table cellpadding=\"0\" cellspacing=\"0\" " +
        		"border=\"0\" class=\"display\" id=\"resultstable\">");
        html.append("<thead><tr>");
        for (String key : results.get(0).keySet()) {
            html.append("<th>" + key + "</th>");
        }
        html.append("</tr></thead>");
        html.append("<tbody>");

        for (Map<String, String> resultRow : results) {
            html.append("<tr>");
            for (Map.Entry<String, String> entry : resultRow
                    .entrySet()) {
                html.append("<td>" + entry.getValue()
                        + "</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody>");
        html.append("</table>");
        
        return html.toString();
	}

	@Get("xml")
    public Representation toXml() {
        Representation representation = null;

        Form query = getRequest().getResourceRef().getQueryAsForm();
        String queryInSparql = query.getFirstValue("query");

        try {
            String results = "";

            if (queryInSparql != null && !"".equals(queryInSparql)) {
            	//check that query is allowed.
        	    if(!validQuery(queryInSparql)){
        	    			throw new ResourceException(
                                    Status.CLIENT_ERROR_BAD_REQUEST,
                                    "query not allowed.");
        	    }
                
                try {
					results = queryResultsAsString(queryInSparql);
				} catch (MarketplaceException e) {
					LOGGER.severe(e.getMessage());
				}
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
       
    private boolean validQuery(String query)
    throws MalformedQueryException {
    	boolean valid = true;

    	//make sure query is valid for sesame.
        QueryParser parser = QueryParserUtil
                .createParser(QueryLanguage.SPARQL);
        parser.parseQuery(query, MARKETPLACE_URI);
    	
    	ParserSPARQL11 jenaParser = new ParserSPARQL11();
    	Query select = jenaParser.parse(new Query(), query);

    	if(!select.isSelectType() || 
    			select.hasDatasetDescription()){
    		valid = false;
    	} else {
    		Element queryElement = select.getQueryPattern();
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
