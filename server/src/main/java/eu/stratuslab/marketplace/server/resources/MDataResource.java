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
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
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

        ModelMaker mk = ModelFactory.createMemModelMaker();
        Model datum = mk.createDefaultModel();
        datum.read(entity.getStream(), "");
        datum.setNsPrefix( "slterm", "http://stratuslab.eu/terms#" );
        datum.setNsPrefix( "dcterm", "http://purl.org/dc/terms/" );

        String identifier = ((datum.listStatements(
                              new SimpleSelector(null, DCTerms.identifier,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String endorser = ((datum.listStatements(
                              new SimpleSelector(null, datum.createProperty("http://stratuslab.eu/terms#", "email"),
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
            boolean where = false;
            StringBuffer wherePredicate = new StringBuffer();
            
            String identifier = form.getFirstValue("identifier");
            if (identifier != null){
            	if(!where){
            	    wherePredicate.append(" WHERE identifier = \"" + identifier + "\" ");
            	} else {
            		wherePredicate.append(" AND identifier = \"" + identifier + "\" ");
            	}
            	where = true;
            } else {
            	metadataQuery = false;
            }
            
            String endorser = form.getFirstValue("email");
            if (endorser != null){
            	if(!where){
            	    wherePredicate.append(" WHERE email = \"" + endorser + "\" ");
            	} else {
            		wherePredicate.append(" AND email = \"" + endorser + "\" ");
            	}
            	where = true;
            } else {
            	metadataQuery = false;
            }
            
            String created = form.getFirstValue("created");
            if (created != null){
            	if(!where){
            	    wherePredicate.append(" WHERE created = \"" + created + "\" ");
            	} else {
            		wherePredicate.append(" AND created = \"" + created + "\" ");
            	}
            } else {
            	metadataQuery = false;
            }
                             
            StringBuffer queryString = new StringBuffer("SELECT identifier, created, description, email " +
                                 " FROM {} dcterms:identifier {identifier}, " +
                                 " {} dcterms:created {created}, " +
                                 " {} dcterms:description {description}, " +
                                 " {} slterms:email {email} ");
            
            if(where){
            	queryString.append(wherePredicate.toString());
            }
            
            queryString.append(" USING NAMESPACE dcterms = <http://purl.org/dc/terms/>, " +
                                 " slterms = <http://stratuslab.eu/terms#>");
            
            StringRepresentation representation;
            if(metadataQuery){
            	String ref = getRequest().getRootRef().toString();
            	String iri = ref + "/metadata/" + identifier + "/" + endorser + "/" + created;

            	representation = new StringRepresentation(new StringBuffer(modelToString(getMetadatum(iri))), 
            			MediaType.APPLICATION_RDF_XML);
            } else {
            	String results = query(queryString.toString(), QueryLanguage.SERQL, format);

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
