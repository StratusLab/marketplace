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

import static eu.stratuslab.marketplace.server.cfg.Parameter.METADATA_MAX_BYTES;
import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.MARKETPLACE_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.json.simple.JSONValue;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

/**
 * This resource represents a list of all Metadata entries
 */
public class MDataResource extends MDataResourceBase {
    		
	@Post("www_form")
	public Representation acceptDatatablesQueryString(Representation entity){
		if (entity == null) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "post with null entity");
        }
		
		String query = "";
    	try {
    		query = entity.getText();
    	} catch(IOException e){}

    	getRequest().getResourceRef().setQuery(query);

    	return toJSON();
	}
	
	@Post("multipart")
    public Representation acceptMetadataWebUpload(Representation entity) {
		if (entity == null) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "post with null entity");
        }

		isUploadPermitted();
		
        File upload = processMultipartForm();
       
        return acceptMetadataEntry(upload);
    }
	
	@Post("application_rdf|application_xml")
    public Representation acceptMetadataUpload(Representation entity) {
		if (entity == null) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "post with null entity");
        }

		isUploadPermitted();
		
		File upload = MetadataFileUtils.writeContentsToDisk(entity);
        		
        return acceptMetadataEntry(upload);
    }

	private void isUploadPermitted(){
		String type = Configuration.getParameterValue(MARKETPLACE_TYPE);
		
		if(type != null && type.equalsIgnoreCase("replica")){
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
            "read-only repository");
		}
	}
	
    // Currently this method will only process the first uploaded file. This is
    // done to simplify the logic for treating a post request. This should be
    // extended in the future to handle multiple files.
    private File processMultipartForm() {

        File storeDirectory = Configuration
                .getParameterValueAsFile(PENDING_DIR);

        int fileSizeLimit = Configuration
                .getParameterValueAsInt(METADATA_MAX_BYTES);

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(fileSizeLimit);

        RestletFileUpload upload = new RestletFileUpload(factory);

        List<FileItem> items;

        try {
            Request request = getRequest();
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e
                    .getMessage(), e);
        }

        for (FileItem fi : items) {
            if (fi.getName() != null) {
                String uuid = UUID.randomUUID().toString();
                File file = new File(storeDirectory, uuid);
                try {
                    fi.write(file);
                    return file;
                } catch (Exception consumed) {
                }
            }
        }

        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                "no valid file uploaded");
    }
	
    @Get("html")
    public Representation toHtml() throws IOException {
    	
    	Map<String, Object> data = createInfoStructure("Metadata");

    	// Load the FreeMarker template
        // Wraps the bean with a FreeMarker representation
        Representation representation = createTemplateRepresentation(
                "metadata.ftl", data, MediaType.TEXT_HTML);

        return representation;
    }

    /**
     * Returns a listing of all registered metadata or a particular entry if
     * specified.
     */
    @Get("xml")
    public Representation toXml() {
    	
    	String deprecatedFlag = getDeprecatedFlag();
    		
    	List<Map<String, String>> metadata = getMetadata(deprecatedFlag, 
    			getRequestQueryValues());
    	metadata.remove(0);
    	
        List<String> pathsToMetadata = buildPathsToMetadata(metadata);

        String xmlOutput = buildXmlOutput(pathsToMetadata);
                
        // Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(xmlOutput,
                MediaType.APPLICATION_XML);

        return representation;
    }

    private List<String> buildPathsToMetadata(List<Map<String, String>> metadata){
    	
    	List<String> pathsToMetadata = new ArrayList<String>(metadata.size());
        for (Map<String, String> entry : metadata) {
        	String path = entry.get("identifier") 
        	+ File.separator + entry.get("email") 
        	+ File.separator + entry.get("created"); 
        			       	            
            pathsToMetadata.add(path);
        }
        
        return pathsToMetadata;
    }
    
	private String buildXmlOutput(List<String> pathsToMetadata) {
		
		StringBuilder output = new StringBuilder(XML_HEADER);

		output.append("<metadata>");
		for (String path : pathsToMetadata) {
			String datum = getMetadatum(path);
			if (datum != null) {
				if (datum.startsWith(XML_HEADER)) {
					datum = datum.substring(XML_HEADER.length());
				}

				output.append(datum);
			}
		}
		output.append("</metadata>");
		
		return output.toString();
	}
    
    /**
     * Returns a listing of all registered metadata or a particular entry if
     * specified.
     */
    @Get("json")
    public Representation toJSON() {
    	String deprecatedFlag = getDeprecatedFlag();
    	            	
    	List<Map<String, String>> metadata = null;
    	
    	String msg = "no metadata matching query found";
    	
    	try {
    		metadata = getMetadata(deprecatedFlag, 
    				getRequestQueryValues());
    	} catch(ResourceException r){
    		metadata = new ArrayList<Map<String, String>>();
        	if(r.getCause() != null){
        		msg = "ERROR: " + r.getCause().getMessage();
        	}
    	}
    	
		String iTotalDisplayRecords = "0";
		String iTotalRecords = "0";
		if (metadata.size() > 0) {
			Map<String, String> recordCounts = (Map<String, String>) metadata
					.remove(0);			
			iTotalDisplayRecords = recordCounts.get("iTotalDisplayRecords");
			iTotalRecords = recordCounts.get("iTotalRecords");	
		}

		Map<String, Object> json = buildJsonHeader(iTotalRecords,
				iTotalDisplayRecords, msg);
		List<ArrayList<String>> jsonResults = buildJsonResults(metadata);
		
		json.put("aaData", jsonResults);

		// Returns the XML representation of this document.
		return new StringRepresentation(JSONValue.toJSONString(json), 
				MediaType.APPLICATION_JSON);
    }
       
   private Map<String, Object> buildJsonHeader(String iTotalRecords, 
    		String iTotalDisplayRecords, String msg){
    	Map<String, Object> json = new HashMap<String, Object>();
    	
    	Integer sEcho = (getRequestQueryValues().get("sEcho") != null 
    			? Integer.valueOf(getRequestQueryValues().get("sEcho")) : 0);
    	
        json.put("sEcho", sEcho);
        json.put("iTotalRecords", Long.valueOf(iTotalRecords));
        json.put("iTotalDisplayRecords", Long.valueOf(iTotalDisplayRecords));
        json.put("rMsg", msg);
    	    	
    	return json;
    }
    
    private List<ArrayList<String>> buildJsonResults(List<Map<String, String>> metadata){
    	int numberOfRows = metadata.size();
    	
    	List<ArrayList<String>> aaData = new ArrayList<ArrayList<String>>(numberOfRows);
    	
    	for(int i = 0; i < metadata.size(); i++){
    		Map<String, String> resultRow = (Map<String, String>)metadata.get(i);

    		ArrayList<String> row = new ArrayList<String>(resultRow.size());
			row.add(""); //empty cell used for display
			row.add(resultRow.get("os"));
            row.add(resultRow.get("osversion"));
            row.add(resultRow.get("arch"));
            row.add(resultRow.get("email"));
            row.add(resultRow.get("created"));
            row.add(resultRow.get("identifier"));
            row.add(resultRow.get("location"));
            row.add(resultRow.get("description"));
            row.add(resultRow.get("title"));
            
            aaData.add(row);		    		
    	}

    	return aaData;
    }
    
    private String getDeprecatedFlag(){
    	Map<String, String> requestValues = getRequestQueryValues();
    	
    	String deprecatedFlag = (requestValues.containsKey("deprecated")) ? 
    			getDeprecatedFlag(requestValues.get("deprecated")) : "off";
    			
        return deprecatedFlag;
    }
    
    /*
     * Gets the value of the deprecated flag from the query
     * 
     * @param the value taken from the request
     * 
     * @return the value of the deprecated flag
     */
    private String getDeprecatedFlag(String deprecated){
    	return (deprecated == null) ? "on" : deprecated;
    }
    
    private Map<String, String> getRequestQueryValues(){
    	Form form = getRequest().getResourceRef().getQueryAsForm();
    	
    	return form.getValuesMap();
    }
}
