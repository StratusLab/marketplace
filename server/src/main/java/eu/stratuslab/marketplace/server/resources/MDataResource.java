package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;
import java.util.logging.Level;

import org.openrdf.query.QueryLanguage;
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
            Form form = getRequest().getResourceRef().getQueryAsForm();
            String format = (form.getFirstValue("format") != null) ? 
                                form.getFirstValue("format") : "xml";
            
            boolean metadataQuery = true;
            boolean filter = false;
            StringBuffer filterPredicate = new StringBuffer();
            
            String identifier = form.getFirstValue("identifier");
            if (identifier != null){
            	filterPredicate.append(" FILTER (?identifier = \"" + identifier + "\"). ");
            	filter = true;
            } else {
            	metadataQuery = false;
            }
            
            String endorser = form.getFirstValue("email");
            if (endorser != null){
            	filterPredicate.append(" FILTER (?email = \"" + endorser + "\"). ");
            	filter = true;
            } else {
            	metadataQuery = false;
            }
            
            String created = form.getFirstValue("created");
            if (created != null){
            	filterPredicate.append(" FILTER (?created = \"" + created + "\"). ");
            	filter = true;
            } else {
            	metadataQuery = false;
            }
                       
            StringBuffer queryString = new StringBuffer("SELECT ?identifier ?email ?created " +
                    " WHERE {" +
                    " ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
                    " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
                    " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . " +
                    " ?endorsement <http://purl.org/dc/terms/created> ?created ." +
                    " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .");
            
            if(filter){
            	queryString.append(filterPredicate.toString() + "}");
            } else {
            	queryString.append("}");
            }
                         
            StringRepresentation representation;
            if(metadataQuery){
            	String ref = getRequest().getRootRef().toString();
            	String iri = ref + "/metadata/" + identifier + "/" + endorser + "/" + created;

            	representation = new StringRepresentation(new StringBuffer(modelToString(getMetadatum(iri))), 
            			MediaType.APPLICATION_RDF_XML);
            } else {
            	String results = query(queryString.toString(), QueryLanguage.SPARQL, format);

            	if(format.equals("json")){
            		representation = new StringRepresentation(results, MediaType.APPLICATION_JSON);
            	} else {
            		representation = new StringRepresentation(results, MediaType.APPLICATION_XML);
            	}
            }
            // Returns the XML representation of this document.
            return representation;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
