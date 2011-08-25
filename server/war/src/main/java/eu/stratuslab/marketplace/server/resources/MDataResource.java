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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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

/**
 * This resource represents a list of all Metadata entries
 */
public class MDataResource extends BaseResource {

    private static final Logger LOGGER = Logger.getLogger("org.restlet");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
    	DATE_FORMAT.setLenient(false);
    	DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

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

        List<Map<String, String>> results = getMetadata();
        
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> root =
            new HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>>();
        
        for (Map<String, String> resultRow : results) {

            String identifier = resultRow.get("identifier");
            String endorser = resultRow.get("email");
            String created = resultRow.get("created");  
            String os = resultRow.get("os");
            String osversion = resultRow.get("osversion");
            String arch = resultRow.get("arch");
            String location = resultRow.get("location");
            String description = resultRow.get("description");
            
            HashMap<String, HashMap<String, HashMap<String, String>>> endorserMap;
            if (root.containsKey(identifier)) {
                    endorserMap = root.get(identifier);
            } else {
                    endorserMap = new HashMap<String, HashMap<String, HashMap<String, String>>>();
            }

            HashMap<String, HashMap<String, String>> dataMap;
            if (endorserMap.containsKey(endorser)) {
                    dataMap = endorserMap.get(endorser);
            } else {
                    dataMap = new HashMap<String, HashMap<String, String>>();
            }

            HashMap<String, String> dMap = new HashMap<String, String>();
            dMap.put("os", os);
            dMap.put("osversion", osversion);
            dMap.put("arch", arch);
            dMap.put("location", location);
            dMap.put("description", description);
            
            dataMap.put(created, dMap);
            endorserMap.put(endorser, dataMap);
            root.put(identifier, endorserMap);
        }

        Map<String, Object> data = createInfoStructure("Metadata");
        data.put("content", root);

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

        List<Map<String, String>> results = getMetadata();
        
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

    private List<Map<String, String>> getMetadata() {

    	Map<String, Object> requestAttr = getRequest().getAttributes();
    	boolean dateSearch = false;

    	StringBuilder filterPredicate = new StringBuilder();
    	boolean filter = false;

    	for (Map.Entry<String, Object> arg : requestAttr.entrySet()) {

    		String key = arg.getKey();
    		if (!key.startsWith("org.restlet")) {
    			switch (classifyArg((String) arg.getValue())) {
    			case ARG_EMAIL:
    				filter = true;
    				filterPredicate.append(" FILTER (?email = \""
    						+ arg.getValue() + "\"). ");
    				break;
    			case ARG_DATE:
    				filter = true;
    				dateSearch = true;
    				filterPredicate.append(" FILTER (?created = \""
    						+ arg.getValue() + "\"). ");
    				break;
    			case ARG_OTHER:
    				filter = true;
    				filterPredicate.append(" FILTER (?identifier = \""
    						+ arg.getValue() + "\"). ");
    				break;
    			default:
    				break;
    			}
    		}
    	}

        String datetime = DATE_FORMAT.format(new Date());
        
        StringBuilder queryString = new StringBuilder(
            		"SELECT ?identifier ?email ?created"
            		+ " ?os ?osversion ?arch ?location ?description"
                    + " WHERE {"
                    + " ?x <http://purl.org/dc/terms/identifier>  ?identifier ."
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os> ?os . }"
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-version> ?osversion . }"
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-arch> ?arch . }"
                    + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#location> ?location . }"
                    + " OPTIONAL { ?x <http://purl.org/dc/terms/description> ?description . }"
                    + " ?x <http://purl.org/dc/terms/valid> ?valid;"
                    + " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ."
                    + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;"
                    + " <http://purl.org/dc/terms/created> ?created ."
                    + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .");

        queryString.append(filterPredicate.toString());

        if (!dateSearch) {
                queryString
                        .append(" OPTIONAL { "
                                + " ?lx <http://purl.org/dc/terms/identifier>  ?lidentifier; "
                                + " <http://mp.stratuslab.eu/slreq#endorsement> ?lendorsement ."
                                + " ?lendorsement <http://mp.stratuslab.eu/slreq#endorser> ?lendorser;"
                                + " <http://purl.org/dc/terms/created> ?latestcreated ."
                                + " ?lendorser <http://mp.stratuslab.eu/slreq#email> ?lemail ."
                                + " FILTER (?lidentifier = ?identifier) ."
                                + " FILTER (?lemail = ?email) ."
                                + " FILTER (?latestcreated > ?created) . } FILTER (!bound (?lendorsement))");
        }
        queryString.append(" . FILTER (?valid > \"" + datetime + "\") ");
        queryString.append(" }");

        List<Map<String, String>> results = query(queryString.toString());

        if(results.size() <= 0 && filter){
		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "no metadata matching query found");
        } else {
        	return results;
        }
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
