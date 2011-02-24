package eu.stratuslab.marketplace.server.resources;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * This resource represents a metadata entry
 */
public class MDatumResource extends BaseResource {

    private String datum = null;
    
    @Override
    protected void doInit() throws ResourceException {
        String iri = getRequest().getResourceRef().getPath().substring(10);
        
        this.datum = getMetadatum(getDataDir() + "/" + iri + ".xml");
    }
    
    @Get("xml")
    public Representation toXml() {
        StringRepresentation representation =
                new StringRepresentation(new StringBuffer(datum),
                		MediaType.APPLICATION_RDF_XML);

        // Returns the XML representation of this document.
        return representation;
    }
  
}
