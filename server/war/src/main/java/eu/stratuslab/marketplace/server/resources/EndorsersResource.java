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

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import static eu.stratuslab.marketplace.server.utils.SparqlUtils.EMAIL_QUERY;
import eu.stratuslab.marketplace.server.MarketplaceException;

/**
 * This resource represents a list of endorsers
 */
public class EndorsersResource extends BaseResource {
   
	@Get("html")
    public Representation toHtml() {
		String cn = "CN=";
		
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        try {
        	results = query(EMAIL_QUERY);
        } catch(MarketplaceException e){
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }
        
        for(int i = 0; i < results.size(); i++){
        	Map<String, String> resultRow = results.get(i);
        	String subject = resultRow.get("subject");
        	String name = subject.substring(subject.indexOf(cn)+cn.length(), 
        			subject.indexOf(",", subject.indexOf(cn)+cn.length()));
        	resultRow.put("name", name);
        	results.set(i, resultRow);
        }
        Map<String, Object> data = createInfoStructure("Endorsers");
        data.put("content", results);

        // Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "endorsers.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }

    /**
     * Returns a listing of all endorsers.
     */
    @Get("xml")
    public Representation toXml() {
        // Generate the right representation according to its media type.
        String results = "";
		try {
			results = queryResultsAsXml(EMAIL_QUERY);
		} catch (MarketplaceException e) {
			LOGGER.severe(e.getMessage());
		}
        StringRepresentation representation = new StringRepresentation(results,
                MediaType.APPLICATION_XML);

        // Returns the XML representation of this document.
        return representation;
    }
    
}
