package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.w3c.dom.Document;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.ClientInfo;
import org.restlet.data.Preference;
import org.restlet.data.Method;
import org.restlet.data.Protocol;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class MDatumResourceTest extends ResourceTestBase {
	
	private String iri;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication("memory");
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
		postMetadataFile("valid-indate-signature.xml");
		
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));
		
		this.iri = "/metadata/" + getValueFromDoc(metadata, "identifier")
			+ "/" + getValueFromDoc(metadata, "email")
			+ "/" + getValueFromDoc(metadata, "created");
	}
	
	@Test
	public void testGetHtml() throws Exception {
		Request request = createRequest(null, Method.GET,
				 null, iri);
		ClientInfo info = new ClientInfo(MediaType.TEXT_HTML);
		info.getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.TEXT_HTML));
		request.setClientInfo(info);
		Response response = executeRequest(request);
		assertThat(response.getEntity().getMediaType().getName()
				, is("text/html"));
	}
	
	@Test
	public void testGetXml() throws Exception {
		Request request = createRequest(null, Method.GET,
				 null, iri);
		ClientInfo info = new ClientInfo(MediaType.TEXT_XML);
		info.getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.TEXT_XML));
		request.setClientInfo(info);
		Response response = executeRequest(request);
		assertThat(response.getEntity().getMediaType().getName(), 
				is("application/rdf+xml"));
	}
	
	@Test
	public void testGetJson() throws Exception {
		Request request = createRequest(null, Method.GET,
				 null, iri);
		ClientInfo info = new ClientInfo(MediaType.APPLICATION_JSON);
		info.getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.APPLICATION_JSON));
		request.setClientInfo(info);
		Response response = executeRequest(request);
		assertThat(response.getEntity().getMediaType().getName(), 
				is("application/json"));
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new MDatumResource());
	}
			
}