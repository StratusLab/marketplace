package eu.stratuslab.marketplace.server.resources;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

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
        String iri = getRequest().getResourceRef().getPath();
        iri = iri.substring(iri.indexOf("metadata") + 9);
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
    
    @Get("html")
    public Representation toHtml() {
    	// Retrieve resource
    	TransformerFactory tFactory = TransformerFactory.newInstance();
    	
    	StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<html>");
        stringBuilder
                .append("<head><title>Metadata</title></head>");
        stringBuilder.append("<body bgcolor=white>");
    	
        try {
			 Transformer transformer =
			  tFactory.newTransformer
			     (new javax.xml.transform.stream.StreamSource
			        (getClass().getResourceAsStream( "/rdf.xsl" )));
			 
			 StringWriter xmlOutWriter = new StringWriter();
			 
			 transformer.transform
		      (new javax.xml.transform.stream.StreamSource
		            (new StringReader(datum)),
		       new javax.xml.transform.stream.StreamResult
		            ( xmlOutWriter ));
			 
			 stringBuilder.append(xmlOutWriter.toString());
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	stringBuilder.append("</body>");
        stringBuilder.append("</html>");
        
        Representation representation = (new StringRepresentation(stringBuilder
                .toString(), MediaType.TEXT_HTML));
		return representation;
    }
    
}
