package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class TagsResourceTest extends ResourceTestBase {
	
	String email;
	String tag;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication("memory", "file");
		component.getDefaultHost().attach("/", application);
		component.getClients().add(Protocol.CLAP);
		application.setContext(component.getDefaultHost().getContext());
		application.createInboundRoot();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		application.stop();
	}
	
	@Before 
	public void setUp() throws Exception {
		String metadata = "valid-alternative.xml";
		
		postMetadataFile(metadata);
		
		email = getValueFromDoc(extractXmlDocument(
				this.getClass().getResourceAsStream(metadata)),
				"email");
		tag = getValueFromDoc(extractXmlDocument(
				this.getClass().getResourceAsStream(metadata)),
				"alternative");
	}
	
	@Test
	public void testGetEndorserTags() throws Exception {
		Map<String, Object> attributes = createAttributes("email", email);
		Response response = executeQuery(attributes);
		
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		assertTrue(response.getEntityAsText().indexOf(tag) > 0);
	}
		
	private Response executeQuery(Map<String, Object> attributes) throws Exception {
		Request request = createRequest(attributes, Method.GET,
				 null, null);
		return executeRequest(request);
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new TagsResource());
	}
	
}