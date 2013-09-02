package eu.stratuslab.marketplace.server.utils;

import java.util.List;
import java.util.logging.Logger;

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
import eu.stratuslab.marketplace.server.store.file.FileStore;

public class RdfStoreUpdater {
	private static final Logger LOGGER = Logger.getLogger("org.restlet");

	private static final int LIMIT = 1000;
	
	private Application application;

	private FileStore store;

	public RdfStoreUpdater(Application app, FileStore fileStore) {
		application = app;
		store = fileStore;
		
	}

	public void update() {
		List<String> documents;
		
		do {
			documents = store.updates(LIMIT);
			
			for (String document : documents){
				processEntry(document);
			}
			
		} while (documents.size() == LIMIT);
		
	}

	private void processEntry(String metadata) {
		Representation rdf = new StringRepresentation(metadata,
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
	}

}

