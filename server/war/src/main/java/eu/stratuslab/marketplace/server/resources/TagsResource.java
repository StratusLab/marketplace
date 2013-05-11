package eu.stratuslab.marketplace.server.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import eu.stratuslab.marketplace.server.MarketplaceException;

public class TagsResource extends BaseResource{
	
	private String query = null;
    private String email = null;
    
	@Override
    protected void doInit() {
        email = (String) getRequest().getAttributes().get("email");
        query = getQueryBuilder().buildEndorserTagsQuery(email);
    }

    @Get("html")
    public Representation toHtml() {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        
        try {
        	results = query(query);      	
        } catch(MarketplaceException e){
        	LOGGER.severe(e.getMessage());
        }
        	
        Map<String, Object> data = createInfoStructure("Current tags for " + this.email);
        data.put("content", results);
        data.put("email", email);

        // Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "tags.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }

    /**
     * Returns the endorser's tags details.
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
