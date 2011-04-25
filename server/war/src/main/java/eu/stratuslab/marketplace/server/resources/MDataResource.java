package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.server.cfg.Parameter.METADATA_MAX_BYTES;
import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.VALIDATE_EMAIL;
import static org.restlet.data.MediaType.APPLICATION_RDF_XML;
import static org.restlet.data.MediaType.APPLICATION_XML;
import static org.restlet.data.MediaType.MULTIPART_FORM_DATA;
import static org.restlet.data.MediaType.TEXT_PLAIN;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
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
            // TODO: Log this.
            e.printStackTrace();
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
            // TODO: Log this.
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "unable to read metadata file");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException consumed) {

                }
            }
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
            try {
                reader.close();
            } catch (IOException consumed) {

            }
            try {
                writer.close();
            } catch (IOException consumed) {

            }
        }
        return output;
    }

    @Get("html")
    public Representation toHtml() {

        List<Map<String, String>> results = getMetadata();

        HashMap<String, HashMap<String, Object>> root = new HashMap<String, HashMap<String, Object>>();

        for (Map<String, String> resultRow : results) {

            String identifier = resultRow.get("identifier");
            String endorser = resultRow.get("email");
            String created = resultRow.get("created");
            logger
                    .log(Level.INFO, identifier + "  " + endorser + " "
                            + created);

            HashMap<String, Object> endorserMap;
            if (root.containsKey(identifier)) {
                endorserMap = root.get(identifier);
            } else {
                endorserMap = new HashMap<String, Object>();
            }

            endorserMap.put(endorser, created);
            root.put(identifier, endorserMap);

        }

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("title", "Metadata");
        data.put("content", root);

        // Load the FreeMarker template
        Representation listFtl = new ClientResource(LocalReference
                .createClapReference("/metadata.ftl")).get();
        // Wraps the bean with a FreeMarker representation
        Representation representation = new TemplateRepresentation(listFtl,
                data, MediaType.TEXT_HTML);

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

        StringBuffer output = new StringBuffer(XML_HEADER);

        for (String uri : uris) {
            String datum = getMetadatum(getDataDir() + File.separatorChar + uri
                    + ".xml");
            if (datum.startsWith(XML_HEADER)) {
                datum = datum.substring(XML_HEADER.length());
            }
            output.append(datum);
        }

        // Returns the XML representation of this document.
        StringRepresentation representation = new StringRepresentation(output,
                MediaType.APPLICATION_XML);

        return representation;
    }

    private List<Map<String, String>> getMetadata() {
        try {
            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            Map<String, Object> requestAttr = getRequest().getAttributes();
            boolean dateSearch = false;

            StringBuffer filterPredicate = new StringBuffer();

            for (Map.Entry<String, Object> arg : requestAttr.entrySet()) {

                String key = arg.getKey();
                if (!key.startsWith("org.restlet")) {
                    switch (classifyArg((String) arg.getValue())) {
                    case ARG_EMAIL:
                        filterPredicate.append(" FILTER (?email = \""
                                + arg.getValue() + "\"). ");
                        break;
                    case ARG_DATE:
                        dateSearch = true;
                        filterPredicate.append(" FILTER (?created = \""
                                + arg.getValue() + "\"). ");
                        break;
                    case ARG_OTHER:
                        filterPredicate.append(" FILTER (?identifier = \""
                                + arg.getValue() + "\"). ");
                        break;
                    default:
                        break;
                    }
                }
            }

            filterPredicate.append(formToString(queryForm));

            StringBuffer queryString = new StringBuffer(
                    "SELECT DISTINCT ?identifier ?email ?created "
                            + " WHERE {"
                            + " ?x <http://purl.org/dc/terms/identifier>  ?identifier; "
                            + " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ."
                            + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;"
                            + " <http://purl.org/dc/terms/created> ?created ."
                            + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .");

            queryString.append(filterPredicate.toString());

            if (!dateSearch && !queryForm.getNames().contains("created")) {
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

            queryString.append(" }");

            return query(queryString.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
