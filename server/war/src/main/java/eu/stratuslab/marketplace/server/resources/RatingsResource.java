package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;
import eu.stratuslab.marketplace.server.cfg.Configuration;

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.data.MediaType;
import org.restlet.data.Form;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.Closeable;

/**
 * This resource represents star ratings
 */
public class RatingsResource extends BaseResource {
	
	@Get("json")
    public Representation toJson() {
		Form form = getRequest().getResourceRef().getQueryAsForm();
    	Map<String, String> formValues = form.getValuesMap();
    			
		String identifier = (String)formValues.get("identifier");
		String ratings = getRatings(identifier);
		
		// Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(ratings,
                MediaType.APPLICATION_JSON);

        return representation;	
	}
	
	/**
     * Handle POST requests: accept vote.
     */
    @Post
    public Representation acceptRating(Representation entity) {
    	
		String identifier = "";
		String clickedOn = "";
		
		if (entity.getMediaType().equals(MediaType.APPLICATION_WWW_FORM,
			    true)) {
			   Form form = new Form(entity);
			   identifier = form.getFirstValue("identifier");
			   clickedOn = form.getFirstValue("clicked_on");
		}
		
		int vote = 0;
		
		if(clickedOn != ""){
			try {
				vote = Integer.parseInt(
						clickedOn.substring(clickedOn.indexOf("_") + 1, 
								clickedOn.indexOf(" ")));
			} catch(NumberFormatException e){}
		}
				
		Map<String, String> ratings = loadData(identifier);
		float numberVotes = 0;
		float totalPoints = 0;
				
		try {
		    numberVotes = Float.parseFloat(ratings.get("number_votes"));		
		    totalPoints = Float.parseFloat(ratings.get("total_points"));
		} catch (NumberFormatException e){}
		
		numberVotes += 1;
		totalPoints += vote;
		
		float decAvg = totalPoints/numberVotes;
		int wholeAvg = Math.round(decAvg);
		    
		ratings.put("number_votes", String.valueOf(Math.round(numberVotes)));
		ratings.put("total_points", String.valueOf(Math.round(totalPoints)));
		ratings.put("whole_avg", String.valueOf(wholeAvg));
		ratings.put("dec_avg", String.valueOf(decAvg));
		
		saveData(identifier, ratings);
		
		// Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(data2json(ratings),
                MediaType.APPLICATION_JSON);

        return representation;
    }
    
    private String getRatings(String identifier){
    	Map<String, String> data = loadData(identifier);
    	   	   	
    	return data2json(data);
    }
    
    private Map<String, String> loadData(String identifier){
    	String data = "identifier=" + identifier 
    	            + ":number_votes=0:total_points=0:dec_avg=0:whole_avg=0";
    	
    	String path = Configuration.getParameterValue(DATA_DIR) 
    		+ "/" + identifier + "/rating.txt";
    	
    	try {
    		data = readFileAsString(path);
    	} catch(IOException e){
    		LOGGER.severe("Unable to read ratings");
    	}
    	
    	Map<String, String> map = new HashMap<String, String>();
    	
    	if(data != ""){
    		String[] entries = data.trim().split(":");
    		for(int i = 0; i < entries.length; i++){
    			String[] field = entries[i].split("=");
    			map.put(field[0], field[1]);
    		}
    	}
    	
    	return map;
    }
    
    private synchronized void saveData(String identifier, Map<String, String> data) {
    	File dataFile = new File(
    			Configuration.getParameterValue(DATA_DIR) 
    			+ "/" + identifier + "/rating.txt");
    	FileWriter writer = null;
    	
    	try {
    		writer = new FileWriter(dataFile);
    	    writer.write(data2String(data));
    	} catch(IOException e) {
    		LOGGER.severe("Unable to save rating");
    	} finally {
            closeReliably(writer);
        }
    	
    }
    
    private static void closeReliably(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException consumed) {
                LOGGER.severe(consumed.getMessage());
            }
        }
    }
    
    private String data2String(Map<String, String> data){
    	StringBuilder dataString = new StringBuilder();
    	
    	dataString.append("identifier=" + data.get("identifier"));
    	dataString.append(":number_votes=" + data.get("number_votes"));
    	dataString.append(":total_points=" + data.get("total_points"));
    	dataString.append(":dec_avg=" + data.get("dec_avg"));
    	dataString.append(":whole_avg=" + data.get("whole_avg"));
    
    	return dataString.toString();
    }
    
    private String data2json(Map<String, String> data){
    	StringBuilder jsonString = new StringBuilder();
    	jsonString.append("{");
    	
    	for(Map.Entry<String, String> field : data.entrySet()){
    		jsonString.append("\"" + field.getKey() 
    				+ "\": \"" + field.getValue() + "\" , ");
    	}
    	    	    	
    	String json = jsonString.toString();
       	json = json.substring(0, json.length() - 3);
    	json = json + " }";
    	
    	return json;
    }
    
}
    	