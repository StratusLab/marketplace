package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.w3c.dom.Document;

import org.junit.Before;
import org.junit.Test;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.ClientInfo;
import org.restlet.data.Preference;
import org.restlet.data.Method;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class MDatumResourceTest extends ResourceTestBase {
	
	private String iri;
	
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