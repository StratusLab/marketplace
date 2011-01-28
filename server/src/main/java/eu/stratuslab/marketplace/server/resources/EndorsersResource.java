package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;

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
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.AlreadyExistsException;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Iterator;

/**
 * This resource represents all races in the appliction
 */
public class EndorsersResource extends BaseResource {
    
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
            
            // Create a new query
            String queryString =
                "PREFIX slterms: <http://stratuslab.eu/terms#> " +
                        "SELECT ?email { _:z slterms:email ?email . }";

            Query query = QueryFactory.create(queryString);
            // Execute the query and obtain results
            QueryExecution qe = QueryExecutionFactory.create(query);
            ResultSet results = qe.execSelect();

            for ( Iterator<String> imagesIter = getImages().listNames(); imagesIter.hasNext(); ){
                Element eltItem = d.createElement("image");

                Element eltName = d.createElement("identifier");
                eltName.appendChild(d.createTextNode(imagesIter.next()));
                eltItem.appendChild(eltName);

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
