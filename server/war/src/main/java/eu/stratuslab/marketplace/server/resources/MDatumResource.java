/**
 * Created as part of the StratusLab project (http://stratuslab.eu),
 * co-funded by the European Commission under the Grant Agreement
 * INSFO-RI-261552.
 *
 * Copyright (c) 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.talis.rdfwriters.json.JSONJenaWriter;

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

/**
 * This resource represents a metadata entry
 */
public class MDatumResource extends BaseResource {

	private static final String ENCODING = "UTF-8";
	
	private static final String METADATA_ROUTE = "/metadata/";
	
    private String datum = null;
    private String identifier = null;
    private String url = null;        

    @Override
    protected void doInit() {
    	String metadataPath = "";
    	
    	if(getRequest().getAttributes().containsKey("tag")){
    		String email = (String)getRequest().getAttributes().get("email");
    		String tag = getTag((String)getRequest().getAttributes().get("tag"));
			
    		url = getTaggedEntry(tag, email);
    		metadataPath = url;
    	} else {
    		url = getRequest().getResourceRef().getPath();
    		metadataPath = url.substring(METADATA_ROUTE.length());
    	}
    	
    	datum = getMetadatum(metadataPath);
    	identifier = metadataPath.substring(0, metadataPath.indexOf('/'));
    }

    private String getTag(String tag){
    	String decodedTag = "";
    	
    	try {
			decodedTag = URLDecoder.decode((String)getRequest().getAttributes().get("tag"), ENCODING)
					.replaceAll("^\"|\"$", "");
		} catch (UnsupportedEncodingException e) {
			LOGGER.severe("Unable to decode tag query: " + e.getMessage());
		}
    	
    	return decodedTag;
    }
    
    private String getTaggedEntry(String tag, String email){
    	
    	List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        try {
        	String query = getQueryBuilder().buildTagQuery(tag, email);
        	results = query(query);
        } catch(MarketplaceException e){
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }
        
        String identifier = "";
        String created = "";
        
        if (results.size() > 0) {
        	Map<String, String> resultRow = results.get(0);
        	identifier = resultRow.get("identifier");
        	created = resultRow.get("created");
        }
        
        if (identifier == "null" || created == "null")
        	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "metadata entry not found.\n");
        
    	return identifier + "/" + email + "/" + created;
    }
    
    @Get("xml")
    public Representation toXml() {
    	if (this.datum == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "metadata entry not found.\n");
        }
    	
    	StringRepresentation representation = new StringRepresentation(
                new StringBuilder(datum), MediaType.APPLICATION_RDF_XML);

        Disposition disposition = new Disposition();
        disposition.setFilename(identifier + ".xml");
        disposition.setType(Disposition.TYPE_ATTACHMENT);
        representation.setDisposition(disposition);            	

        // Returns the XML representation of this document.
        return representation;
    }

    @Get("json")
    public Representation toJSON() {
        if (this.datum == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "metadata entry not found.\n");
        }
        
        Model rdfModel = ModelFactory.createMemModelMaker()
                .createDefaultModel();
        rdfModel.read(new ByteArrayInputStream((MetadataFileUtils.stripSignature(datum))
				        .getBytes(Charset.forName(ENCODING))), MARKETPLACE_URI);
		
        JSONJenaWriter jenaWriter = new JSONJenaWriter();
        ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
        jenaWriter.write(rdfModel, jsonOut, MARKETPLACE_URI);

        StringRepresentation representation = new StringRepresentation(jsonOut.toString(), 
        		MediaType.APPLICATION_JSON);

        Disposition disposition = new Disposition();
        disposition.setFilename(identifier + ".json");
        representation.setDisposition(disposition);
        disposition.setType(Disposition.TYPE_ATTACHMENT);

        return representation;
    }

    @Get("html")
    public Representation toHtml() {
    	if (this.datum == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "metadata entry not found.\n");
        }
        
        TransformerFactory tFactory = TransformerFactory.newInstance();

        StringBuilder stringBuilder = new StringBuilder();

        try {
            Transformer transformer = tFactory
                    .newTransformer(new javax.xml.transform.stream.StreamSource(
                            getClass().getResourceAsStream("/rdf.xsl")));

            StringWriter xmlOutWriter = new StringWriter();

            transformer.transform(new javax.xml.transform.stream.StreamSource(
                    new StringReader(MetadataFileUtils.stripSignature(datum))),
                    new javax.xml.transform.stream.StreamResult(xmlOutWriter));

            stringBuilder.append(xmlOutWriter.toString());
        } catch (TransformerConfigurationException e) {
            LOGGER.severe("Error parsing metadata stylesheet: " + e.getMessage());
        } catch (TransformerException e) {
        	LOGGER.severe("Error parsing metadata stylesheet: " + e.getMessage());
        }

        Map<String, Object> data = createInfoStructure("Metadata");
        data.put("identifier", identifier);
        data.put("content", stringBuilder.toString());
        data.put("url", url);

        // Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "mdatum.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }   
}
