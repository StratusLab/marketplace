package eu.stratuslab.marketplace.server.resources;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;

import org.restlet.data.Form;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.restlet.resource.ClientResource;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.resource.ResourceException;

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
    public Representation acceptMetadatum(Representation entity) throws ResourceException {
        Representation result = null;
        
        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document datumDoc = null;
        String rdfEntry = "";
        try {
			datumDoc = db.parse(entity.getStream());
			
			// Create a deep copy of the document and strip signature elements.
            Document copy = (Document) datumDoc.cloneNode(true);
            MetadataUtils.stripSignatureElements(copy);
            rdfEntry = XMLUtils.documentToString(copy);
		} catch (SAXException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to parse metadata: " 
					+ e.getMessage());
		} catch (IOException e){
			throw new ResourceException(e);
		}
        
		try {
			//Make sure metadata is valid before continuing
			ValidateXMLSignature.validate(datumDoc);
			ValidateMetadataConstraints.validate(datumDoc);
			ValidateRDFModel.validate(datumDoc);

			//Get key for metadata
			String identifier = XPathUtils.getValue(datumDoc, XPathUtils.IDENTIFIER_ELEMENT);
			String endorser = XPathUtils.getValue(datumDoc, XPathUtils.EMAIL);
			String created = XPathUtils.getValue(datumDoc, XPathUtils.CREATED_DATE);

			//Check that date is within valid range
			try {
				Date endorsementDate = DATE_FORMAT.parse(created);
				Date now = new Date(System.currentTimeMillis() - getTimeRange());

				if(endorsementDate.compareTo(now) < 0){
					logger.log(Level.SEVERE, "Endorsement date outside allowed range.");
					throw new MetadataException("Metadata endorsement creation date outside allowed range.");
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String ref = getRequest().getResourceRef().toString();
			String iri = ref + "/" + identifier + "/" + endorser + "/" + created;

			File rdfFileParent = new File(getDataDir(), identifier + File.separatorChar + endorser);
			File rdfFile = new File(rdfFileParent, created + ".xml");

			if(!rdfFileParent.exists()){
				if(rdfFileParent.mkdirs()){
					MetadataUtils.writeStringToFile(rdfEntry, rdfFile);	
				}
			} else {
				MetadataUtils.writeStringToFile(XMLUtils.documentToString(datumDoc), rdfFile);
			}

			storeMetadatum(iri, rdfEntry);
			// Set the response's status and entity
			setStatus(Status.SUCCESS_CREATED);
			Representation rep = new StringRepresentation("Metadata entry created",
					MediaType.TEXT_PLAIN);
			// Indicates where is located the new resource.
			rep.setLocationRef(getRequest().getResourceRef().getIdentifier() + "/"
					+ identifier + "/" + endorser + "/" + created);
			result = rep;

		} catch (MetadataException m){
			m.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,m.getMessage());
		}
				
        return result;
    }
        
    @Get("html")
    public Representation toHtml() {
    	ArrayList<HashMap<String, String>> results = getMetadata();
    	HashMap<String, Object> data = new HashMap<String, Object>();
    	HashMap<String, Object> root = new HashMap<String, Object>();
    	
    	for ( Iterator<HashMap<String, String>> resultsIter = results.listIterator(); resultsIter.hasNext(); ){
        	HashMap<String, String> resultRow = resultsIter.next();
    	
    	    String identifier = resultRow.get("identifier");
    	    String endorser = resultRow.get("email");
    	    String created = resultRow.get("created");
    	    logger.log(Level.INFO, identifier + "  " + endorser + " " + created);
    	    if(root.containsKey(identifier)){
    	    	HashMap<String, String> endorserMap = (HashMap<String, String>)root.get(identifier);
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
    	Representation listFtl = new ClientResource(LocalReference.createClapReference("/metadata.ftl")).get();
    	// Wraps the bean with a FreeMarker representation
    	Representation representation = new TemplateRepresentation(listFtl, 
    			data, MediaType.TEXT_HTML);
    	
       return representation;
    }
    
    /**
     * Returns a listing of all registered metadata or a particular entry if specified.
     */
    @Get("xml")
    public Representation toXml() {
    	ArrayList<HashMap<String, String>> results = getMetadata();
    	String[] uris = new String[results.size()];
        int i = 0;
    	for ( Iterator<HashMap<String, String>> resultsIter = results.listIterator(); resultsIter.hasNext(); ){
        	HashMap<String, String> resultRow = resultsIter.next();
        	String iri = resultRow.get("identifier") 
            + "/" + resultRow.get("email") + "/" 
            + resultRow.get("created");
            uris[i] = iri;
            i++;
        }
    	
    	StringBuffer output = new StringBuffer(XML_HEADER);
        
        if(uris.length > 0){
        	for(String uri : uris ){
        		String datum = getMetadatum(getDataDir() + File.separatorChar + uri + ".xml");
        		if(datum.startsWith(XML_HEADER)){
        			datum = datum.substring(XML_HEADER.length());
        		}
        		output.append(datum);
        	}
        }
                    
        // Returns the XML representation of this document.
    	StringRepresentation representation =
                new StringRepresentation(output,
                                         MediaType.APPLICATION_XML);
            
        return representation;
    }

    private ArrayList<HashMap<String, String>> getMetadata() {
    	try {
            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            Map<String, Object> requestAttr =  getRequest().getAttributes();
            boolean dateSearch = false;          
            
            StringBuffer filterPredicate = new StringBuffer();
                        
            for ( Iterator<Map.Entry<String,Object>> argsIter = requestAttr.entrySet().iterator(); argsIter.hasNext(); ){
            	Map.Entry<String,Object> arg = argsIter.next();
            	String key = arg.getKey();
            	if(!key.startsWith("org.restlet")){
            		switch(classifyArg((String)arg.getValue())){
            		case ARG_EMAIL:
            			filterPredicate.append(" FILTER (?email = \"" + arg.getValue() + "\"). ");
            			break;
            		case ARG_DATE:
            			dateSearch = true;
            			filterPredicate.append(" FILTER (?created = \"" + arg.getValue() + "\"). ");
            			break;
            		case ARG_OTHER:
            			filterPredicate.append(" FILTER (?identifier = \"" + arg.getValue() + "\"). ");
            			break;
            		default:
            			break;
            		}
            	}
            }
            
            filterPredicate.append(formToString(queryForm));
                      
            StringBuffer queryString = new StringBuffer("SELECT DISTINCT ?identifier ?email ?created " +
                    " WHERE {" +
                    " ?x <http://purl.org/dc/terms/identifier>  ?identifier; " +
                    " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ." +
                    " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;" +
                    " <http://purl.org/dc/terms/created> ?created ." +
                    " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .");
            
            queryString.append(filterPredicate.toString());
                        
            if(!dateSearch && !queryForm.getNames().contains("created")) {
            	queryString.append(" OPTIONAL { " +
            			" ?lx <http://purl.org/dc/terms/identifier>  ?lidentifier; " +
            			" <http://mp.stratuslab.eu/slreq#endorsement> ?lendorsement ." +
            			" ?lendorsement <http://mp.stratuslab.eu/slreq#endorser> ?lendorser;" +
            			" <http://purl.org/dc/terms/created> ?latestcreated ." +
            			" ?lendorser <http://mp.stratuslab.eu/slreq#email> ?lemail ." +
            			" FILTER (?lidentifier = ?identifier) ." +
            			" FILTER (?lemail = ?email) ." +
            	" FILTER (?latestcreated > ?created) . } FILTER (!bound (?lendorsement))");
            }
            
            queryString.append(" }");
                         
            ArrayList<HashMap<String, String>> results = (ArrayList<HashMap<String, String>>)query(queryString.toString());
                                 
            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
