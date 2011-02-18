package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        // Generate the right representation according to its media type.

        try {
            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            Map<String, Object> requestAttr =  getRequest().getAttributes();
                                   
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
            			filterPredicate.append(" FILTER (?date = \"" + arg.getValue() + "\"). ");
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
                    " ?x <http://purl.org/dc/terms/identifier>  ?identifier ." +
                    " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ." +
                    " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser ." +
                    " ?endorsement <http://purl.org/dc/terms/created> ?created ." +
                    " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .");
            
            queryString.append(filterPredicate.toString() + " }");
            
            ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>)query(queryString.toString());
            String[] uris = new String[results.size()];
            int i = 0;
            
            for ( Iterator<HashMap<String, String>> resultsIter = results.listIterator(); resultsIter.hasNext(); ){
            	HashMap<String, String> resultRow = resultsIter.next();
                String ref = getRequest().getRootRef().toString();
                String iri = ref + "/metadata/" + resultRow.get("identifier") 
                + "/" + resultRow.get("email") + "/" + resultRow.get("created");
                logger.log(Level.INFO, iri);
                uris[i] = iri;
                i++;
            }
                                    
            Model metadata;
            if(uris.length > 0){
                metadata = getMetadata(uris);
            } else {
            	metadata = createModel(null, "");
            }
                                   
            StringRepresentation representation =
                new StringRepresentation(new StringBuffer(modelToString(metadata)),
                                         MediaType.APPLICATION_RDF_XML);
            
            // Returns the XML representation of this document.
            return representation;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

}
