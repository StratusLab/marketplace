package eu.stratuslab.marketplace.server.util;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;
import org.restlet.Application;
import org.restlet.data.MediaType;
import org.restlet.representation.InputRepresentation;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.utils.XPathUtils;
import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.server.resources.MDataResource;

public class ResourceTestBase {

	static Application application;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication("memory");
		component.getDefaultHost().attach("/", application);
		component.getClients().add(Protocol.CLAP);
		application.setContext(component.getDefaultHost().getContext());
		application.createInboundRoot();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		application.stop();
	}
	
	
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
		}
				
		return value;
	}
	
}