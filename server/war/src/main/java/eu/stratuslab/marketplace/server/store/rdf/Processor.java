package eu.stratuslab.marketplace.server.store.rdf;

import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

import eu.stratuslab.marketplace.server.resources.MDataResource;

public class Processor {
	
	private Application application;
	
	public Processor(Application app){
		application = app;
	}
	
	public void processEntry(String entry) {
		Representation rdf = new StringRepresentation(entry,
				MediaType.APPLICATION_RDF_XML);
		Request request = new Request(Method.POST, "http://localhost.replica");
		request.setEntity(rdf);
		request.setRootRef(new Reference("http://localhost.replica"));
		
		Response response = new Response(request);
		
		ServerResource resource = new MDataResource();
		resource.setApplication(application);
		
		resource.init(null, request, response);
		
		if (response.getStatus().isSuccess()) {
			resource.handle();
		}		
		
		resource.release();
	}
}
