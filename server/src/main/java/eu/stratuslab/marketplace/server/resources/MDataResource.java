package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.server.resources.XPathUtils.CREATED_DATE;
import static eu.stratuslab.marketplace.server.resources.XPathUtils.EMAIL;
import static eu.stratuslab.marketplace.server.resources.XPathUtils.IDENTIFIER_ELEMENT;
import static org.restlet.data.MediaType.MULTIPART_FORM_DATA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;

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
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.metadata.MetadataException;
import eu.stratuslab.marketplace.metadata.MetadataUtils;
import eu.stratuslab.marketplace.metadata.ValidateMetadataConstraints;
import eu.stratuslab.marketplace.metadata.ValidateRDFModel;
import eu.stratuslab.marketplace.metadata.ValidateXMLSignature;

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
    public Representation acceptMetadatum(Representation entity)
            throws ResourceException {

        Representation result = null;

        List<File> files = new ArrayList<File>();

        if (entity != null) {
            if (MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
                processMultipartForm(files);
            } else {
                writeContentsToDisk(files, entity);
            }
        } else {
            // POST request with no entity.
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }

        // Process all of the uploaded files.
        for (File file : files) {

            InputStream stream = null;

            try {

                stream = new FileInputStream(file);

                Document doc = extractXmlDocument(stream);

                result = validateMetadata(doc);

                if (result == null) {

                    writeMetadataToDisk(getDataDir(), doc);
                    String iri = writeMetadataToStore(doc);

                    setStatus(Status.SUCCESS_CREATED);
                    Representation rep = new StringRepresentation(
                            "Metadata entry created.\n", MediaType.TEXT_PLAIN);
                    rep.setLocationRef(getRequest().getResourceRef()
                            .getIdentifier()
                            + iri);
                    result = rep;
                }

            } catch (FileNotFoundException e) {
                // FIXME: Log this. It shouldn't happen.
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException consumed) {

                    }
                }
            }

        }

        getResponse().setEntity(result);

        return result;
    }

    private void processMultipartForm(List<File> files) {

        File storeDirectory = new File("/tmp/");

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(102400000);

        RestletFileUpload upload = new RestletFileUpload(factory);

        List<FileItem> items;

        try {
            Request request = getRequest();
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            e.printStackTrace();
            items = new ArrayList<FileItem>();
        }

        for (FileItem fi : items) {
            if (fi.getName() != null) {
                String uuid = UUID.randomUUID().toString();
                File file = new File(storeDirectory, uuid);
                try {
                    fi.write(file);
                    files.add(file);
                } catch (Exception consumed) {
                }
            }
        }

    }

    private static void writeMetadataToDisk(String dataDir, Document doc)
            throws ResourceException {

        String[] coordinates = getMetadataEntryCoordinates(doc);

        String identifier = coordinates[0];
        String endorser = coordinates[1];
        String created = coordinates[2];

        File rdfFile = new File(dataDir, identifier + File.separator + endorser
                + File.separator + created + ".xml");

        File rdfFileParent = rdfFile.getParentFile();
        if (!rdfFileParent.exists()) {
            if (!rdfFileParent.mkdirs()) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
            }
        }

        String contents = XMLUtils.documentToString(doc);
        MetadataUtils.writeStringToFile(contents, rdfFile);

    }

    private String writeMetadataToStore(Document datumDoc) {

        String[] coordinates = getMetadataEntryCoordinates(datumDoc);

        String identifier = coordinates[0];
        String endorser = coordinates[1];
        String created = coordinates[2];

        String ref = getRequest().getResourceRef().toString();
        String iri = ref + "/" + identifier + "/" + endorser + "/" + created;

        String rdfEntry = createRdfEntry(datumDoc);
        storeMetadatum(iri, rdfEntry);

        return iri;
    }

    private static String[] getMetadataEntryCoordinates(Document doc) {

        String[] coords = new String[3];

        coords[0] = XPathUtils.getValue(doc, IDENTIFIER_ELEMENT);
        coords[1] = XPathUtils.getValue(doc, EMAIL);
        coords[2] = XPathUtils.getValue(doc, CREATED_DATE);

        return coords;
    }

    private Representation validateMetadata(Document doc) {

        try {

            ValidateXMLSignature.validate(doc);
            ValidateMetadataConstraints.validate(doc);
            ValidateRDFModel.validate(doc);

            return null;

        } catch (MetadataException m) {
            m.printStackTrace();
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new StringRepresentation(m.getMessage() + "\n",
                    MediaType.TEXT_PLAIN);

        }

    }

    // Create a deep copy of the document and strip signature elements.
    private static String createRdfEntry(Document doc) {
        Document copy = (Document) doc.cloneNode(true);
        MetadataUtils.stripSignatureElements(copy);
        return XMLUtils.documentToString(copy);
    }

    private static Document extractXmlDocument(InputStream stream) {

        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document datumDoc = null;

        try {

            datumDoc = db.parse(stream);

        } catch (SAXException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Unable to parse metadata: " + e.getMessage());
        } catch (IOException e) {
            throw new ResourceException(e);
        }

        return datumDoc;
    }

    private static void writeContentsToDisk(List<File> files,
            Representation entity) {

        char[] buffer = new char[4096];

        File output = new File("/tmp", UUID.randomUUID().toString());

        Reader reader = null;

        FileWriter writer = null;

        try {

            writer = new FileWriter(output);

            reader = entity.getReader();

            int nchars = reader.read(buffer);
            while (nchars >= 0) {
                writer.write(buffer, 0, nchars);
                nchars = reader.read(buffer);
            }

            files.add(output);

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

    }

    @Get("html")
    public Representation toHtml() {

        ArrayList<HashMap<String, String>> results = getMetadata();
        HashMap<String, Object> data = new HashMap<String, Object>();
        HashMap<String, Object> root = new HashMap<String, Object>();

        for (HashMap<String, String> resultRow : results) {

            String identifier = resultRow.get("identifier");
            String endorser = resultRow.get("email");
            String created = resultRow.get("created");
            logger
                    .log(Level.INFO, identifier + "  " + endorser + " "
                            + created);

            if (root.containsKey(identifier)) {
                HashMap<String, String> endorserMap = (HashMap<String, String>) root
                        .get(identifier);
                endorserMap.put(endorser, created);
                root.put(identifier, endorserMap);
            } else {
                HashMap<String, Object> endorserMap = new HashMap<String, Object>();
                endorserMap.put(endorser, created);
                root.put(identifier, endorserMap);
            }

        }

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

        ArrayList<HashMap<String, String>> results = getMetadata();

        ArrayList<String> uris = new ArrayList<String>();
        for (HashMap<String, String> resultRow : results) {

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

    private ArrayList<HashMap<String, String>> getMetadata() {
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

            ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>) query(queryString
                    .toString());

            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
