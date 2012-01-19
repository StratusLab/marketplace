package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class AboutResourceTest extends ResourceTestBase {
	
	@Test
	public void getAbout() throws Exception {
		Request request = createGetRequest("test", "test");
		Response response = executeRequest(request);

		assertThat(response.getStatus(), is(Status.SUCCESS_OK));
	}
	
	private Request createGetRequest(String key, String value)
	throws Exception {
		Map<String, Object> attributes = createAttributes(key, value);
		Request request = createGetRequest(attributes);
		return request;
	}
	
	private Response executeRequest(Request request) {
		return executeRequest(request, new AboutResource());
	}
		
}
