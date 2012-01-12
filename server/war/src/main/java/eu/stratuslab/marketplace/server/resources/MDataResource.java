package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.server.cfg.Parameter.METADATA_MAX_BYTES;
import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.VALIDATE_EMAIL;
import static org.restlet.data.MediaType.APPLICATION_RDF_XML;
import static org.restlet.data.MediaType.APPLICATION_WWW_FORM;
import static org.restlet.data.MediaType.APPLICATION_XML;
import static org.restlet.data.MediaType.MULTIPART_FORM_DATA;
import static org.restlet.data.MediaType.TEXT_PLAIN;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
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
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.metadata.MetadataException;
import eu.stratuslab.marketplace.metadata.ValidateMetadataConstraints;
import eu.stratuslab.marketplace.metadata.ValidateRDFModel;
import eu.stratuslab.marketplace.metadata.ValidateXMLSignature;
import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MessageUtils;
import eu.stratuslab.marketplace.server.utils.Notifier;
import eu.stratuslab.marketplace.server.utils.SparqlUtils;

/**
 * This resource represents a list of all Metadata entries
 */
public class MDataResource extends BaseResource {
        
	private static String JSON_DISPLAY_TEMPLATE = "<table class=vmpanel>"
			+ "<tr><td colspan=3><div id=header>%s v"
			+ "%s %s </div></td></tr>"
			+ "<tr><td></td><td></td><td rowspan=5><a href="
			+ "%s><img src=/css/download.png/></a></td></tr>"
			+ "<tr><td><div id=detail>Endorser:</div></td>"
			+ "<td><div id=detail>%s</div></td></tr>"
			+ "<tr><td><div id=detail>Identifier:</div></td>"
			+ "<td><div id=detail>%s</div></td></tr>"
			+ "<tr><td><div id=detail>Created:</div></td>"
			+ "<td><div id=detail>%s</div></td></tr>" + "<tr></tr></div>"
			+ "<tr><td colspan=3><div id=description>%s" + "</div></td></tr>"
			+ "<tr><td><a href=/metadata/%s/%s/%s>More...</a></td></tr>"
			+ "</table>";
	
	/**
     * Handle POST requests: register new Metadata entry.
     */
    @Post
    public Representation acceptMetadatum(Representation entity) {

        if (entity == null) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "post with null entity");
        }

        MediaType mediaType = entity.getMediaType();

        File upload = null;
        if (MULTIPART_FORM_DATA.equals(mediaType, true)) {
            upload = processMultipartForm();
        } else if (APPLICATION_RDF_XML.equals(mediaType, true)
                || (APPLICATION_XML.equals(mediaType, true))) {
            upload = writeContentsToDisk(entity);
        } else if (APPLICATION_WWW_FORM.equals(mediaType, true)) {
        	String query = "";
        	try {
        		query = entity.getText();
        	} catch(IOException e){}
	
        	getRequest().getResourceRef().setQuery(query);

        	return toJSON();
	} else {
            throw new ResourceException(
                    Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, mediaType
                            .getName());
        }

        boolean validateEmail = Configuration
                .getParameterValueAsBoolean(VALIDATE_EMAIL);

        Document validatedUpload = validateMetadata(upload);

        if (!validateEmail) {

            String iri = commitMetadataEntry(upload, validatedUpload);

            setStatus(Status.SUCCESS_CREATED);
            Representation status = createStatusRepresentation("Upload", 
            		"metadata entry created");
            status.setLocationRef(iri);

            return status;

        } else {

            confirmMetadataEntry(upload, validatedUpload);

            setStatus(Status.SUCCESS_ACCEPTED);
            return createStatusRepresentation("Upload",
                    "confirmation email sent for new metadata entry\n");
        }

    }

	private Representation createStatusRepresentation(String title,
			String message) {
		Representation status = null;
		if (getRequest().getClientInfo().getAcceptedMediaTypes().size() > 0
			&& getRequest().getClientInfo().getAcceptedMediaTypes()
				.get(0).getMetadata().equals(MediaType.TEXT_HTML)) {
			Map<String, Object> dataModel = createInfoStructure(title);
			dataModel.put("statusName", getResponse().getStatus().getName());
			dataModel.put("statusDescription", message);
			status = createTemplateRepresentation("status.ftl", dataModel,
				MediaType.TEXT_HTML);
		} else {
			status = new StringRepresentation(message, TEXT_PLAIN);
		}

		return status;
	}
    
    private static void confirmMetadataEntry(File upload, Document metadata) {

        try {
            String[] coords = getMetadataEntryCoordinates(metadata);
            sendEmailConfirmation(coords[1], upload);
        } catch (Exception e) {
            String msg = "error sending confirmation email";
            LOGGER.severe(msg + ": " + e.getMessage());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "error sending confirmation email");
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
                    .getMessage());
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

    private static void sendEmailConfirmation(String email, File file)
            throws Exception {

        String baseUrl = "http://localhost:8080/";
        String message = MessageUtils.createNotification(baseUrl, file);
        Notifier.sendNotification(email, message);
    }

    private Document validateMetadata(File upload) {

        InputStream stream = null;
        Document metadataXml = null;

        try {

            stream = new FileInputStream(upload);

            metadataXml = extractXmlDocument(stream);

            ValidateXMLSignature.validate(metadataXml);
            ValidateMetadataConstraints.validate(metadataXml);
            ValidateRDFModel.validate(metadataXml);

        } catch (MetadataException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "invalid metadata: " + e.getMessage());
        } catch (FileNotFoundException e) {
            LOGGER.severe("unable to read metadata file: "
                    + upload.getAbsolutePath());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "unable to read metadata file");
        } finally {
            closeReliably(stream);
        }

        return metadataXml;
    }

    private static File writeContentsToDisk(Representation entity) {

        char[] buffer = new char[4096];

        File storeDirectory = Configuration
                .getParameterValueAsFile(PENDING_DIR);

        File output = new File(storeDirectory, UUID.randomUUID().toString());

        Reader reader = null;
        Writer writer = null;

        try {

            reader = entity.getReader();
            writer = new FileWriter(output);

            int nchars = reader.read(buffer);
            while (nchars >= 0) {
                writer.write(buffer, 0, nchars);
                nchars = reader.read(buffer);
            }

        } catch (IOException consumed) {

        } finally {
            closeReliably(reader);
            closeReliably(writer);
        }
        return output;
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
    			
    	List<Map<String, String>> metadata = getMetadata(deprecatedFlag);
        metadata.remove(0);
    	
        List<String> pathsToMetadata = buildPathsToMetadata(metadata);

        String xmlOutput = buildXmlOutput(pathsToMetadata);
                
        // Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(xmlOutput,
                MediaType.APPLICATION_XML);

        return representation;
    }

    private List<String> buildPathsToMetadata(List<Map<String, String>> metadata){
    	List<String> pathsToMetadata = new ArrayList<String>();
        for (Map<String, String> entry : metadata) {

            String path = entry.get("identifier") + "/"
                    + entry.get("email") + "/" + entry.get("created");
            pathsToMetadata.add(path);
        }
        
        return pathsToMetadata;
    }
    
	private String buildXmlOutput(List<String> pathsToMetadata) {
		StringBuilder output = new StringBuilder(XML_HEADER);

		output.append("<metadata>");
		for (String path : pathsToMetadata) {
			String datum = getMetadatum(getDataDir() + File.separatorChar + path
					+ ".xml");
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
    		metadata = getMetadata(deprecatedFlag);
    	} catch(ResourceException r){
    		metadata = new ArrayList<Map<String, String>>();
                if(r.getCause() != null){
                	msg = "ERROR: " + r.getCause().getMessage();
                }
    	}
    	
    	String json = buildJsonOutput(metadata, msg, getTotalRecords(deprecatedFlag));
    	    	    	
    	// Returns the XML representation of this document.
        return new StringRepresentation(json,
                MediaType.APPLICATION_JSON);
    }
    
    private String buildJsonOutput(List<Map<String, String>> metadata, String msg, long iTotalRecords){
    	String iTotalDisplayRecords = "0";
    	if(metadata.size() > 0){
    		iTotalDisplayRecords = (String)((Map<String, String>)metadata.remove(0)).get("count");
    	}
    	
    	String jsonHeader = buildJsonHeader(iTotalRecords, iTotalDisplayRecords, msg);    	
    	String jsonResults = buildJsonResults(metadata);    	
    	    
    	StringBuilder json = new StringBuilder();
    	json.append(jsonHeader);
    	json.append(jsonResults);
    	json.append("]}");

    	return json.toString().replaceAll("\n", "<br>");
    }
    
    private String buildJsonHeader(long iTotalRecords, String iTotalDisplayRecords, String msg){
    	StringBuilder json = new StringBuilder("{ \"sEcho\":\"" 
    			+ getRequestQueryValues().get("sEcho") + "\", ");
    	json.append("\"iTotalRecords\":" + iTotalRecords + ", ");
    	json.append("\"iTotalDisplayRecords\":" + iTotalDisplayRecords + ", ");
    	json.append("\"rMsg\":\"" + msg + "\", ");
    	json.append("\"aaData\":[ ");
    	
    	return json.toString();
    }
    
    private String buildJsonResults(List<Map<String, String>> metadata){
    	StringBuilder json = new StringBuilder();
    	
    	for(int i = 0; i < metadata.size(); i++){
    		Map<String, String> resultRow = (Map<String, String>)metadata.get(i);

    		String identifier = resultRow.get("identifier");
    		String endorser = resultRow.get("email");
    		String created = resultRow.get("created");  
    		String os = resultRow.get("os");
    		String osversion = resultRow.get("osversion");
    		String arch = resultRow.get("arch");
    		String location = resultRow.get("location");
    		String description = resultRow.get("description");
    		
			String display = String.format(JSON_DISPLAY_TEMPLATE, os,
					osversion, arch, location, endorser, identifier, created,
					description.replaceAll("\"", "&quot;"), identifier,
					endorser, created);
    		    		    		
    		json.append("[\"" + display + "\"," +
    					"\"" + os + "\"," +
    		            "\"" + osversion + "\"," +
    		            "\"" + arch + "\"," +
    		            "\"" + endorser + "\"," +
    		            "\"" + created + "\"" +
    		            "]");

    		if(i < metadata.size() -1){
    			json.append(",");
    		}
    	}

    	return json.toString();
    }
    
    /*
     * Retrieves the metadata from the repository
     * 
     * @param deprecatedValue indicates whether deprecated values should be included
     *                        possible values are on, off, only.
     *                        
     * @return a metadata list
     */
    private List<Map<String, String>> getMetadata(String deprecatedValue) {
    	boolean dateSearch = false;
    	boolean hasFilter = false;
    	StringBuilder filterPredicate = new StringBuilder();
    	
    	Map<String, String> formValues = getRequestQueryValues();
    	Map<String, Object> requestAttr = getRequest().getAttributes();
    	    	    	
    	//Create filter from request parameters.    	    	 	
    	if(formValues.containsKey("identifier")){
    		requestAttr.put("identifier", formValues.get("identifier"));
    	}
    	if(formValues.containsKey("email")){
    		requestAttr.put("email", formValues.get("email"));
    	}
    	if(formValues.containsKey("created")){
    		requestAttr.put("created", formValues.get("created"));
    	}    	
    	
    	for (Map.Entry<String, Object> arg : requestAttr.entrySet()) {
    		String key = arg.getKey();
    		if (!key.startsWith("org.restlet")) {
    			switch (classifyArg((String) arg.getValue())) {
    			case ARG_EMAIL:
    				hasFilter = true;
    				filterPredicate.append(
    						SparqlUtils.buildFilterEq("email", (String)arg.getValue()));
    				break;
    			case ARG_DATE:
    				hasFilter = true;
    				dateSearch = true;
    				filterPredicate.append(
    						SparqlUtils.buildFilterEq("created", (String)arg.getValue()));
    				break;
    			case ARG_OTHER:
    				hasFilter = true;
    				filterPredicate.append(
    						SparqlUtils.buildFilterEq("identifier", (String)arg.getValue()));
    				break;
    			default:
    				break;
    			}
    		}
    	}

    	//Build the paging query
    	String paging = buildPagingFilter(formValues.get("iDisplayStart"), 
    			formValues.get("iDisplayLength"), formValues.get("iSortCol_0"), 
    			formValues.get("sSortDir_0"));
    	
    	String searching = buildSearchingFilter(formValues.get("sSearch"));
    	    	
    	//Build the full SPARQL query
    	StringBuilder dataQuery = new StringBuilder(SparqlUtils.SELECT_ALL);
        StringBuilder countQuery = new StringBuilder(SparqlUtils.SELECT_COUNT);
                
        StringBuilder filter = new StringBuilder(
                    " WHERE {"
                    + SparqlUtils.WHERE_BLOCK);

        filter.append(filterPredicate.toString());

        if (!dateSearch) {
                filter
                        .append(SparqlUtils.getLatestFilter(getCurrentDate()));
        }
                     
        if (deprecatedValue.equals("off")){
        	filter.append(" FILTER (!bound (?deprecated))");
        } else if (deprecatedValue.equals("only")){
        	filter.append(" FILTER (bound (?deprecated))");
        }
                
        filter.append(searching);
        filter.append(" }");

        //Get the total number of unfiltered results
        countQuery.append(filter);
        Map<String, String> count = new HashMap<String, String>();
        count.put("count", "0");
        
        try {
        	 List<Map<String, String>> countResult = query(countQuery.toString());
             
        	 if(countResult.size() > 0){
        		 count = (Map<String, String>)countResult.get(0);
        	 }
        } catch(MarketplaceException e){
       		LOGGER.severe(e.getMessage());
        }
                
        dataQuery.append(filter);
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
        	results.add(0, count);
        	return results;
        }
    }

    /*
     * Build a paging filter (ORDER BY ... LIMIT ...)
     * 
     * @param iDisplayStart offset
     * @param iDisplayLength page length
     * @param iSortCol the column to sort on
     * @param sSortDir direction of sort
     * 
     * @return SPARQL LIMIT clause
     */
    private String buildPagingFilter(String iDisplayStart, String iDisplayLength, 
    		String iSortCol, String sSortDir){
    	String paging = "";
    	
    	if(iDisplayStart != null && iDisplayLength != null){
    		int sort = (iSortCol != null) ? 
    				Integer.parseInt(iSortCol) : 5;
    		String sortCol = SparqlUtils.getColumn(sort);
    		      		
    		paging = SparqlUtils.buildLimit(sortCol, iDisplayLength, iDisplayStart, sSortDir);
    	}
    	
    	return paging;
    }
    
    /*
     * Build the search filter based on the entries in the search text boxes.
     * 
     * @param String taken from the 'Search all columns' field
     * @param individual column search text
     * 
     * @return sparql filter string
     */
    private String buildSearchingFilter(String sSearch){
    	String filter = "";
    	
    	if(sSearch != null && sSearch.length() > 0){		    		
    		String[] searchTerms = sSearch.split(" ");
    		StringBuilder searchFilter = new StringBuilder(" FILTER (");
    		for(int i = 0; i < searchTerms.length; i++){
    			
    			searchFilter.append("(");
    			for(int j = 0; j < SparqlUtils.getColumnCount(); j++){
    				searchFilter.append(SparqlUtils.buildRegex(
    						SparqlUtils.getColumn(j), searchTerms[i]));
    		        
    				if(j < SparqlUtils.getColumnCount() - 1)
    					searchFilter.append(" || ");
    			}
    			searchFilter.append(")");
    			
    			if(i < searchTerms.length -1)
					searchFilter.append(" && ");    			
    		}
    		searchFilter.append(") . ");
    		filter = searchFilter.toString();
    	}
    	
    	return filter;
    }
        
    private Map<String, String> getRequestQueryValues(){
    	Form form = getRequest().getResourceRef().getQueryAsForm();
    	
    	return form.getValuesMap();
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
    
    /*
     * Gets the total number of unfiltered records
     * 
     * @param whether to include deprecated entries or not
     * 
     * @return the total number of records
     */
    private long getTotalRecords(String deprecatedFlag){
    	StringBuilder query = new StringBuilder(
        		SparqlUtils.SELECT_COUNT
                + " WHERE {"
                + SparqlUtils.WHERE_BLOCK
                + SparqlUtils.getLatestFilter(getCurrentDate()));
    	
    	if (deprecatedFlag.equals("off")){
        	query.append(" FILTER (!bound (?deprecated))");
        } else if (deprecatedFlag.equals("only")){
        	query.append(" FILTER (bound (?deprecated))");
        }
    	query.append(" }");
    	
    	String iTotalRecords = "0";
    	
    	try {
    		List<Map<String, String>> results = query(query.toString());
        	
    		if(results.size() > 0){
    			iTotalRecords = (String)((Map<String, String>)results.remove(0)).get("count");
    		}
    	} catch(MarketplaceException e){
    		LOGGER.severe(e.getMessage());
    	}
    	
    	return Long.parseLong(iTotalRecords);
    }
        
    private static void closeReliably(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException consumed) {

            }
        }
    }

}
