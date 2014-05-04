package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.store.rdf.RdfStore;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class EndorserResourceTest extends ResourceTestBase {
	
	private static String email;
	private static String identifier;
	private static String created;
	
	static String tmpDir;
	private static RdfStore store;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tmpDir = getTempDir("marketplace");
		
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication(tmpDir, "solr", "file");
		component.getDefaultHost().attach("/", application);
		component.getClients().add(Protocol.CLAP);
		application.setContext(component.getDefaultHost().getContext());
		application.createInboundRoot();
		
		store = application.getMetadataRdfStore();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		store.remove(identifier + "/" + email + "/" + created);	
		application.stop();
		
		FileUtils.deleteDirectory(new File(tmpDir));
	}
	
	@Before 
	public void setUp() throws Exception {
		postMetadataFile("valid-indate-signature.xml");
		
		Document doc = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		
		identifier = getValueFromDoc(doc, "identifier");
		email = getValueFromDoc(doc, "email");
		created = getValueFromDoc(doc, "created");
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