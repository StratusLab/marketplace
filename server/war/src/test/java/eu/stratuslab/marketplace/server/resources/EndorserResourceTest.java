package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Status;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class EndorserResourceTest extends ResourceTestBase {
	
	String email;
	String identifier;
	
	@Before 
	public void setUp() throws Exception {
		postMetadataFile("valid-indate-signature.xml");
		
		email = getValueFromDoc(extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml")),
				"email");
		identifier = getValueFromDoc(extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml")),
				"identifier");
	}
	
	@Test
	public void testGetEndorsers() throws Exception {
		Map<String, Object> attributes = createAttributes("email", email);
		Response response = executeQuery(attributes, "?range=1460");
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		assertTrue(response.getEntityAsText().indexOf(identifier) > 0);
	}
		
	private Response executeQuery(Map<String, Object> attributes, String query) throws Exception {
		Request request = createRequest(attributes, Method.GET,
				 null, query);
		return executeRequest(request);
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new EndorserResource());
	}
	
}