package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Iterator;

/**
 * This resource represents all races in the appliction
 */
public class ImagesResource extends BaseResource {

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
     * Returns a listing of all registered items.
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
            for ( Iterator<String> imagesIter = getImages().listModels(); imagesIter.hasNext(); ){
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
