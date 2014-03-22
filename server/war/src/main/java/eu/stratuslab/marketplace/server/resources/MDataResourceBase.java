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

import static eu.stratuslab.marketplace.server.utils.XPathUtils.DEPRECATED;
import static eu.stratuslab.marketplace.server.cfg.Parameter.VALIDATE_EMAIL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import eu.stratuslab.marketplace.metadata.MetadataException;
import eu.stratuslab.marketplace.metadata.ValidateMetadataConstraints;
import eu.stratuslab.marketplace.metadata.ValidateRDFModel;
import eu.stratuslab.marketplace.metadata.ValidateXMLSignature;
import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.EndorserWhitelist;
import eu.stratuslab.marketplace.server.utils.MarketplaceUtils;
import eu.stratuslab.marketplace.server.utils.MessageUtils;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;
import eu.stratuslab.marketplace.server.utils.Notifier;
import eu.stratuslab.marketplace.server.utils.XPathUtils;

public class MDataResourceBase extends BaseResource {

	protected Representation acceptMetadataEntry(File upload){
		boolean validateEmail = Configuration
		.getParameterValueAsBoolean(VALIDATE_EMAIL);
		
		Document validatedUpload = validateMetadata(upload);

		EndorserWhitelist whitelist = getWhitelist();
		
		String baseUrl = getRequest().getRootRef().toString();
				
		if (!validateEmail
				|| (whitelist.isEnabled() && whitelist
						.isEndorserWhitelisted(validatedUpload))) {

			String metadataPath = commitMetadataEntry(upload, validatedUpload);

			setStatus(Status.SUCCESS_CREATED);
			Representation status = createStatusRepresentation("Upload", 
			"metadata entry created");
			
			status.setLocationRef(baseUrl + "/metadata/" + metadataPath);

			return status;

		} else {
			confirmMetadataEntry(baseUrl, upload, validatedUpload);
			
			setStatus(Status.SUCCESS_ACCEPTED);
			return createStatusRepresentation("Upload",
			"confirmation email sent for new metadata entry\n");
		}
	}
		
	private Document validateMetadata(File upload) {

        InputStream stream = null;
        Document metadataXml = null;

        try {
        	stream = new FileInputStream(upload);

            metadataXml = MetadataFileUtils.extractXmlDocument(stream);
            
            ValidateXMLSignature.validate(metadataXml);
            ValidateMetadataConstraints.validate(metadataXml);
            ValidateRDFModel.validate(metadataXml);

            validateMetadataNewerThanLatest(metadataXml);
            
        } catch (MetadataException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "invalid metadata: " + e.getMessage());
        } catch (FileNotFoundException e) {
            LOGGER.severe("unable to read metadata file: "
                    + upload.getAbsolutePath());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "unable to read metadata file");
        } finally {
            MetadataFileUtils.closeReliably(stream);
        }
        return metadataXml;
    }
    
	private void validateMetadataNewerThanLatest(Document metadataXml) 
	throws MetadataException {
		String[] coordinates = getMetadataEntryCoordinates(metadataXml);
		
		String identifier = coordinates[0];
        String endorser = coordinates[1];
        String created = coordinates[2];
        
        String query = getQueryBuilder().buildLatestEntryQuery(
        		identifier, endorser);
        
        try {
        	List<Map<String, String>> results = query(query);
        	
        	if(results.size() > 0){
        		
        		String latest = results.get(0).get(getQueryBuilder().getLatestField());
        		if(!latest.equals("null")){

        			try {
        				Date newEntry = MarketplaceUtils.getFormattedDate(created);
        				Date existingEntry = MarketplaceUtils.getFormattedDate(latest);

        				if(newEntry.before(existingEntry)){        		
        					throw new MetadataException("older than latest entry");
        				}
        				        				        				
        			} catch (ParseException e) {
        				throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        			}
        		}
        	}
        } catch(MarketplaceException e){
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        }
		
	}
	
	protected void confirmMetadataEntry(String baseUrl, File upload, Document metadata) {

        try {
        	sendEmailConfirmation(baseUrl, upload, metadata);
        } catch (MarketplaceException e) {
            String msg = "error sending confirmation email";
            LOGGER.severe(msg + ": " + e.getMessage());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "error sending confirmation email");
        }
    }
	
	private void sendEmailConfirmation(String baseUrl, File file, Document metadata)
	throws MarketplaceException {
		String[] coords = getMetadataEntryCoordinates(metadata);
		String emailAddress = coords[1];		
		
		String deprecated = XPathUtils.getValue(metadata, DEPRECATED);
		
		String message = MessageUtils.createNotification(baseUrl, 
				file, coords, deprecated);
		Notifier.sendNotification(emailAddress, message);	
	}
	
	/*
     * Retrieves the metadata from the repository
     * 
     * @param status indicates status of entries to include (valid, deprecated, expired)
     *                        
     * @return a metadata list
     */
    protected List<Map<String, String>> getMetadata(String status, String access,
    		Map<String, String> requestQueryValues) {
    	boolean hasFilter = false;
    	    	
    	Map<String, Object> attributes = getRequest().getAttributes();
    	
    	if(isSearchQuery(requestQueryValues, attributes)){
        	hasFilter = true;
    	}
    	
    	Map<String, String> recordCounts = new HashMap<String, String>();
    	
    	String iTotalRecords = getTotalRecords(status, access);
    	recordCounts.put("iTotalRecords", iTotalRecords);
    	
     	String dataQuery = getQueryBuilder().buildGetMetadataQuery(status, access,
    			requestQueryValues, attributes);
    	
    	String countQuery = getQueryBuilder().buildGetMetadataCountQuery(status, access,
    			requestQueryValues, attributes);
    	String iTotalDisplayRecords = getTotalDisplayRecords(countQuery);
    	recordCounts.put("iTotalDisplayRecords", iTotalDisplayRecords);       
        
        //Get the results
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        try {
        	results = query(dataQuery);
        } catch(MarketplaceException e){
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }

        if(results.size() <= 0 && hasFilter || (results.size() <= 0 || (results.get(0).get("identifier").equals("null")))){
        	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "no metadata matching query found");
        } else {
        	results.add(0, recordCounts);
        	return results;
        }
    }
       
    private boolean isSearchQuery(Map<String, String> requestQueryValues,
			Map<String, Object> attributes) {
		boolean filter = false;    	
    	
    	if(requestQueryValues.containsKey("identifier") 
    			|| requestQueryValues.containsKey("email")
    			|| requestQueryValues.containsKey("created")){
    		filter = true;
    	}
    	
    	if(attributes.containsKey("arg1") || attributes.containsKey("arg2")) {
    		filter = true;
    	}
    	
    	if(attributes.containsKey("identifier") || attributes.containsKey("email")
    			|| attributes.containsKey("created")){
    		filter = true;
    	}
    	   	    		
		return filter;
	}

	private String getTotalDisplayRecords(String query){
    	String iTotalDisplayRecords = "0";
        
        try {
        	 List<Map<String, String>> countResult = query(query);
             
        	 if(countResult.size() > 0){
        		 iTotalDisplayRecords = (String)countResult.get(0).get("count");
        	 }
        } catch(MarketplaceException e){
       		LOGGER.severe(e.getMessage());
        }
        
        return iTotalDisplayRecords;
    }
    
	/*
     * Gets the total number of unfiltered records
     * 
     * @param whether to include deprecated entries or not
     * 
     * @return the total number of records
     */
    private String getTotalRecords(String status, String access){
    	String query = getQueryBuilder().buildGetTotalRecordsQuery(status, access);
    	
    	String iTotalRecords = "0";
    	
    	try {
    		List<Map<String, String>> results = query(query);
        	
    		if(results.size() > 0){
    			iTotalRecords = (String)(
    					(Map<String, String>)results.remove(0)).get("count");
    		}
  
    	} catch(MarketplaceException e){
    		LOGGER.severe(e.getMessage());
    	}
    	
    	return iTotalRecords;
    }
}
