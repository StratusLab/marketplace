package eu.stratuslab.marketplace.server.resources;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
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
    	
		Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", "Metadata");
        data.put("content", stringBuilder.toString());
        
        // Load the FreeMarker template
    	Representation listFtl = new ClientResource(LocalReference.createClapReference("/mdatum.ftl")).get();
    	// Wraps the bean with a FreeMarker representation
    	Representation representation = new TemplateRepresentation(listFtl, 
    			data, MediaType.TEXT_HTML);
       
		return representation;
    }
    
}
