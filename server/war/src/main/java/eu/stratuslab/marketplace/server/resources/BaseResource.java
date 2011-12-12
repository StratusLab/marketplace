package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.CREATED_DATE;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.EMAIL;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.IDENTIFIER_ELEMENT;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.PatternUtils;
import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.metadata.MetadataUtils;
import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.utils.XPathUtils;

/**

 *  Base resource class that supports common behaviours or attributes shared by
 *  all resources.
 */
/**
 * @author stkenny
 * 
 */
public abstract class BaseResource extends ServerResource {

	protected static final Logger LOGGER = Logger.getLogger("org.restlet");

    protected static final int ARG_EMAIL = 1;
    protected static final int ARG_DATE = 2;
    protected static final int ARG_OTHER = 3;

    protected static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
    protected static final String NO_TITLE = null;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
    	DATE_FORMAT.setLenient(false);
    	DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    protected Repository getMetadataStore() {
        return ((MarketPlaceApplication) getApplication()).getMetadataStore();
    }

    protected String getDataDir() {
        return ((MarketPlaceApplication) getApplication()).getDataDir();
    }

    protected freemarker.template.Configuration getFreeMarkerConfiguration() {
        return ((MarketPlaceApplication) getApplication())
                .getFreeMarkerConfiguration();
    }

    protected TemplateRepresentation createTemplateRepresentation(String tpl,
            Map<String, Object> info, MediaType mediaType) {

        freemarker.template.Configuration freeMarkerConfig = getFreeMarkerConfiguration();
                
        return new TemplateRepresentation(tpl, freeMarkerConfig, info,
                mediaType);
    }

    protected Map<String, Object> createInfoStructure(String title) {

        Map<String, Object> info = new HashMap<String, Object>();

        // Add the standard base URL declaration.
        info.put("baseurl", getRequest().getRootRef().toString());

        // Add the title if appropriate.
        if (title != null && !"".equals(title)) {
            info.put("title", title);
        }

        return info;
    }

    protected static Document extractXmlDocument(InputStream stream) {

        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document datumDoc = null;

        try {

            datumDoc = db.parse(stream);

        } catch (SAXException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "unable to parse metadata: " + e.getMessage());
        } catch (IOException e) {
            throw new ResourceException(e);
        }

        return datumDoc;
    }

    protected String commitMetadataEntry(File uploadedFile, Document doc) {
        writeMetadataToDisk(getDataDir(), doc);
        String iri = null;
        try {
           iri = writeMetadataToStore(doc);
        } catch(ResourceException e){
        	//transaction has failed, so rollback
        	deleteMetadataFromDisk(getDataDir(), doc);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        }

        if (!uploadedFile.delete()) {
            LOGGER
                    .severe("uploaded file could not be deleted: "
                            + uploadedFile);
        }

        return iri;
    }

    protected static void writeMetadataToDisk(String dataDir, Document doc) {

        String[] coordinates = getMetadataEntryCoordinates(doc);

        String identifier = coordinates[0];
        String endorser = coordinates[1];
        String created = coordinates[2];

        File rdfFile = new File(dataDir, identifier + File.separator + endorser
                + File.separator + created + ".xml");

        File rdfFileParent = rdfFile.getParentFile();
        if (!rdfFileParent.exists()) {
            if (!rdfFileParent.mkdirs()) {
            	LOGGER.severe("Unable to create directory structure for file.");
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
            }
        }

        String contents = XMLUtils.documentToString(doc);
        MetadataUtils.writeStringToFile(contents, rdfFile);

    }
    
    protected static void deleteMetadataFromDisk(String dataDir, Document doc) {

        String[] coordinates = getMetadataEntryCoordinates(doc);

        String identifier = coordinates[0];
        String endorser = coordinates[1];
        String created = coordinates[2];

        File rdfFile = new File(dataDir, identifier + File.separator + endorser
                + File.separator + created + ".xml");

        if (rdfFile.exists()) {
            if (!rdfFile.delete()) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
            }
        }

    }

    // Create a deep copy of the document and strip signature elements.
    protected static String createRdfEntry(Document doc) {
        Document copy = (Document) doc.cloneNode(true);
        MetadataUtils.stripSignatureElements(copy);
        String rdfEntry = XMLUtils.documentToString(copy);
        String[] coords = getMetadataEntryCoordinates(copy);
        rdfEntry = rdfEntry.replaceFirst("<rdf:Description rdf:about=\"#" + coords[0] + "\">",
        		"<rdf:Description rdf:about=\"#" + coords[0] + "/" + coords[1] + "/" + coords[2] + "\">");
        return rdfEntry;
    }

    protected static String[] getMetadataEntryCoordinates(Document doc) {

        String[] coords = new String[3];

        coords[0] = XPathUtils.getValue(doc, IDENTIFIER_ELEMENT);
        coords[1] = XPathUtils.getValue(doc, EMAIL);
        coords[2] = XPathUtils.getValue(doc, CREATED_DATE);

        return coords;
    }

    protected String writeMetadataToStore(Document datumDoc) {

        String[] coordinates = getMetadataEntryCoordinates(datumDoc);

        String identifier = coordinates[0];
        String endorser = coordinates[1];
        String created = coordinates[2];

        String ref = getRequest().getResourceRef().toString();
        String iri = ref + "/" + identifier + "/" + endorser + "/" + created;
              
        String rdfEntry = createRdfEntry(datumDoc);
        if(!storeMetadatum(iri, rdfEntry)){
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        }

        return iri;
    }

    /**
     * Stores a new metadata entry
     * 
     * @param iri
     *            identifier of the metadata entry
     * @param rdf
     *            the metadata entry to store
     */
    protected boolean storeMetadatum(String iri, String rdf) {
    	boolean success = false;
        try {
            RepositoryConnection con = getMetadataStore().getConnection();
            ValueFactory vf = con.getValueFactory();
            Reader reader = new StringReader(rdf);
            try {
            	con.clear(vf.createURI(iri));
                con.add(reader, MARKETPLACE_URI, RDFFormat.RDFXML, vf
                        .createURI(iri));
            } finally {
                con.close();
            }
            success = true;
        } catch (RepositoryException e) {
            LOGGER.severe("Unable to clear metadata entry: " + e.getMessage());
        } catch (java.io.IOException e) {
            LOGGER.severe("Error storing metadata entry: " + e.getMessage());
        } catch(org.openrdf.rio.RDFParseException e){
        	LOGGER.severe(e.getMessage());
        }
        
        return success;
    }

    /**
     * Retrieve a particular metadata entry
     * 
     * @param iri
     *            identifier of the metadata entry
     * @return metadata entry as a Jena model
     */
    protected String getMetadatum(String iri) {
        String model = null;
        try {
            model = readFileAsString(iri);           
        } catch (IOException e) {
            LOGGER.severe("Unable to read metadata file: " + iri);
        }

        return model;
    }
    
    /**
     * Remove a metadata entry
     * 
     * @param iri
     *            identifier of the metadata entry
     */
    protected void removeMetadatum(String iri) {
        try {
            RepositoryConnection con = getMetadataStore().getConnection();
            ValueFactory vf = con.getValueFactory();
            try {
                con.clear(vf.createURI(iri));
            } finally {
                con.close();
            }
        } catch (RepositoryException e) {
            LOGGER.severe("Error removing metadata entry: " + e.getMessage());
        }
    }

    /**
     * Query the metadata
     * 
     * @param queryString
     *            query string
     * @param syntax
     *            the syntax of the query
     * @param format
     *            the output format of the resultset
     * @return
     */
    protected String query(String queryString, QueryLanguage syntax) {
        String resultString = null;

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bytes);
            SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(
                    out);

            RepositoryConnection con = getMetadataStore().getConnection();
            try {
                TupleQuery tupleQuery = con.prepareTupleQuery(syntax,
                        queryString);
                tupleQuery.evaluate(sparqlWriter);
                resultString = bytes.toString();
            } finally {
                con.close();
            }
        } catch (RepositoryException e) {
        	LOGGER.severe("Error accessing repository: " + e.getMessage());
        } catch (MalformedQueryException e) {
        	LOGGER.severe("Malformed query: " + e.getMessage());
        } catch (QueryEvaluationException e) {
        	LOGGER.severe("Error processing query: " + e.getMessage());
        } catch (org.openrdf.query.TupleQueryResultHandlerException e){
        	LOGGER.severe(e.getMessage());
        }
        return resultString;
    }

    /**
     * Query the metadata
     * 
     * @param queryString
     *            the query
     * @return the resultset as a Java Collection
     */
    protected List<Map<String, String>> query(String queryString) 
    throws MarketplaceException {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        
        try {
        	RepositoryConnection con = getMetadataStore().getConnection();
        	try {
        		TupleQuery tupleQuery = con.prepareTupleQuery(
        				QueryLanguage.SPARQL, queryString);
        		TupleQueryResult results = tupleQuery.evaluate();
        		try {
        			List<String> columnNames = results.getBindingNames();
        			int cols = columnNames.size();

        			while (results.hasNext()) {
        				BindingSet solution = results.next();
        				HashMap<String, String> row = new HashMap<String, String>(
        						cols, 1);
        				for (Iterator<String> namesIter = columnNames
        						.listIterator(); namesIter.hasNext();) {
        					String columnName = namesIter.next();
        					Value columnValue = solution.getValue(columnName);
        					if (columnValue != null) {
        						row.put(columnName, (solution
        								.getValue(columnName)).stringValue());
        					} else {
        						row.put(columnName, "null");
        					}
        				}
        				list.add(row);
        			}
        		} finally {
        			results.close();
        		}
        	} finally {
        		con.close();
        	}
        } catch (RepositoryException e) {
                throw new MarketplaceException(e.getMessage());
        } catch (IllegalStateException e) {
                throw new MarketplaceException(e.getMessage());
        } catch (MalformedQueryException m) {
                throw new MarketplaceException(m.getMessage());
        } catch (QueryEvaluationException q) {
                throw new MarketplaceException(q.getMessage());
        }

        return list;
    }

    protected StringBuilder formToString(Form form) {
        StringBuilder predicate = new StringBuilder();

        for (String key : form.getNames()) {
            predicate.append(" FILTER (?");
            predicate.append(key);
            predicate.append(" = \"");
            predicate.append(form.getFirstValue(key));
            predicate.append("\" ). ");
        }

        return predicate;

    }

    protected int classifyArg(String arg) {
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

    protected String stripSignature(String signedString) {
        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document datumDoc = null;
        String rdfEntry = "";
        try {
            datumDoc = db.parse(new ByteArrayInputStream(signedString
                    .getBytes("UTF-8")));

            // Create a deep copy of the document and strip signature elements.
            Document copy = (Document) datumDoc.cloneNode(true);
            MetadataUtils.stripSignatureElements(copy);
            rdfEntry = XMLUtils.documentToString(copy);
        } catch (SAXException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Unable to parse metadata: " + e.getMessage());
        } catch (IOException e) {
            throw new ResourceException(e);
        }

        return rdfEntry;
    }

    protected static String readFileAsString(String filePath)
            throws IOException {

        File file = new File(filePath);
        int bytes = (int) file.length();

        byte[] buffer = new byte[bytes];

        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(file));
            int remaining = bytes;
            int offset = 0;
            while (remaining > 0) {
                int readBytes = f.read(buffer, offset, remaining);
                offset += readBytes;
                remaining -= readBytes;
            }
        } finally {
            closeReliably(f);
        }
        return new String(buffer);
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
    
    protected static String getCurrentDate(){
    	return DATE_FORMAT.format(new Date());
    }
        
}
