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
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.talis.rdfwriters.json.JSONJenaWriter;

import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

/**
 * This resource represents a metadata entry
 */
public class MDatumResource extends BaseResource {

    private String datum = null;
    private String identifier = null;
        
    @Override
    protected void doInit() {
        String iri = getRequest().getResourceRef().getPath();
        iri = iri.substring(iri.indexOf("metadata") + 9);
        this.datum = getMetadatum(getDataDir() + "/" + iri + ".xml");
        this.identifier = iri.substring(0, iri.indexOf("/"));
    }

    @Get("xml")
    public Representation toXml() {
    	if (this.datum == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "metadata entry not found.\n");
        }
    	
    	StringRepresentation representation = new StringRepresentation(
                new StringBuilder(datum), MediaType.APPLICATION_RDF_XML);

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
			        .getBytes()), MARKETPLACE_URI);
		
        JSONJenaWriter jenaWriter = new JSONJenaWriter();
        ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
        jenaWriter.write(rdfModel, jsonOut, MARKETPLACE_URI);

        StringRepresentation representation = new StringRepresentation(jsonOut
                .toString(), MediaType.APPLICATION_JSON);

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
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        Map<String, Object> data = createInfoStructure("Metadata");
        data.put("identifier", this.identifier);
        data.put("content", stringBuilder.toString());

        // Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "mdatum.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }   
}
