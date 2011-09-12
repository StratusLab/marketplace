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
import eu.stratuslab.marketplace.server.utils.XPathUtils;

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
		
		assertThat(response.getStatus(), is(Status.SUCCESS_OK)); 
	}
	
	@Test
	public void testIdentifierReturnsLatest() throws Exception {
		Request request = createPostRequest("valid-indate-newer-signature.xml");
		Response response = executeRequest(request);
		
		Map<String, Object> attributes = createAttributes("identifier",
				"BEE8-MMAw-Lk_IgsEExAy3d9R8h");
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
		Document doc = extractXmlDocument(response.getEntity().getStream());
		String created = XPathUtils.getValue(doc, XPathUtils.CREATED_DATE);
		
		assertThat(created, is("2011-09-12T09:58:55Z")); 
	}
		
	@Test
	public void testDeprecatedNotInList() throws Exception {
		Request request = createPostRequest("valid-indate-deprecated-signature.xml");
		Response response = executeRequest(request);
		
		Map<String, Object> attributes = createAttributes("identifier",
				"BEE8-MMAw-Lk_IgsEExAy3d9R8h");
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}
	
	@Test
	public void testDeprecatedFlag() throws Exception {
		Request request = createPostRequest("valid-indate-deprecated-signature.xml");
		Response response = executeRequest(request);
		
		Map<String, Object> attributes = createAttributes("identifier",
				"BEE8-MMAw-Lk_IgsEExAy3d9R8h");
		attributes.put("email", "jane.tester@example.org");
		attributes.put("date", "2011-09-12T11:07:57Z");
				
		Request getRequest = createGetRequest(attributes);
		getRequest.getResourceRef().addQueryParameter("deprecated", "");
		response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}
	
	@Test
	public void testExpiredNotInList() throws Exception {
		Request request = createPostRequest("valid-expired-signature.xml");
		Response response = executeRequest(request);
		
		Map<String, Object> attributes = createAttributes("identifier",
		"Ce3uQbAaxJMbTBaCXWO2FZPRFTT");
		
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}
	
	@Test
	public void testExpiredStillAvailable() throws Exception {
		Request request = createPostRequest("valid-expired-signature.xml");
		Response response = executeRequest(request);
		
		Map<String, Object> attributes = createAttributes("identifier",
		"Ce3uQbAaxJMbTBaCXWO2FZPRFTT");
		attributes.put("email", "jane.tester@example.org");
		attributes.put("date", "2011-09-12T11:23:44Z");
		
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
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
