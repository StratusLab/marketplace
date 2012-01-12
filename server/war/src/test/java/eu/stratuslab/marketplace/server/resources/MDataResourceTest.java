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
import org.restlet.data.Method;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class MDataResourceTest extends ResourceTestBase {
    
	@Test
	public void testInvalidPostNoSignature() throws Exception {
		Response response = postMetadataFile("valid.xml");
        assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST)); 
		assertThat(response.getStatus().getDescription(), is("invalid metadata: no signature"));
	}
		
	@Test
	public void testValidPostSignature() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED)); 
	}
	
	@Test
	public void testGetMetadataEntry() throws Exception {
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		attributes.put("email", getValueFromDoc(metadata, "email"));
		attributes.put("created", getValueFromDoc(metadata, "created"));
		Request getRequest = createGetRequest(attributes);
		Response response = executeRequest(getRequest);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK)); 
	}
	
	@Test
	public void testGetMetadataEntryByIdentifier() throws Exception {
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		Request getRequest = createGetRequest(attributes);
		Response response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.SUCCESS_OK)); 
	}
	
	@Test
	public void testIdentifierQueryString() throws Exception {
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		Request getRequest = createRequest(null, Method.GET,
			null, "/test?identifier=" + identifier);
		Response response = executeRequest(getRequest);
		Document responseDoc = extractXmlDocument(response.getEntity().getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");
		
		assertThat(identifier, is(responseId));
	}
	
	@Test
	public void testIdentifierEmailQueryString() throws Exception {
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		Request getRequest = createRequest(null, Method.GET,
			null, "/test?identifier=" + identifier + "&email=jane.tester@example.org");
		Response response = executeRequest(getRequest);
		Document responseDoc = extractXmlDocument(response.getEntity().getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");
		
		assertThat(identifier, is(responseId));
	}
	
	@Test
	public void testIdentifierUnknownEmailQueryString() throws Exception {
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		Request getRequest = createRequest(null, Method.GET,
			null, "/test?identifier=" + identifier + "&email=j.tester@example.org");
		Response response = executeRequest(getRequest);
						
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}
	
	@Test
	public void testCreatedQuery() throws Exception {
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		String created = getValueFromDoc(metadata, "created");
		Request getRequest = createRequest(null, Method.GET,
			null, "/test?created=" + created);
		Response response = executeRequest(getRequest);
		Document responseDoc = extractXmlDocument(response.getEntity().getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");
		
		assertThat(identifier, is(responseId));
	}
		
	@Test
	public void testIdentifierReturnsLatest() throws Exception {
		Response response = postMetadataFile("valid-indate-newer-signature.xml");
		
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-newer-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
				
		String created = getValueFromDoc(
				extractXmlDocument(response.getEntity().getStream())
				, "created");
		
		assertThat(created, is("2011-09-12T09:58:55Z")); 
	}
		
	@Test
	public void testDeprecatedNotInList() throws Exception {
		Response response = postMetadataFile("valid-indate-deprecated-signature.xml");
				
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-deprecated-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}
	
	@Test
	public void testDeprecatedFlag() throws Exception {
		Response response = postMetadataFile("valid-indate-deprecated-signature.xml");
		
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-deprecated-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		attributes.put("email", getValueFromDoc(metadata, "email"));
		attributes.put("created", getValueFromDoc(metadata, "created"));
				
		Request getRequest = createGetRequest(attributes);
		getRequest.getResourceRef().addQueryParameter("deprecated", "");
		response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}
	
	@Test
	public void testExpiredNotInList() throws Exception {
		Response response = postMetadataFile("valid-expired-signature.xml");
		
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-expired-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}
	
	@Test
	public void testExpiredStillAvailable() throws Exception {
		Response response = postMetadataFile("valid-expired-signature.xml");
		
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-expired-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		attributes.put("email", getValueFromDoc(metadata, "email"));
		attributes.put("created", getValueFromDoc(metadata, "created"));
		
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
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
