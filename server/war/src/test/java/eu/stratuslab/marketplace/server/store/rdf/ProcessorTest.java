package eu.stratuslab.marketplace.server.store.rdf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.resources.MDataResource;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class ProcessorTest extends ResourceTestBase {

	private String classPrefix = "/eu/stratuslab/marketplace/server/resources/";
	private static String tmpPath;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tmpPath = getTempDir("marketplace");
		
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication(tmpPath, "memory", "file");
		component.getDefaultHost().attach("/", application);
		component.getClients().add(Protocol.CLAP);
		application.setContext(component.getDefaultHost().getContext());
		application.createInboundRoot();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		application.stop();
		
		FileUtils.deleteDirectory(new File(tmpPath));
	}
	
	@Test
	public void testProcessEntry() throws Exception {
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream(classPrefix + "valid-indate-signature.xml"));
		
		Processor processor = new Processor(application);
		processor.processEntry(XMLUtils.documentToString(metadata));
		
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		attributes.put("email", getValueFromDoc(metadata, "email"));
		attributes.put("created", getValueFromDoc(metadata, "created"));
		Request getRequest = createGetRequest(attributes);
		Response response = executeRequest(getRequest);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new MDataResource());
	}

}
