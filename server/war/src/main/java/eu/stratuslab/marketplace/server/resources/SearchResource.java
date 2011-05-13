package eu.stratuslab.marketplace.server.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import eu.stratuslab.marketplace.server.utils.Schema;

public class SearchResource extends BaseResource {

    private final static String BASE_QUERY = "SELECT DISTINCT ?identifier ?email ?created "
            + "WHERE { ";

    @Get("html")
    public Representation toHtml() {
        String query = createQuery();

        List<Map<String, String>> results = query(query);
        HashMap<String, HashMap<String, Object>> root = new HashMap<String, HashMap<String, Object>>();

        for (Map<String, String> resultRow : results) {

            String identifier = resultRow.get("identifier");
            String endorser = resultRow.get("email");
            String created = resultRow.get("created");

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
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "metadata.ftl", data, MediaType.TEXT_HTML);

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

    private String createQuery() {
        String query = BASE_QUERY;
        HashMap<String, String> where = new HashMap<String, String>();
        StringBuffer filter = new StringBuffer();
        String queryString = "";

        where.put("identifier", Schema.IDENTIFIER.getWhere());
        where.put("endorsement", Schema.ENDORSEMENT.getWhere());
        where.put("endorser", Schema.ENDORSER.getWhere());
        where.put("created", Schema.ENDORSEMENT_CREATED.getWhere());
        where.put("email", Schema.EMAIL.getWhere());
        
        Form queryForm = getRequest().getResourceRef().getQueryAsForm();

        if(queryForm.getNames().contains("q")) {
           queryString = queryForm.getFirstValue("q");
        } else {
                for ( int i = 0; i < (queryForm.size() / 2); i++){
                        if (i > 0) {
                            queryString += "+" + queryForm.getFirstValue("qname" + i).trim()
                            + ":" + queryForm.getFirstValue("value" + i, "null").trim();
                        } else {
                                queryString = queryForm.getFirstValue("qname").trim()
                                + ":" + queryForm.getFirstValue("value", "null").trim();
                        }
                }
        }

        if(queryString != null && queryString != ""){
                StringTokenizer st=new StringTokenizer(queryString,"+");
                while(st.hasMoreTokens()){
                        String searchTerm = st.nextToken();
                        StringTokenizer term = new StringTokenizer(searchTerm, ":");

                        if(term.countTokens() == 2){
                            String qname = term.nextToken().trim();
                            String value = term.nextToken().trim();
                            if (!value.equals("null")){
                                for(Schema s: Schema.values()){
                                       if(s.getQName().equals(qname)){
                                                where.put(qname, s.getWhere());
                                                filter.append(" " + s.getFilter(value));
                                       }
                                }
                           }
                        } else { //text search
                                String value = term.nextToken();
                                where.put(Schema.DESCRIPTION.getQName(), Schema.DESCRIPTION.getWhere());
                                filter.append(" " + "FILTER REGEX(str(?" + Schema.DESCRIPTION.getQName()
                                    + "), \"" + value + "\", \"i\") .");
                        }
                }

                StringBuffer whereString = new StringBuffer();
                for (Map.Entry<String, String> entry : where.entrySet()){
                	whereString.append(" " + entry.getValue());
                }
                	                	
                query += whereString.toString() + filter.toString();
        }

        query = query + "}";

        return query;
    }

}
