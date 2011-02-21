package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * This resource represents a list of all Metadata entries
 */
public class MDataResource extends BaseResource {

    /**
     * Handle POST requests: register new Metadata entry.
     */
    @Post
    public Representation acceptMetadatum(Representation entity) throws IOException {
        Representation result = null;
        
        Model datum = createModel(entity.getStream(), "http://mp.stratuslab.eu/");
        
        String identifier = "";
        StmtIterator iter = datum.listStatements(new SimpleSelector(null, DCTerms.identifier, (RDFNode) null));
        while (iter.hasNext()) {
        	Statement s = iter.nextStatement();
        	if(s.getSubject().getNameSpace() != null 
        			&& s.getSubject().getNameSpace().startsWith("http://mp.stratuslab.eu/#")){
        		identifier = (s.getObject().toString());
        	}
        }
        
        String endorser = ((datum.listStatements(
                              new SimpleSelector(null, datum.createProperty("http://mp.stratuslab.eu/slreq#", "email"),
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String created = ((datum.listStatements(
                              new SimpleSelector(null, DCTerms.created,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();

        String ref = getRequest().getResourceRef().toString();
        String iri = ref + "/" + identifier + "/" + endorser + "/" + created;

        storeMetadatum(iri, datum);
        // Set the response's status and entity
        setStatus(Status.SUCCESS_CREATED);
        Representation rep = new StringRepresentation("Metadata entry created",
            MediaType.TEXT_PLAIN);
        // Indicates where is located the new resource.
        rep.setLocationRef(getRequest().getResourceRef().getIdentifier() + "/"
             + identifier + "/" + endorser + "/" + created);
        result = rep;

        return result;
    }
        
    /**
     * Returns a listing of all registered metadata or a particular entry if specified.
     */
    @Get("xml")
    public Representation toXml() {
        StringRepresentation representation =
                new StringRepresentation(getMetadata(),
                                         MediaType.APPLICATION_XML);
            
            // Returns the XML representation of this document.
            return representation;
    }

    private StringBuffer getMetadata() {
    	// Generate the right representation according to its media type.

        try {
            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            Map<String, Object> requestAttr =  getRequest().getAttributes();
            boolean dateSearch = false;          
            
            StringBuffer filterPredicate = new StringBuffer();
                        
            for ( Iterator<Map.Entry<String,Object>> argsIter = requestAttr.entrySet().iterator(); argsIter.hasNext(); ){
            	Map.Entry<String,Object> arg = argsIter.next();
            	String key = arg.getKey();
            	if(!key.startsWith("org.restlet")){
            		switch(classifyArg((String)arg.getValue())){
            		case ARG_EMAIL:
            			filterPredicate.append(" FILTER (?email = \"" + arg.getValue() + "\"). ");
            			break;
            		case ARG_DATE:
            			dateSearch = true;
            			filterPredicate.append(" FILTER (?created = \"" + arg.getValue() + "\"). ");
            			break;
            		case ARG_OTHER:
            			filterPredicate.append(" FILTER (?identifier = \"" + arg.getValue() + "\"). ");
            			break;
            		default:
            			break;
            		}
            	}
            }
            
            filterPredicate.append(formToString(queryForm));
                      
            StringBuffer queryString = new StringBuffer("SELECT DISTINCT ?identifier ?email ?created " +
                    " WHERE {" +
                    " ?x <http://purl.org/dc/terms/identifier>  ?identifier; " +
                    " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ." +
                    " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;" +
                    " <http://purl.org/dc/terms/created> ?created ." +
                    " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .");
            
            queryString.append(filterPredicate.toString());
                        
            if(!dateSearch && !queryForm.getNames().contains("created")) {
            	queryString.append(" OPTIONAL { " +
            			" ?lx <http://purl.org/dc/terms/identifier>  ?lidentifier; " +
            			" <http://mp.stratuslab.eu/slreq#endorsement> ?lendorsement ." +
            			" ?lendorsement <http://mp.stratuslab.eu/slreq#endorser> ?lendorser;" +
            			" <http://purl.org/dc/terms/created> ?latestcreated ." +
            			" ?lendorser <http://mp.stratuslab.eu/slreq#email> ?lemail ." +
            			" FILTER (?lidentifier = ?identifier) ." +
            			" FILTER (?lemail = ?email) ." +
            	" FILTER (?latestcreated > ?created) . } FILTER (!bound (?lendorsement))");
            }
            
            queryString.append(" }");
                         
            ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>)query(queryString.toString());
            String[] uris = new String[results.size()];
            int i = 0;
            
            for ( Iterator<HashMap<String, String>> resultsIter = results.listIterator(); resultsIter.hasNext(); ){
            	HashMap<String, String> resultRow = resultsIter.next();
                String ref = getRequest().getRootRef().toString();
                String iri = ref + "/metadata/" + resultRow.get("identifier") 
                + "/" + resultRow.get("email") + "/" + resultRow.get("created");
                uris[i] = iri;
                i++;
            }
                                    
            StringBuffer output = new StringBuffer("<Metadata>");
            
            if(uris.length > 0){
            	for(int j = 0; j < uris.length; j++){
            		output.append(modelToString(getMetadatum(uris[j])));
            	}
            } else {
            	output.append(modelToString(createModel(null, "")));
            }
                                   
            output.append("</Metadata>");
                        
            // Returns the XML representation of this document.
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
