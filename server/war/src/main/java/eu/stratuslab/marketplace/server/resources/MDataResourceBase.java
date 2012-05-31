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

import static eu.stratuslab.marketplace.server.cfg.Parameter.VALIDATE_EMAIL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.PatternUtils;
import eu.stratuslab.marketplace.metadata.MetadataException;
import eu.stratuslab.marketplace.metadata.ValidateMetadataConstraints;
import eu.stratuslab.marketplace.metadata.ValidateRDFModel;
import eu.stratuslab.marketplace.metadata.ValidateXMLSignature;
import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.EndorserWhitelist;
import eu.stratuslab.marketplace.server.utils.MessageUtils;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;
import eu.stratuslab.marketplace.server.utils.Notifier;
import eu.stratuslab.marketplace.server.utils.SparqlUtils;

public class MDataResourceBase extends BaseResource {

	private static final String IDENTIFIER = "identifier";
	private static final String EMAIL = "email";
	private static final String CREATED = "created";
	
	protected Representation acceptMetadataEntry(File upload){
		boolean validateEmail = Configuration
		.getParameterValueAsBoolean(VALIDATE_EMAIL);
		
		Document validatedUpload = validateMetadata(upload);

		EndorserWhitelist whitelist = getWhitelist();
		
		String baseUrl = getRequest().getRootRef().toString();
				
		if (!validateEmail
				|| (whitelist.isEnabled() && whitelist
						.isEndorserWhitelisted(validatedUpload))) {

			String iri = commitMetadataEntry(upload, validatedUpload);

			setStatus(Status.SUCCESS_CREATED);
			Representation status = createStatusRepresentation("Upload", 
			"metadata entry created");
			
			status.setLocationRef(baseUrl + "/metadata" + iri);

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
        
        String query = String.format(SparqlUtils.LATEST_ENTRY_QUERY_TEMPLATE, 
        		identifier, endorser, created);
        
        try {
        	List<Map<String, String>> results = query(query);
        	
        	if(results.size() > 0){
        		throw new MetadataException("older than latest entry");
        	}
        } catch(MarketplaceException e){
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        }
		
	}
	
	protected static void confirmMetadataEntry(String baseUrl, File upload, Document metadata) {

        try {
            String[] coords = getMetadataEntryCoordinates(metadata);
            sendEmailConfirmation(baseUrl, coords[1], upload);
        } catch (MarketplaceException e) {
            String msg = "error sending confirmation email";
            LOGGER.severe(msg + ": " + e.getMessage());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "error sending confirmation email");
        }
    }
	
	private static void sendEmailConfirmation(String baseUrl, String email, File file)
	throws MarketplaceException {

		String message = MessageUtils.createNotification(baseUrl, file);
		Notifier.sendNotification(email, message);
		
	}
	
	/*
     * Retrieves the metadata from the repository
     * 
     * @param deprecatedValue indicates whether deprecated values should be included
     *                        possible values are on, off, only.
     *                        
     * @return a metadata list
     */
    protected List<Map<String, String>> getMetadata(String deprecatedValue, 
    		Map<String, String> requestQueryValues) {
    	boolean hasDate = false;
    	boolean hasFilter = false;
    	    	    	
    	addQueryParametersToRequest(requestQueryValues);
    	
    	Map<String, String> recordCounts = new HashMap<String, String>();
    	recordCounts.put("iTotalRecords", getTotalRecords(deprecatedValue));
    	    	
    	String filter = buildFilterFromUrlQuery();
    	
    	if(filter.length() > 0){
    		hasFilter = true;
    		
    		if(filter.contains(CREATED)){
    			hasDate = true;
    		}
    	}
    	    	
    	//Build the paging query
    	String paging = buildPagingFilter(requestQueryValues);
    	String searching = buildSearchingFilter(requestQueryValues);
    	    	
    	//Build the full SPARQL queries
    	StringBuilder dataQuery = new StringBuilder(SparqlUtils.SELECT_ALL);
        StringBuilder countQuery = new StringBuilder(SparqlUtils.SELECT_COUNT);
                
        String where = " WHERE {"
            + SparqlUtils.WHERE_BLOCK;
        
        StringBuilder wherePredicate = new StringBuilder(
                    where);

        wherePredicate.append(filter);

        if (!hasDate) {
                wherePredicate
                        .append(SparqlUtils.getLatestFilter(getCurrentDate()));
        }
                     
        appendDeprecated(deprecatedValue, wherePredicate);
                        
        wherePredicate.append(searching);
        wherePredicate.append(" }");

        //Get the total number of unfiltered results
        countQuery.append(wherePredicate);
        
        recordCounts.put("iTotalDisplayRecords", getTotalDisplayRecords(countQuery.toString()));       
                
        dataQuery.append(wherePredicate);
        dataQuery.append(paging);
        
        //Get the results
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        try {
        	results = query(dataQuery.toString());
        } catch(MarketplaceException e){
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }

        if(results.size() <= 0 && hasFilter){
        	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "no metadata matching query found");
        } else {
        	results.add(0, recordCounts);
        	return results;
        }
    }

    private void appendDeprecated(String deprecated, StringBuilder filter) {
		if (deprecated.equals("off")){
        	filter.append(SparqlUtils.DEPRECATED_OFF);
        } else if (deprecated.equals("only")){
        	filter.append(SparqlUtils.DEPRECATED_ON);
        }
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
    
	private void addQueryParametersToRequest(Map<String, String> formValues) {
    	
		//Create filter from request parameters.    	    	 	
    	if(formValues.containsKey(IDENTIFIER)){
    		getRequest().getAttributes().put(IDENTIFIER, formValues.get(IDENTIFIER));
    	}
    	if(formValues.containsKey(EMAIL)){
    		getRequest().getAttributes().put(EMAIL, formValues.get(EMAIL));
    	}
    	if(formValues.containsKey(CREATED)){
    		getRequest().getAttributes().put(CREATED, formValues.get(CREATED));
    	}   
    }
    
    private String buildFilterFromUrlQuery(){
    	StringBuilder filter = new StringBuilder();
    	
    	for (Map.Entry<String, Object> arg : getRequest().getAttributes().entrySet()) {
    		String key = arg.getKey();
    		if (!key.startsWith("org.restlet")) {
    			switch (classifyArg((String) arg.getValue())) {
    			case ARG_EMAIL:
    				filter.append(
    						SparqlUtils.buildFilterEq(EMAIL, (String)arg.getValue()));
    				break;
    			case ARG_DATE:
    				filter.append(
    						SparqlUtils.buildFilterEq(CREATED, (String)arg.getValue()));
    				break;
    			case ARG_OTHER:
    				filter.append(
    						SparqlUtils.buildFilterEq(IDENTIFIER, (String)arg.getValue()));
    				break;
    			default:
    				break;
    			}
    		}
    	}
    	
    	return filter.toString();
    }
    
    private int classifyArg(String arg) {
        if (arg == null || arg.equals("null") || arg.equals("")) {
            return -1;
        } else if (PatternUtils.isEmail(arg)) {
            return ARG_EMAIL;
        } else if (PatternUtils.isDate(arg)) {
            return ARG_DATE;
        } else {
            return ARG_OTHER;
        }
    }
    
    /*
     * Build a paging filter (ORDER BY ... LIMIT ...)
     * 
     * @return SPARQL LIMIT clause
     */
    private String buildPagingFilter(Map<String, String> requestQueryValues){
    	
    	String iDisplayStart = requestQueryValues.get("iDisplayStart");
		String iDisplayLength = requestQueryValues.get("iDisplayLength");
		String iSortCol = requestQueryValues.get("iSortCol_0");
		String sSortDir = requestQueryValues.get("sSortDir_0");   	
    	String paging = "";
    	
    	if(iDisplayStart != null && iDisplayLength != null){
    		int sort = (iSortCol != null) ? 
    				Integer.parseInt(iSortCol) : SparqlUtils.DEFAULT_SEARCH_COL;
    		String sortCol = SparqlUtils.getColumn(sort);
    		      		
    		paging = SparqlUtils.buildLimit(sortCol, iDisplayLength, iDisplayStart, sSortDir);
    	}
    	
    	return paging;
    }
             
    private String buildSearchingFilter(Map<String, String> requestQueryValues){
    	String sSearch = requestQueryValues.get("sSearch");
    	
    	String[] sSearchCols = new String[SparqlUtils.getColumnCount()];
    	
        for(int i = 0; i < SparqlUtils.getColumnCount();i++){
                sSearchCols[i] = requestQueryValues.get("sSearch_" + i);
        }
        
        String searching = buildSearchAllFilter(sSearch)
        					+ buildSearchColsFilter(sSearchCols);      
        
        return searching;
    }
           
    private String buildSearchAllFilter(String sSearch){
    	String searching = "";
    	
    	if(sSearch != null && sSearch.length() > 0){
            String[] searchTerms = sSearch.split(" ");
            StringBuilder searchAllFilter = new StringBuilder(" FILTER (");
            for(int i = 0; i < searchTerms.length; i++){

                    searchAllFilter.append("(");
                    for(int j = 1; j < SparqlUtils.getColumnCount(); j++){
                            searchAllFilter.append(SparqlUtils.buildRegex(
                                            SparqlUtils.getColumn(j), searchTerms[i]));

                            if(j < SparqlUtils.getColumnCount() - 1){
                                    searchAllFilter.append(" || ");
                            }
                    }
                    searchAllFilter.append(")");

                    if(i < searchTerms.length -1){
                                    searchAllFilter.append(" && ");
                    }
            }
            searchAllFilter.append(") . ");
            searching = searchAllFilter.toString();
    	}
    	
    	return searching;    	
    }
    
    private String buildSearchColsFilter(String[] sSearchCols) {
    	StringBuilder searchColumnsPredicate = new StringBuilder();
        
        for(int i = 1; i <= SparqlUtils.SEARCHABLE_COLUMNS; i++){
                if ( sSearchCols[i] != null && sSearchCols[i].length() > 0 )
                {
                        if ( searchColumnsPredicate.length() == 0 )
                        {
                                searchColumnsPredicate.append(" FILTER (");
                        }
                        else
                        {
                                searchColumnsPredicate.append(" && ");
                        }
                        String word = sSearchCols[i];
                        searchColumnsPredicate.append(SparqlUtils.buildRegex(
                                        SparqlUtils.getColumn(i), word));
                }
        }
        
        if(searchColumnsPredicate.length() != 0){
                searchColumnsPredicate.append(" ) . ");
        }
        
        return searchColumnsPredicate.toString();
    }
    
    /*
     * Gets the total number of unfiltered records
     * 
     * @param whether to include deprecated entries or not
     * 
     * @return the total number of records
     */
    private String getTotalRecords(String deprecatedFlag){
    	String q = SparqlUtils.SELECT_COUNT
        + " WHERE {"
        + SparqlUtils.WHERE_BLOCK
        + SparqlUtils.getLatestFilter(getCurrentDate());
    	
    	StringBuilder query = new StringBuilder(q);
    	
    	appendDeprecated(deprecatedFlag, query);
    	
    	query.append(" }");
    	
    	String iTotalRecords = "0";
    	
    	try {
    		List<Map<String, String>> results = query(query.toString());
        	
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
