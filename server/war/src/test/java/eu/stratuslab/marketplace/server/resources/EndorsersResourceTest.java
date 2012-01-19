package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class EndorsersResourceTest extends ResourceTestBase {
	
	String email;
	
	@Before 
	public void setUp() throws Exception {
		postMetadataFile("valid-indate-signature.xml");
		
		email = getValueFromDoc(extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml")),
				"email");
	}
	
	@Test
	public void testGetEndorsers() throws Exception {
		Request request = createGetRequest(null);
		Response response = executeRequest(request);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		assertTrue(response.getEntityAsText().indexOf(email) > 0);
	}
		
	private Response executeRequest(Request request) {
		return executeRequest(request, new EndorsersResource());
	}
	
}