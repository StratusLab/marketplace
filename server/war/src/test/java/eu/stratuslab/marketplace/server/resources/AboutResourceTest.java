package eu.stratuslab.marketplace.server.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ServerResource;
import org.restlet.Client;
import org.restlet.data.Protocol;
import org.restlet.data.Method;
import org.restlet.representation.StringRepresentation;
import org.restlet.data.MediaType;


import eu.stratuslab.marketplace.server.resources.AboutResource;
import eu.stratuslab.marketplace.server.MarketPlaceApplication;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.Application;

public class AboutResourceTest extends ResourceTestBase {
	
	Application application;
	/*
	@Before
	public void setUp() throws Exception {
		// Create a new Component.
        Component component = new Component();
		application = new MarketPlaceApplication("memory");
		component.getDefaultHost().attach("/", application);
		component.getClients().add(Protocol.CLAP);
		application.setContext(component.getDefaultHost().getContext());
		application.createInboundRoot();
	}
	
	@After
	public void tearDown() throws Exception {
		application.stop();
	}*/
	
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
		//ServerResource about = new AboutResource();
		//about.setApplication(application);
		return executeRequest(request, new AboutResource());
	}
		
}
