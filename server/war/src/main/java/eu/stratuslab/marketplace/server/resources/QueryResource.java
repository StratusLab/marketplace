package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/**
 * This resource represents a query on the rdf data
 */
public class QueryResource extends BaseResource {

    @Get("html")
    public Representation toHtml() {
        Representation representation = null;

        Form queryForm = getRequest().getResourceRef().getQueryAsForm();
        String queryString = queryForm.getFirstValue("query");
        String resultString = "";
        try {
            if (queryString != null && !"".equals(queryString)) {
                if ((queryString.indexOf("FROM") >= 0)
                        || (queryString.indexOf("GRAPH") >= 0)
                        || (queryString.indexOf("INSERT") >= 0)
                        || (queryString.indexOf("CONSTRUCT") >= 0)) {
                    throw new ResourceException(
                            Status.CLIENT_ERROR_BAD_REQUEST,
                            "query not allowed.");
                }

                QueryParser parser = QueryParserUtil
                        .createParser(QueryLanguage.SPARQL);
                parser.parseQuery(queryString, MARKETPLACE_URI);

                List<Map<String, String>> results = query(queryString);
                if (results.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();

                    stringBuilder.append("<table border=\"1\">");
                    stringBuilder.append("<tr>");
                    for (String key : results.get(0).keySet()) {
                        stringBuilder.append("<td>" + key + "</td>");
                    }
                    stringBuilder.append("</tr>");

                    for (Map<String, String> resultRow : results) {
                        stringBuilder.append("<tr>");
                        for (Map.Entry<String, String> entry : resultRow
                                .entrySet()) {
                            stringBuilder.append("<td>" + entry.getValue()
                                    + "</td>");
                        }
                        stringBuilder.append("</tr>");
                    }

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
            String results = "";

            if (queryString != null && !"".equals(queryString)) {
                QueryParser parser = QueryParserUtil
                        .createParser(QueryLanguage.SPARQL);
                parser.parseQuery(queryString, MARKETPLACE_URI);

                results = getResults(queryString, "sparql");
            }

            representation = new StringRepresentation(results,
                    MediaType.APPLICATION_XML);
        } catch (MalformedQueryException e) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            representation = generateErrorRepresentation(e.getMessage(), "1");
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

}
