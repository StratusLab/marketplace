package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.server.cfg.Parameter.METADATA_MAX_BYTES;
import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.VALIDATE_EMAIL;
import static org.restlet.data.MediaType.APPLICATION_RDF_XML;
import static org.restlet.data.MediaType.APPLICATION_XML;
import static org.restlet.data.MediaType.MULTIPART_FORM_DATA;
import static org.restlet.data.MediaType.TEXT_PLAIN;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;

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
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MessageUtils;
import eu.stratuslab.marketplace.server.utils.Notifier;
import eu.stratuslab.marketplace.server.utils.SparqlUtils;

/**
 * This resource represents a list of all Metadata entries
 */
public class MDataResource extends BaseResource {
    
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

        File uploadedFile = null;
        if (MULTIPART_FORM_DATA.equals(mediaType, true)) {
            uploadedFile = processMultipartForm();
        } else if (APPLICATION_RDF_XML.equals(mediaType, true)
                || (APPLICATION_XML.equals(mediaType, true))) {
            uploadedFile = writeContentsToDisk(entity);
        } else {
            throw new ResourceException(
                    Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, mediaType
                            .getName());
        }

        boolean validateEmail = Configuration
                .getParameterValueAsBoolean(VALIDATE_EMAIL);

        Document doc = validateMetadata(uploadedFile);

        if (!validateEmail) {

            String iri = commitMetadataEntry(uploadedFile, doc);

            setStatus(Status.SUCCESS_CREATED);
            Representation rep = new StringRepresentation(
                    "metadata entry created.\n", TEXT_PLAIN);
            rep.setLocationRef(iri);

            return rep;

        } else {

            confirmMetadataEntry(uploadedFile, doc);

            setStatus(Status.SUCCESS_ACCEPTED);
            return new StringRepresentation(
                    "confirmation email sent for new metadata entry\n",
                    TEXT_PLAIN);
        }

    }

    private static void confirmMetadataEntry(File uploadedFile, Document doc) {

        try {
            String[] coords = getMetadataEntryCoordinates(doc);
            sendEmailConfirmation(coords[1], uploadedFile);
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

    private Document validateMetadata(File uploadedFile) {

        InputStream stream = null;
        Document doc = null;

        try {

            stream = new FileInputStream(uploadedFile);

            doc = extractXmlDocument(stream);

            ValidateXMLSignature.validate(doc);
            ValidateMetadataConstraints.validate(doc);
            ValidateRDFModel.validate(doc);

        } catch (MetadataException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "invalid metadata: " + e.getMessage());
        } catch (FileNotFoundException e) {
            LOGGER.severe("unable to read metadata file: "
                    + uploadedFile.getAbsolutePath());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "unable to read metadata file");
        } finally {
            closeReliably(stream);
        }

        return doc;
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
    public Representation toHtml() {
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
    	Form form = getRequest().getResourceRef().getQueryAsForm();
    	Map<String, String> formValues = form.getValuesMap();
    	    	
    	String deprecatedValue = (formValues.containsKey("deprecated")) ? 
    			getDeprecatedFlag(formValues.get("deprecated")) : "off";
    			
    	List<Map<String, String>> results = getMetadata(deprecatedValue);
        results.remove(0);
    	
        ArrayList<String> uris = new ArrayList<String>();
        for (Map<String, String> resultRow : results) {

            String iri = resultRow.get("identifier") + "/"
                    + resultRow.get("email") + "/" + resultRow.get("created");
            uris.add(iri);
        }

        StringBuilder output = new StringBuilder(XML_HEADER);
        
        output.append("<metadata>");
        for (String uri : uris) {
            String datum = getMetadatum(getDataDir() + File.separatorChar + uri
                    + ".xml");
            if (datum.startsWith(XML_HEADER)) {
                datum = datum.substring(XML_HEADER.length());
            }
            output.append(datum);
        }
        output.append("</metadata>");
        
        // Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(output,
                MediaType.APPLICATION_XML);

        return representation;
    }

    /**
     * Returns a listing of all registered metadata or a particular entry if
     * specified.
     */
    @Get("json")
    public Representation toJSON() {
    	Form form = getRequest().getResourceRef().getQueryAsForm();
    	Map<String, String> formValues = form.getValuesMap();
    	
    	String deprecatedValue = (formValues.containsKey("deprecated")) ? 
    			getDeprecatedFlag(formValues.get("deprecated")) : "off";
    	
    	List<Map<String, String>> results = null;
    	
    	try {
    		results = getMetadata(deprecatedValue);
    	} catch(ResourceException r){
    		results = new ArrayList();
    	}
    	
    	long iTotalRecords = getTotalRecords(deprecatedValue);
    	String iTotalDisplayRecords = "0";
    	if(results.size() > 0){
    		iTotalDisplayRecords = (String)((Map<String, String>)results.remove(0)).get("count");
    	}
    	
    	StringBuilder json = new StringBuilder("{ \"sEcho\":\"" 
    			+ formValues.get("sEcho") + "\", ");
    	json.append("\"iTotalRecords\":" + iTotalRecords + ", ");
    	json.append("\"iTotalDisplayRecords\":" + iTotalDisplayRecords + ", ");
    	json.append("\"aaData\":[ ");
    	
    	for(int i = 0; i < results.size(); i++){
    		Map<String, String> resultRow = (Map<String, String>)results.get(i);

    		String identifier = resultRow.get("identifier");
    		String endorser = resultRow.get("email");
    		String created = resultRow.get("created");  
    		String os = resultRow.get("os");
    		String osversion = resultRow.get("osversion");
    		String arch = resultRow.get("arch");
    		String location = resultRow.get("location");
    		String description = resultRow.get("description");

    		json.append("[\"<img src='/css/details_open.png'>\", " + 
    				"\"" + os + "\"," +
    				"\"" + osversion + "\"," +
    				"\"" + arch + "\"," +
    				"\"" + endorser + "\"," +
    				"\"" + created + "\"," +
    				"\"" + identifier + "\"," +
    				"\"" + location + "\"," +
    				"\"" + description.replaceAll("\"", "&quot;") + "\"" +
    		"]");

    		if(i < results.size() -1){
    			json.append(",");
    		}
    	}
    	
    	json.append("]}");
             
    	String jsonString = json.toString().replaceAll("\n", "<br>");
    	
    	// Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(jsonString,
                MediaType.APPLICATION_JSON);

        return representation;
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
    	StringBuilder filterPredicate = new StringBuilder();
    	boolean filter = false;

    	Form form = getRequest().getResourceRef().getQueryAsForm();
    	Map<String, String> formValues = form.getValuesMap();
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
    				filter = true;
    				filterPredicate.append(
    						SparqlUtils.buildFilterEq("email", (String)arg.getValue()));
    				break;
    			case ARG_DATE:
    				filter = true;
    				dateSearch = true;
    				filterPredicate.append(
    						SparqlUtils.buildFilterEq("created", (String)arg.getValue()));
    				break;
    			case ARG_OTHER:
    				filter = true;
    				filterPredicate.append(
    						SparqlUtils.buildFilterEq("identifier", (String)arg.getValue()));
    				break;
    			default:
    				break;
    			}
    		}
    	}

    	String paging = buildPagingFilter(formValues.get("iDisplayStart"), formValues.get("iDisplayLength"),
    			formValues.get("iSortCol_0"), formValues.get("sSortDir_0"));    	
    	String searching = buildSearchingFilter(formValues);
    	    	
    	StringBuilder queryString = new StringBuilder(
            		"SELECT ?identifier ?email ?created"
            		+ " ?os ?osversion ?arch ?location ?description");
        StringBuilder counterString = new StringBuilder(
        		    "SELECT DISTINCT(COUNT(*) AS ?count)");
                
        StringBuilder filterString = new StringBuilder(
                    " WHERE {"
                    + " ?x <http://purl.org/dc/terms/identifier>  ?identifier ."
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os> ?os . }"
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-version> ?osversion . }"
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-arch> ?arch . }"
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#location> ?location . }"
                    + " OPTIONAL { ?x <http://purl.org/dc/terms/description> ?description . }"
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated . }"
                    + " ?x <http://purl.org/dc/terms/valid> ?valid;"
                    + " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ."
                    + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;"
                    + " <http://purl.org/dc/terms/created> ?created ."
                    + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .");

        filterString.append(filterPredicate.toString());

        if (!dateSearch) {
                filterString
                        .append(" OPTIONAL { "
                                + " ?lx <http://purl.org/dc/terms/identifier>  ?lidentifier; "
                                + " <http://mp.stratuslab.eu/slreq#endorsement> ?lendorsement ."
                                + " ?lendorsement <http://mp.stratuslab.eu/slreq#endorser> ?lendorser;"
                                + " <http://purl.org/dc/terms/created> ?latestcreated ."
                                + " ?lendorser <http://mp.stratuslab.eu/slreq#email> ?lemail ."
                                + " FILTER (?lidentifier = ?identifier) ."
                                + " FILTER (?lemail = ?email) ."
                                + " FILTER (?latestcreated > ?created) . } FILTER (!bound (?lendorsement)) .");
                filterString.append(" FILTER (?valid > \"" + getCurrentDate() + "\") .");
        }
                     
        if (deprecatedValue.equals("off")){
        	filterString.append(" FILTER (!bound (?deprecated))");
        } else if (deprecatedValue.equals("only")){
        	filterString.append(" FILTER (bound (?deprecated))");
        }
                
        filterString.append(searching);
        filterString.append(" }");

        counterString.append(filterString);
        List<Map<String, String>> countResult = query(counterString.toString());
        Map<String, String> count = new HashMap();
        count.put("count", "0");
        if(countResult.size() > 0){
        	count = (Map<String, String>)countResult.get(0);
        }
                
        queryString.append(filterString);
        queryString.append(paging);
                
        List<Map<String, String>> results = query(queryString.toString());

        if(results.size() <= 0 && filter){
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
    				Integer.parseInt(iSortCol) : 1;
    		String sortCol = SparqlUtils.getColumn(sort);
    		      		
    		paging = SparqlUtils.buildLimit(sortCol, iDisplayLength, iDisplayStart, sSortDir);
    	}
    	
    	return paging;
    }
    
    private String buildSearchingFilter(Map<String, String> formValues){
    	String searching = "";
    	
    	if(formValues.get("sSearch") != null){
    		String sSearch = formValues.get("sSearch");
    		
    		String[] searchTerms = sSearch.split(" ");
    		StringBuilder searchAllFilter = new StringBuilder(" FILTER (");
    		for(int i = 0; i < searchTerms.length; i++){
    			
    			searchAllFilter.append("(");
    			for(int j = 1; j < SparqlUtils.getColumnCount(); j++){
    				searchAllFilter.append(SparqlUtils.buildRegex(
    						SparqlUtils.getColumn(j), searchTerms[i]));
    		        
    				if(j < SparqlUtils.getColumnCount() - 1)
    					searchAllFilter.append(" || ");
    			}
    			searchAllFilter.append(")");
    			
    			if(i < searchTerms.length -1)
					searchAllFilter.append(" && ");    			
    		}
    		searchAllFilter.append(") . ");
    		searching = searchAllFilter.toString();
    	}
    	
    	StringBuilder searchColumnsPredicate = new StringBuilder();
    	for(int i = 1; i < 6; i++){
    		if ( formValues.get("sSearch_" + i) != null )
    		{
    			if ( searchColumnsPredicate.length() == 0 )
    			{
    				searchColumnsPredicate.append(" FILTER (");
    			}
    			else
    			{
    				searchColumnsPredicate.append(" && ");
    			}
    			String word = formValues.get("sSearch_" + i);
    			searchColumnsPredicate.append(SparqlUtils.buildRegex(
    					SparqlUtils.getColumn(i), word));
    		}
    	}
    	if(searchColumnsPredicate.length() != 0){
    		searchColumnsPredicate.append(" ) . ");
    	}
    	searching += searchColumnsPredicate.toString();
    	
    	return searching;
    }
    
    private String getDeprecatedFlag(String deprecated){
    	return (deprecated == null) ? "on" : deprecated;
    }
    
    private long getTotalRecords(String deprecatedValue){
    	String queryString = 
        		"SELECT DISTINCT(COUNT(*) AS ?count)"
                + " WHERE {"
                + " ?x <http://purl.org/dc/terms/identifier>  ?identifier ."
                + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os> ?os . }"
                + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-version> ?osversion . }"
                + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-arch> ?arch . }"
                + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#location> ?location . }"
                + " OPTIONAL { ?x <http://purl.org/dc/terms/description> ?description . }"
                + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated . }"
                + " ?x <http://purl.org/dc/terms/valid> ?valid;"
                + " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ."
                + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;"
                + " <http://purl.org/dc/terms/created> ?created ."
                + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email ."
                + " OPTIONAL { "
                + " ?lx <http://purl.org/dc/terms/identifier>  ?lidentifier; "
                + " <http://mp.stratuslab.eu/slreq#endorsement> ?lendorsement ."
                + " ?lendorsement <http://mp.stratuslab.eu/slreq#endorser> ?lendorser;"
                + " <http://purl.org/dc/terms/created> ?latestcreated ."
                + " ?lendorser <http://mp.stratuslab.eu/slreq#email> ?lemail ."
                + " FILTER (?lidentifier = ?identifier) ."
                + " FILTER (?lemail = ?email) ."
                + " FILTER (?latestcreated > ?created) . } FILTER (!bound (?lendorsement)) ."
                + " FILTER (?valid > \"" + getCurrentDate() + "\") .";
    	
    	if (deprecatedValue.equals("off")){
        	queryString += " FILTER (!bound (?deprecated))";
        } else if (deprecatedValue.equals("only")){
        	queryString += " FILTER (bound (?deprecated))";
        }
    	queryString += " }";
    	
    	List<Map<String, String>> totalResults = query(queryString);
    	String iTotalRecords = "0";
    	
    	if(totalResults.size() > 0){
    		iTotalRecords = (String)((Map<String, String>)totalResults.remove(0)).get("count");
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
