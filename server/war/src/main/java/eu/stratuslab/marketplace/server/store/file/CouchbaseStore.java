package eu.stratuslab.marketplace.server.store.file;

import static eu.stratuslab.marketplace.server.cfg.Parameter.COUCHBASE_BUCKET;
import static eu.stratuslab.marketplace.server.cfg.Parameter.COUCHBASE_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.COUCHBASE_URIS;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONValue;
import org.w3c.dom.Document;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.ViewDesign;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.server.cfg.Configuration;

public class CouchbaseStore extends FileStore {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	CouchbaseClient client = null;
	
	public CouchbaseStore(){
		String bucket = Configuration.getParameterValue(COUCHBASE_BUCKET);
		String password = Configuration.getParameterValue(COUCHBASE_PASSWORD);
		String[] uris = Configuration.getParameterValue(COUCHBASE_URIS).split("\\s+");
		
		List<URI> hosts = new LinkedList<URI>();

	    try {
	    	for(String uri : uris){
	    		hosts.add(URI.create(uri));	
	    	}
			
			client = new CouchbaseClient(hosts, bucket, password);
			
			createView();
		} catch (IOException e) {
			LOGGER.severe("Error connecting to Couchbase: " + e.getMessage());
		}
	}
	
	private void createView() {
		// TODO Auto-generated method stub
		DesignDocument designDoc = new DesignDocument("marketplace_views");

	    String viewName = "by_timestamp";
	    String mapFunction =
	            "function (doc, meta) {\n" +
	            "    emit(doc.uploaded);\n" +
	            "}";

	    ViewDesign viewDesign = new ViewDesign(viewName,mapFunction);
	    designDoc.getViews().add(viewDesign);
	    client.createDesignDoc( designDoc );
	}

	@Override
	public void store(String key, Document metadata) {
		String contents = XMLUtils.documentToString(metadata);
		String uploaded = Long.toString(System.currentTimeMillis());
		
		Map<String, String> jsonDocument = new HashMap<String, String>();
		jsonDocument.put("uploaded", uploaded);
		jsonDocument.put("metadata", contents);
		
		client.set(key, JSONValue.toJSONString(jsonDocument));
	}

	@Override
	public void remove(String key) {
		client.delete(key);
	}

	@Override
	public String read(String key) {
		String jsonDocument = (String)client.get(key);
		
		@SuppressWarnings("unchecked")
		Map<String, String> document = (HashMap<String, String>) JSONValue.parse(jsonDocument);
		
		return (String)document.get("metadata");
	}

	@Override
	public void shutdown() {
		client.shutdown();
	}

}
