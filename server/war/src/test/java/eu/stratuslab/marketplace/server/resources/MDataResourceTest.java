package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.data.Method;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDataResourceTest extends ResourceTestBase {
	
	static final Logger logger = 
			LoggerFactory.getLogger(MDataResourceTest.class);
	
	private String tmpPath;
	
	@Before
	public void setUpBeforeClass() throws Exception {
		tmpPath = getTempDir("marketplace");
		
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication(tmpPath, "memory", "file");
		component.getDefaultHost().attach("/", application);
		component.getClients().add(Protocol.CLAP);
		application.setContext(component.getDefaultHost().getContext());
		application.createInboundRoot();
	}
	
	@After
	public void tearDownAfterClass() throws Exception {
		application.stop();
		
		FileUtils.deleteDirectory(new File(tmpPath));
	}
	
	@Test
	public void testInvalidPostNoSignature() throws Exception {
		Response response = postMetadataFile("valid.xml");
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
		assertThat(response.getStatus().getDescription(),
				is("invalid metadata: no signature"));
	}
	
	@Test
	public void testValidPostSignature() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
	}

	@Test
	public void testGetMetadataEntry() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		attributes.put("email", getValueFromDoc(metadata, "email"));
		attributes.put("created", getValueFromDoc(metadata, "created"));
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}
    
	@Test
	public void testGetMetadataEntryByIdentifier() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}

	@Test
	public void testIdentifierQueryString() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		Request getRequest = createRequest(null, Method.GET, null,
				"/test?identifier=" + identifier);
		response = executeRequest(getRequest);
		Document responseDoc = extractXmlDocument(response.getEntity()
				.getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");

		assertThat(identifier, is(responseId));
	}

	@Test
	public void testIdentifierEmailQueryString() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		Request getRequest = createRequest(null, Method.GET, null,
				"/test?identifier=" + identifier
						+ "&email=jane.tester@example.org");
		response = executeRequest(getRequest);
		Document responseDoc = extractXmlDocument(response.getEntity()
				.getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");

		assertThat(identifier, is(responseId));
	}

	@Test
	public void testIdentifierUnknownEmailQueryString() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		Request getRequest = createRequest(null, Method.GET, null,
				"/test?identifier=" + identifier
						+ "&email=j.tester@example.org");
		response = executeRequest(getRequest);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}

	@Test
	public void testCreatedQuery() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-signature.xml"));
		String identifier = getValueFromDoc(metadata, "identifier");
		String created = getValueFromDoc(metadata, "created");
		Request getRequest = createRequest(null, Method.GET, null,
				"/test?created=" + created);
		response = executeRequest(getRequest);
		Document responseDoc = extractXmlDocument(response.getEntity()
				.getStream());
		String responseId = getValueFromDoc(responseDoc, "identifier");

		assertThat(identifier, is(responseId));
	}

	@Test
	public void testIdentifierReturnsLatest() throws Exception {
		Response response = postMetadataFile("valid-indate-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		response = postMetadataFile("valid-indate-newer-signature.xml");

		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-newer-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);

		String created = getValueFromDoc(extractXmlDocument(response
				.getEntity().getStream()), "created");

		assertThat(created, is("2014-03-08T14:23:18Z"));
	}

	@Test
	public void testUploadingOlderMetadataFails() throws Exception {
		Response response = postMetadataFile("valid-indate-newer-signature.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		response = postMetadataFile("valid-indate-signature.xml");

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
		assertThat(response.getStatus().getDescription(),
				is("invalid metadata: older than latest entry"));
	}

	@Test
	public void testDeprecatedNotInList() throws Exception {
		Response response = postMetadataFile("valid-indate-deprecated-signature.xml");

		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-deprecated-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}

	@Test
	public void testDeprecatedFlag() throws Exception {
		Response response = postMetadataFile("valid-indate-deprecated-signature.xml");

		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-indate-deprecated-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		attributes.put("email", getValueFromDoc(metadata, "email"));
		attributes.put("created", getValueFromDoc(metadata, "created"));

		Request getRequest = createGetRequest(attributes);
		getRequest.getResourceRef().addQueryParameter("status", "deprecated");
		response = executeRequest(getRequest);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}

	@Test
	public void testExpiredNotInList() throws Exception {
		Response response = postMetadataFile("valid-expired-signature.xml");

		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-expired-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));

		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);

		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
	}

	@Test
	public void testExpiredStillAvailable() throws Exception {
		Response response = postMetadataFile("valid-expired-signature.xml");

		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-expired-signature.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		attributes.put("email", getValueFromDoc(metadata, "email"));
		attributes.put("created", getValueFromDoc(metadata, "created"));
        
		Request getRequest = createGetRequest(attributes);
		getRequest.getResourceRef().addQueryParameter("status", "expired");
		
		response = executeRequest(getRequest);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}
    
	@Test
	public void testCreatesValidJson() throws Exception {
		postMetadataFile("invalid-special-characters-signature.xml");
        
		Request request = createRequest(null, Method.POST, null,
				"/request/test");
		ClientInfo info = new ClientInfo(MediaType.APPLICATION_JSON);
		info.getAcceptedMediaTypes().add(
				new Preference<MediaType>(MediaType.APPLICATION_JSON));
		request.setClientInfo(info);
		request.setEntity(
				"sEcho=1&iColumns=6&sColumns=&iDisplayStart=0" +
				"&iDisplayLength=10&mDataProp_0=0&mDataProp_1=1&mDataProp_2=2" +
				"&mDataProp_3=3&mDataProp_4=4&mDataProp_5=5&sSearch=&bRegex=false" +
				"&sSearch_0=&bRegex_0=false&bSearchable_0=true&sSearch_1=&bRegex_1=false" +
				"&bSearchable_1=false&sSearch_2=&bRegex_2=false&bSearchable_2=false" +
				"&sSearch_3=&bRegex_3=false&bSearchable_3=false&sSearch_4=&bRegex_4=false" +
				"&bSearchable_4=false&sSearch_5=&bRegex_5=false&bSearchable_5=false" +
				"&iSortingCols=1&iSortCol_0=5&sSortDir_0=desc&bSortable_0=false" +
				"&bSortable_1=true&bSortable_2=true&bSortable_3=true&bSortable_4=true&bSortable_5=true",
				MediaType.APPLICATION_WWW_FORM);
		Response response = executeRequest(request);

		boolean valid = false;
		JSONParser parser = new JSONParser();
        
		try{
			parser.parse(response.getEntity().getText());
		    valid = true;
		} catch(ParseException pe){
		   valid = false;
		}
				
		assertTrue(valid);
	}
	
	@Test
	public void testDuplicateMetadataFields() throws Exception {
		Response response = postMetadataFile("valid-two-locations.xml");
		assertThat(response.getStatus(), is(Status.SUCCESS_CREATED));
		
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream("valid-two-locations.xml"));
		Map<String, Object> attributes = createAttributes("identifier",
				getValueFromDoc(metadata, "identifier"));
		Request getRequest = createGetRequest(attributes);
		response = executeRequest(getRequest);
		
		Document responseDoc = extractXmlDocument(response.getEntity()
				.getStream());
		NodeList list = responseDoc.getElementsByTagName("rdf:RDF");
		assertThat(list.getLength(), is(1));
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new MDataResource());
	}
	
}
