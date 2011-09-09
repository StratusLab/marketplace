package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.w3c.dom.Document;

import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.ServerResource;

import org.restlet.data.MediaType;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class MDataResourceTest extends ResourceTestBase {
	
	@Test
	public void testInvalidPostNoSignature() throws Exception {
		Request request = createPostRequest("valid.xml");
		Response response = executeRequest(request);
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST)); 
		assertThat(response.getStatus().getDescription(), is("invalid metadata: no signature"));
	}
	
	@Test
	public void testValidPostSignature() throws Exception {
		Request request = createPostRequest("valid-indate-signature.xml");
		Response response = executeRequest(request);
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED)); 
	}
	
	@Test
	public void testGetMetadataEntry() throws Exception {
		Map<String, Object> attributes = createAttributes("identifier",
				"BEE8-MMAw-Lk_IgsEExAy3d9R8h");
		attributes.put("email", "jane.tester@example.org");
		attributes.put("date", "2011-09-09T14:12:59Z");
		Request getRequest = createGetRequest(attributes);
		Response response = executeRequest(getRequest);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK)); 
	}
	
	@Test
	public void testGetMetadataEntryByIdentifier() throws Exception {
		Map<String, Object> attributes = createAttributes("identifier",
				"BEE8-MMAw-Lk_IgsEExAy3d9R8h");
		Request getRequest = createGetRequest(attributes);
		Response response = executeRequest(getRequest);
		
		//Document metadata = extractXmlDocument(response.getEntity().getStream());
		
		assertThat(response.getStatus(), is(Status.SUCCESS_OK)); 
	}
			
	private Request createPostRequest(String filename)
	throws Exception{
		Representation rdf = new InputRepresentation(
				this.getClass().getResourceAsStream(filename),
                MediaType.APPLICATION_RDF_XML);
		Request request = createPostRequest(createAttributes("test","test"), rdf);
		return request;
	}
	
	private Request createGetRequest(String key, String value)
	throws Exception {
		Map<String, Object> attributes = createAttributes(key, value);
		Request request = createGetRequest(attributes);
		return request;
	}
			
	private Response executeRequest(Request request) {
		return executeRequest(request, new MDataResource());
	}
	
}