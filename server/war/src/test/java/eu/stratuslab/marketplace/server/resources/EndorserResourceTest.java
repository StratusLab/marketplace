package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.ServerResource;

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
		Request request = createGetRequest(createAttributes("email", email));
		Response response = executeRequest(request);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		assertTrue(response.getEntityAsText().indexOf(identifier) > 0);
	}
		
	private Response executeRequest(Request request) {
		return executeRequest(request, new EndorserResource());
	}
	
}