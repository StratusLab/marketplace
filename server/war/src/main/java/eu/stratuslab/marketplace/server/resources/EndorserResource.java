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
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import eu.stratuslab.marketplace.server.MarketplaceException;

import static eu.stratuslab.marketplace.server.utils.SparqlUtils.ENDORSER_HISTORY_QUERY_TEMPLATE;

/**
 * This resource represents a single endorser
 */
public class EndorserResource extends BaseResource {

    private String query = null;
    private String email = null;
            
    @Override
    protected void doInit() {
        this.email = (String) getRequest().getAttributes().get("email");
        query = String.format(ENDORSER_HISTORY_QUERY_TEMPLATE, email);
    }

    @Get("html")
    public Representation toHtml() {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        
        try {
        	results = query(query);
        } catch(MarketplaceException e){
        	LOGGER.severe(e.getMessage());
        }
        
        for(int i = 0; i < results.size(); i++){
        	Map<String, String> resultRow = results.get(i);
        	String deprecated = resultRow.get("deprecated");
        	String location = resultRow.get("location");
        	
        	if(deprecated.equals("null"))
        		resultRow.put("deprecated", "");
        	
        	if(location.equals("null"))
        		resultRow.put("location", "");
        	
        	results.set(i, resultRow);
        }
        	
        Map<String, Object> data = createInfoStructure("History for " + this.email);
        data.put("content", results);

        // Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "endorser.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }

    /**
     * Returns endorser details.
     */
    @Get("xml")
    public Representation toXml() {
        String results = "";
		try {
			results = queryResultsAsXml(query);
		} catch (MarketplaceException e) {
			LOGGER.severe(e.getMessage());
		}
        StringRepresentation representation = new StringRepresentation(results,
                MediaType.APPLICATION_XML);

        // Returns the XML representation of this document.
        return representation;
    }
}
