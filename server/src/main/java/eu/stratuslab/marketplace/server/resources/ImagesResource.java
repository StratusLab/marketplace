package eu.stratuslab.marketplace.server.resources;

import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.data.Status;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.shared.AlreadyExistsException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;

/**
 * This resource represents all races in the appliction
 */
public class ImagesResource extends BaseResource {

    /**
     * Handle POST requests: create a new item.
     */
    @Post
    public Representation acceptItem(Representation entity) throws IOException {
        Representation result = null;

        ModelMaker mk = ModelFactory.createMemModelMaker();
        Model image = mk.createDefaultModel();
        image.read(entity.getStream(), "");
        image.setNsPrefix( "slterm", "http://stratuslab.eu/terms#" );
        image.setNsPrefix( "dcterm", "http://purl.org/dc/terms/" );

        String identifier = ((image.listStatements(
                              new SimpleSelector(null, DCTerms.identifier,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String endorser = ((image.listStatements(
                              new SimpleSelector(null, image.createProperty("http://stratuslab.eu/terms#", "email"),
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String created = ((image.listStatements(
                              new SimpleSelector(null, DCTerms.created,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();

        String ref = getRequest().getResourceRef().toString();
        String iri = ref + "/" + identifier + "/" + endorser + "/" + created;

	try{
            storeImage(iri, image);

           // Set the response's status and entity
            setStatus(Status.SUCCESS_CREATED);
            Representation rep = new StringRepresentation("Image created",
                 MediaType.TEXT_PLAIN);
            // Indicates where is located the new resource.
            rep.setLocationRef(getRequest().getResourceRef().getIdentifier() + "/"
                 + identifier + "/" + endorser + "/" + created);
            result = rep;
        } catch(AlreadyExistsException are){
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            result = generateErrorRepresentation("Image " + identifier
                + " already exists.", "1");   
        }

        return result;
    }

    /**
     * Generate an XML representation of an error response.
     * 
     * @param errorMessage
     *            the error message.
     * @param errorCode
     *            the error code.
    */
    private Representation generateErrorRepresentation(String errorMessage,
            String errorCode) {
        DomRepresentation result = null;
        // This is an error
        // Generate the output representation
        try {
            result = new DomRepresentation(MediaType.TEXT_XML);
            // Generate a DOM document representing the list of
            // items.
            Document d = result.getDocument();
            Element eltError = d.createElement("error");
            Element eltCode = d.createElement("code");
            eltCode.appendChild(d.createTextNode(errorCode));
            eltError.appendChild(eltCode);
            Element eltMessage = d.createElement("message");
            eltMessage.appendChild(d.createTextNode(errorMessage));
            eltError.appendChild(eltMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Returns a listing of all registered images.
     */
    @Get("xml")
    public Representation toXml() {
        // Generate the right representation according to its media type.
        try {
            DomRepresentation representation = new DomRepresentation(
                    MediaType.TEXT_XML);

            // Generate a DOM document representing the list of
            // items.
            Document d = representation.getDocument();
            Element r = d.createElement("images");
            d.appendChild(r);

            String queryString = "PREFIX slterms: <http://stratuslab.eu/terms#> " +
                        "SELECT ?identifier ?endorser ?created " +
                        "WHERE {" +
                        " ?y <http://purl.org/dc/terms/identifier> ?identifier . " +
                        " ?z slterms:email ?endorser . " +
                        " ?x <http://purl.org/dc/terms/created> ?created . }";
            
            ArrayList results = (ArrayList)query(queryString);

            for ( int i = 0; i < results.size(); i++ ){
                Element eltItem = d.createElement("image");

                Element eltIdentifier = d.createElement("identifier");
                String identifier = (String)(((HashMap)results.get(i))).get("identifier");
                eltIdentifier.appendChild(d.createTextNode(identifier));
                eltItem.appendChild(eltIdentifier);
        
                Element eltEndorser = d.createElement("endorser");
                String endorser = (String)(((HashMap)results.get(i))).get("endorser");
                eltEndorser.appendChild(d.createTextNode(endorser));
                eltItem.appendChild(eltEndorser);

                Element eltCreated = d.createElement("created");
                String created = (String)(((HashMap)results.get(i))).get("created");
                eltCreated.appendChild(d.createTextNode(created));
                eltItem.appendChild(eltCreated);

                r.appendChild(eltItem);
            }
 
            d.normalizeDocument();

            // Returns the XML representation of this document.
            return representation;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
