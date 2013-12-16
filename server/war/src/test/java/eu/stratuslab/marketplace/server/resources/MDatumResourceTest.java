package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.commons.io.FileUtils;
import org.junit.After;
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
	
	private static String tmpDir;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		tmpDir = getTempDir("marketplace");
	}
	
	@Before
	public void setUp() throws Exception {
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication(tmpDir, "memory", "file");
		component.getDefaultHost().attach("/", application);
		component.getClients().add(Protocol.CLAP);
		application.setContext(component.getDefaultHost().getContext());
		application.createInboundRoot();
	}
	
	@After
	public void tearDown() throws Exception {
		application.stop();
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		FileUtils.deleteDirectory(new File(tmpDir));
	}
	
	@Test
	public void testGetHtml() throws Exception {
		postMetadataFile("valid-indate-signature.xml");

		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));

		String iri = "/metadata/" + getValueFromDoc(metadata, "identifier")
				+ "/" + getValueFromDoc(metadata, "email")
				+ "/" + getValueFromDoc(metadata, "created");

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
		postMetadataFile("valid-indate-signature.xml");

		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));

		String iri = "/metadata/" + getValueFromDoc(metadata, "identifier")
				+ "/" + getValueFromDoc(metadata, "email")
				+ "/" + getValueFromDoc(metadata, "created");

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
		postMetadataFile("valid-indate-signature.xml");

		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml"));

		String iri = "/metadata/" + getValueFromDoc(metadata, "identifier")
				+ "/" + getValueFromDoc(metadata, "email")
				+ "/" + getValueFromDoc(metadata, "created");

		Request request = createRequest(null, Method.GET,
				null, iri);
		ClientInfo info = new ClientInfo(MediaType.APPLICATION_JSON);
		info.getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.APPLICATION_JSON));
		request.setClientInfo(info);
		Response response = executeRequest(request);
		assertThat(response.getEntity().getMediaType().getName(), 
				is("application/json"));
	}
	
        @Test
   	public void testGetTag() throws Exception {
   		postMetadataFile("valid-alternative.xml");
   		
   		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-alternative.xml"));
		
   		String identifier = getValueFromDoc(metadata, "identifier");
   		String email = getValueFromDoc(metadata, "email");
   		String alternative = getValueFromDoc(metadata, "alternative");
   		
   		Map<String, Object> attributes = createAttributes("email", email);
   		attributes.put("tag", alternative);
   		
		Request getRequest = createGetRequest(attributes);
		Response response = executeRequest(getRequest);
		
		Document responseDoc = extractXmlDocument(response.getEntity()
				.getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");
   		String responseAlt = getValueFromDoc(responseDoc, "alternative");
		
		assertThat(identifier, is(responseId));
		assertThat(alternative, is(responseAlt));
	}
	
        @Test
  	public void testGetTagReturnsLatest() throws Exception {
  		postMetadataFile("valid-alternative.xml");
  		postMetadataFile("valid-alternative-newid.xml");
  		
  		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream("valid-alternative-newid.xml"));
  				
  		String identifier = getValueFromDoc(metadata, "identifier");
  		String email = getValueFromDoc(metadata, "email");
  		String created = getValueFromDoc(metadata, "created");
  		String alternative = getValueFromDoc(metadata, "alternative");
  		String description = getValueFromDoc(metadata, "description");
  		
  		Map<String, Object> attributes = createAttributes("email", email);
  		attributes.put("tag", alternative);
  		
		Request getRequest = createGetRequest(attributes);
		Response response = executeRequest(getRequest);
		
		Document responseDoc = extractXmlDocument(response.getEntity()
				.getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");
		String responseEmail = getValueFromDoc(metadata, "email");
  		String responseAlt = getValueFromDoc(responseDoc, "alternative");
  		String responseCreated = getValueFromDoc(responseDoc, "created");
  		String responseDesc = getValueFromDoc(responseDoc, "description");
		
		assertThat(identifier, is(responseId));
		assertThat(email, is(responseEmail));
		assertThat(alternative, is(responseAlt));
		assertThat(description, is(responseDesc));
		assertThat(created, is(responseCreated));
	}
   
	private Response executeRequest(Request request) {
		return executeRequest(request, new MDatumResource());
	}
			
}
