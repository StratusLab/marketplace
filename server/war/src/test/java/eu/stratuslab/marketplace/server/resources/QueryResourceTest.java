package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.data.Method;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class QueryResourceTest extends ResourceTestBase {
	
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
		postMetadataFile("valid-indate-signature.xml");
	}
	
	@Test
	public void testEmptyQuery() throws Exception {
		String query = "";
		
		Response response = executeQuery(query);
		assertThat(response.getStatus(), is(Status.SUCCESS_NO_CONTENT));
	}
	
	@Test
	public void testValidSparQl() throws Exception {
		String validQuery = "select ?identifier " +
				"where { ?x <http://purl.org/dc/terms/identifier>  ?identifier }";
		
		String identifier = getValueFromDoc(extractXmlDocument(
				this.getClass().getResourceAsStream("valid-indate-signature.xml")),
				"identifier");
		
		Response response = executeQuery(validQuery);
		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
		assertTrue(response.getEntityAsText().indexOf(identifier) > 0);
	}
	
	@Test
	public void testInvalidSparlQl() throws Exception {
		String invalidQuery = "seect ?identifier " +
		"where { ?x <http://purl.org/dc/terms/identifier>  ?identifier }";
		Response response = executeQuery(invalidQuery);
		
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
	}
	
	@Test
	public void testFromNotAllowed() throws Exception {
		String invalidQuery = "select ?identifier " +
		"FROM <http://marketplace.stratuslab.eu> " +
		"WHERE { ?x <http://purl.org/dc/terms/identifier>  ?identifier }";
		
		Response response = executeQuery(invalidQuery);
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
		assertThat(response.getStatus().getDescription(), is("query not allowed."));
	}
	
	@Test
	public void testFromNamedNotAllowed() throws Exception {
		String invalidQuery = "select ?identifier " +
		"FROM NAMED <http://marketplace.stratuslab.eu> " +
		"WHERE { ?x <http://purl.org/dc/terms/identifier>  ?identifier }";
		
		Response response = executeQuery(invalidQuery);
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
		assertThat(response.getStatus().getDescription(), is("query not allowed."));
	}
	
	@Test
	public void testGraphNotAllowed() throws Exception {
		String invalidQuery = "select ?identifier " +
		"WHERE { GRAPH <http://marketplace.stratuslab.eu> " +
		"{ ?x <http://purl.org/dc/terms/identifier>  ?identifier } . }";
		
		Response response = executeQuery(invalidQuery);
		assertThat(response.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
		assertThat(response.getStatus().getDescription(), is("query not allowed."));
	}
	
	@Test
	public void testGetHtml() throws Exception {
		Request request = createRequest(null, Method.GET);
		ClientInfo info = new ClientInfo(MediaType.TEXT_HTML);
		info.getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.TEXT_HTML));
		request.setClientInfo(info);
		Response response = executeRequest(request);
		assertThat(response.getEntity().getMediaType().getName()
				, is("text/html"));
	}
	
	@Test
	public void testGetXml() throws Exception {
		Request request = createRequest(null, Method.GET);
		ClientInfo info = new ClientInfo(MediaType.TEXT_XML);
		info.getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.TEXT_XML));
		request.setClientInfo(info);
		Response response = executeRequest(request);
		assertThat(response.getEntity().getMediaType().getName(), 
				is("application/sparql-results+xml"));
	}
	
	@Test
	public void testGetJson() throws Exception {
		Request request = createRequest(null, Method.GET);
		ClientInfo info = new ClientInfo(MediaType.APPLICATION_JSON);
		info.getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.APPLICATION_JSON));
		request.setClientInfo(info);
		Response response = executeRequest(request);
		assertThat(response.getEntity().getMediaType().getName(), 
				is("application/sparql-results+json"));
	}
	
	private Response executeQuery(String query) throws Exception {
		Request request = createRequest(null, Method.GET,
				 null, "?query=" + query);
		return executeRequest(request);
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new QueryResource());
	}
	
}