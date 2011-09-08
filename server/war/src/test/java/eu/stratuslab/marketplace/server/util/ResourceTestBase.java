package eu.stratuslab.marketplace.server.util;

import java.util.HashMap;
import java.util.Map;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

public class ResourceTestBase {

	/*
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create a new Component.
        component = new Component();

        // Add a new HTTP server listening on port 8111.
        component.getServers().add(Protocol.HTTP, 8111);

        // Attach the sample application.
        component.getDefaultHost().attach("/",
                new MarketPlaceApplication("memory"));

        // Start the component.
        component.start();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		component.stop();
	}
	*/
	
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

	protected Response executeRequest(Request request, ServerResource resource) {

		Response response = new Response(request);

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
}
