package eu.stratuslab.marketplace.server.resources;

import java.util.logging.Level;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * This resource represents a metadata entry
 */
public class MDatumResource extends BaseResource {

    private  Model datum = null;
    
    @Override
    protected void doInit() throws ResourceException {
        String iri = getRequest().getResourceRef().toString();
        this.datum = getMetadatum(iri);
    }
    
    @Get("xml")
    public Representation toXml() {
        StringRepresentation representation =
                new StringRepresentation(new StringBuffer(modelToString(datum)),
                                         MediaType.APPLICATION_RDF_XML);

        // Returns the XML representation of this document.
        return representation;
    }
  
}
