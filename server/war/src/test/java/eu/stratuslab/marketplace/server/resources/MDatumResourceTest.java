package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;

import org.w3c.dom.Document;

import org.junit.Before;
import org.junit.Test;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.data.MediaType;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.data.ClientInfo;
import org.restlet.data.Preference;
import org.restlet.data.Method;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class MDatumResourceTest extends ResourceTestBase {
	
	private String iri = "/metadata/" +
	"BEE8-MMAw-Lk_IgsEExAy3d9R8h/" +
	"jane.tester@example.org/" +
	"2011-09-09T14:12:59Z";
	
	@Before 
	public void setUp() throws Exception {
		Request request = createPostRequest("valid-indate-signature.xml");
		Response response = executePostRequest(request);
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
	
	private Request createPostRequest(String filename)
	throws Exception{
		Representation rdf = new InputRepresentation(
				this.getClass().getResourceAsStream(filename),
                MediaType.APPLICATION_RDF_XML);
		Request request = createPostRequest(createAttributes("test","test"), rdf);
		return request;
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new MDatumResource());
	}
	private Response executePostRequest(Request request) {
		return executeRequest(request, new MDataResource());
	}
	
}