package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.data.Method;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class QueryResourceTest extends ResourceTestBase {
	
	@Before 
	public void setUp() throws Exception {
		postMetadataFile("valid-indate-signature.xml");
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
	
	private Response executeQuery(String query) throws Exception {
		Request request = createRequest(null, Method.GET,
				 null, "?query=" + query);
		return executeRequest(request);
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new QueryResource());
	}
	
}