package eu.stratuslab.marketplace.server.resources;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.shared.DoesNotExistException;

/**
 * This resource preprents an individual race instance
 */
public class ImageResource extends BaseResource {

    Model image;
    private String identifier;
    
    @Override
    protected void doInit() throws ResourceException {
        // Get the "identifier" attribute value taken from the URI template
        // /{identifier}.
        this.identifier = (String) getRequest().getAttributes().get("identifier");
         
        // Get the image directly from the "persistence layer".
        try {
           this.image = getImages().openModel(identifier, true);
        } catch (DoesNotExistException dne){
           this.image = null;
        }
        setExisting(this.image != null);
    }

    /**
     * Handle DELETE requests.
     */
    @Delete
    public void removeImage() {
        if (image != null) {
            // Remove the image from the list.
            try {
               getImages().removeModel(identifier);
            } catch(DoesNotExistException dne){}
        }

        // Tells the client that the request has been successfully fulfilled.
        setStatus(Status.SUCCESS_NO_CONTENT);
    }

    /**
     * Handle PUT requests.
     * 
     * @throws IOException
     */
    @Put
    public void storeImage(Representation entity) throws IOException {
        // The PUT request updates or creates the resource.
        if (image == null) {
            image = getImages().openModel(identifier, false);
        }

        // Update the rdf.
        image.removeAll();
        image.read(entity.getStream(), "");
        
        if (getImages().hasModel(identifier)) {
            setStatus(Status.SUCCESS_CREATED);
        } else {
            setStatus(Status.SUCCESS_OK);
        }
    }

    @Get("xml")
    public Representation toXml() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(bytes);

        RDFWriter writer = image.getWriter();
        writer.write(image, out, "");
        StringRepresentation representation = 
                new StringRepresentation(new StringBuffer(bytes.toString()), 
                                         MediaType.APPLICATION_RDF_XML);

        // Returns the XML representation of this document.
        return representation;
    }
  
}
