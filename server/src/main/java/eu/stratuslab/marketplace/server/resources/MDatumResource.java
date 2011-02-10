package eu.stratuslab.marketplace.server.resources;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.ext.xml.DomRepresentation;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This resource preprents an individual race instance
 */
public class MDatumResource extends BaseResource {

    //private String iri;
    private Model datum = null;
    
    @Override
    protected void doInit() throws ResourceException {
        //String iri = (String) getRequest().getAttributes().get("identifier");
        String iri = getRequest().getResourceRef().toString(); 
        this.datum = getMetadatum(iri);
    }
    
    @Get("xml")
    public Representation toXml() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(bytes);

        datum.setNsPrefix( "slterm", "http://stratuslab.eu/terms#" );
        datum.setNsPrefix( "dcterm", "http://purl.org/dc/terms/" );
        RDFWriter writer = datum.getWriter();
        writer.write(datum, out, "");
        StringRepresentation representation =
                new StringRepresentation(new StringBuffer(bytes.toString()),
                                         MediaType.APPLICATION_RDF_XML);

        // Returns the XML representation of this document.
        return representation;
    }
  
}
