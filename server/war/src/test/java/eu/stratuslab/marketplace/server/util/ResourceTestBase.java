package eu.stratuslab.marketplace.server.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.io.FileUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.utils.XPathUtils;

public class ResourceTestBase {

	protected static MarketPlaceApplication application;
	
	public Request createRequest(Map<String, Object> attributes, Method method)
			throws Exception {
		return createRequest(attributes, method, null);
	}

	public Request createRequest(Map<String, Object> attributes, Method method,
			Representation entity) throws Exception {
		return createRequest(attributes, method, entity, "/test/request");
	}

	public Request createRequest(Map<String, Object> attributes, Method method,
			Representation entity, String targetUrl)
			throws Exception {
		Request request = new Request(method, "http://something.org"
				+ targetUrl);
		request.setRootRef(new Reference("http://something.org"));
		request.setEntity(entity);
		request.setAttributes(attributes);
		
		return request;
	}
	
	protected Request createGetRequest(Map<String, Object> attributes)
			throws Exception {
		Method method = Method.GET;
		return createRequest(attributes, method);
	}

	protected Request createPutRequest(Map<String, Object> attributes,
			Representation entity) throws Exception {
		return createRequest(attributes, Method.PUT, entity);
	}

	protected Request createPutRequest(Map<String, Object> attributes,
			Representation entity, String targetUrl)
			throws Exception {
		return createRequest(attributes, Method.PUT, entity, targetUrl);
	}

	protected Request createDeleteRequest(Map<String, Object> attributes)
			throws Exception {
		Method method = Method.DELETE;
		return createRequest(attributes, method);
	}

	protected Request createPostRequest(Map<String, Object> attributes,
			Representation entity) throws Exception {
		Method method = Method.POST;
		return createRequest(attributes, method, entity);
	}
	
	private Request createPostRequest(String filename)
	throws Exception {
		Representation rdf = new InputRepresentation(
				this.getClass().getResourceAsStream(filename),
                MediaType.APPLICATION_RDF_XML);
		Request request = createPostRequest(createAttributes("test","test"), rdf);
		return request;
	}

	private Response executeMetadataPostRequest(Request request) {
		return executeRequest(request, new MDataResource());
	}
	
	protected Response executeRequest(Request request, ServerResource resource) {

		Response response = new Response(request);

		resource.setApplication(application);
				
		resource.init(null, request, response);
					
		if (response.getStatus().isSuccess()) {
			resource.handle();
		}

		return resource.getResponse();
	}

	protected Map<String, Object> createAttributes(String name, String value) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(name, value);
		return attributes;
	}
	
	protected Response postMetadataFile(String filename) throws Exception {
		Request request = createPostRequest(filename);
		Response response = executeMetadataPostRequest(request);
		return response;
	}
	
	protected Document extractXmlDocument(InputStream stream) throws Exception {

        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document datumDoc = null;
        datumDoc = db.parse(stream);
        return datumDoc;
    }
	
	protected String getValueFromDoc(Document doc, String key) throws Exception {
		String value = "";		
		if(key.equals("created")){
			value = XPathUtils.getValue(doc, XPathUtils.CREATED_DATE);
		} else if (key.equals("identifier")){
			value = XPathUtils.getValue(doc, XPathUtils.IDENTIFIER_ELEMENT);
		} else if (key.equals("email")){
			value = XPathUtils.getValue(doc, XPathUtils.EMAIL);
		} else if (key.equals("alternative")){
			value = XPathUtils.getValue(doc, XPathUtils.ALTERNATIVE);
		} else if (key.equals("description")){
			value = XPathUtils.getValue(doc, XPathUtils.DESCRIPTION);
		}
				
		return value;
	}
	
	protected static void closeReliably(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException consumed) {
			}
		}
	}
	
	public static String getTempDir(String prefix)
		    throws IOException
		  {
		    String tmpDirStr = FileUtils.getTempDirectoryPath();
		     
		    File resultDir = null;
		    int suffix = (int)System.currentTimeMillis();
		    int failureCount = 0;
		    do {
		      resultDir = new File(tmpDirStr, prefix + suffix % 10000);
		      suffix++;
		      failureCount++;
		    }
		    while (resultDir.exists() && failureCount < 50);
		    
		    if (resultDir.exists()) {
		      throw new IOException(failureCount + 
		        " attempts to generate a non-existent directory name failed, giving up");
		    }
		    
		    return resultDir.getCanonicalPath();
		  }
	
}
