package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;

import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.data.Status;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


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
            Element r = d.createElement("endorsers");
            d.appendChild(r);
            
            String queryString = "PREFIX slterms: <http://stratuslab.eu/terms#> SELECT ?email { _:z slterms:email ?email . }";

            ArrayList results = (ArrayList)query(queryString);
  
            for ( int i = 0; i < results.size(); i++ ){
                Element eltItem = d.createElement("endorser");

                Element eltName = d.createElement("email");
                String email = (String)(((HashMap)results.get(i))).get("email");
                eltName.appendChild(d.createTextNode(email));
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
