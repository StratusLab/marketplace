package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.store.rdf.RdfStore;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class EndorsersResourceTest extends ResourceTestBase {
	
	private static String email;
	private static String identifier;
	private static String created;
	
	static String tmpDir;
	
	static RdfStore store;
	
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
		Request request = createGetRequest(null);
		Response response = executeRequest(request);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		
		assertTrue(response.getEntityAsText().indexOf(email) > 0);
	}
		
	private Response executeRequest(Request request) {
		return executeRequest(request, new EndorsersResource());
	}
	
}