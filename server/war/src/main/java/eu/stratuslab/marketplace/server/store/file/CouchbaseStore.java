package eu.stratuslab.marketplace.server.store.file;

import static eu.stratuslab.marketplace.server.cfg.Parameter.COUCHBASE_BUCKET;
import static eu.stratuslab.marketplace.server.cfg.Parameter.COUCHBASE_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.COUCHBASE_URIS;
import static eu.stratuslab.marketplace.server.cfg.Parameter.COUCHBASE_MARKETPLACEID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.simple.JSONValue;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.ComplexKey;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewDesign;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

public class CouchbaseStore extends FileStore {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private static final String DESIGN_DOC = "marketplace_views";
	private static final String VIEW = "by_timestamp";
	
	CouchbaseClient client = null;
	private String marketplaceId;
	private String lastKeyPath;
	
	private FileStore fileStore;
	
	
	public CouchbaseStore(String dataDir){
		String bucket = Configuration.getParameterValue(COUCHBASE_BUCKET);
		String password = Configuration.getParameterValue(COUCHBASE_PASSWORD);
		marketplaceId = Configuration.getParameterValue(COUCHBASE_MARKETPLACEID);
		
		lastKeyPath = dataDir + File.separator + ".lastkey";
		
		fileStore = new FlatFileStore(dataDir);
		
		String[] uris = Configuration.getParameterValue(COUCHBASE_URIS).split("\\s+");
		
		List<URI> hosts = new LinkedList<URI>();

	    try {
	    	for(String uri : uris){
	    		hosts.add(URI.create(uri));	
	    	}
			
			client = new CouchbaseClient(hosts, bucket, password);
			
			createView();
	    } catch (Exception e){
	    	LOGGER.severe("Error connecting to Couchbase: " + e.getMessage());
		}
	}
	
	private void createView() {
		DesignDocument designDoc = new DesignDocument(DESIGN_DOC);

	    String viewName = VIEW;
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
		jsonDocument.put("marketplaceId", marketplaceId);
		jsonDocument.put("metadata", contents);
		
		try {
			if(client.get(key) == null){
				client.set(key, JSONValue.toJSONString(jsonDocument));
			}
		} catch(Exception e){
			LOGGER.severe("unable to store to couchbase: " + e.getMessage());
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
		
		fileStore.store(key, metadata);
	}

	@Override
	public void remove(String key) {
		client.delete(key);
	}

	@Override
	public String read(String key) {
		String metadata = null;
		
		metadata = fileStore.read(key);
		
		if(metadata == null){
			try {
				String jsonDocument = (String)client.get(key);
		
				@SuppressWarnings("unchecked")
				Map<String, String> document = (HashMap<String, String>)JSONValue.parse(jsonDocument);
		
				metadata = (String)document.get("metadata");
			} catch(Exception e){
				LOGGER.severe("unable to read from couchbase: " + e.getMessage());
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			}
		}
		
		return metadata;
	}

	public List<String> updates(int limit){
		String startKey = getLastKey();
		String lastKey = "";
		
		View view = client.getView(DESIGN_DOC, VIEW);
		Query query = new Query();
		query.setLimit(limit);
		
		if(startKey != null && !startKey.equals("")){
			query.setRangeStart(ComplexKey.of(startKey));
			query.setSkip(1);
		}
		query.setIncludeDocs(true);
		
		List<String> documents = new ArrayList<String>();
		
		try {
			ViewResponse response = client.query(view, query);
			for (ViewRow row : response) {
				String rawDocument = (String) row.getDocument();

				@SuppressWarnings("unchecked")
				Map<String, String> document = (HashMap<String, String>) JSONValue
						.parse(rawDocument);

				if (!document.get("marketplaceId").equals(marketplaceId)) {
					documents.add(document.get("metadata"));
				}

				lastKey = row.getKey();
			}
			setLastKey(lastKey);
		} catch (Exception e) {
			LOGGER.severe("unable to retrieve updates: " + e.getMessage());
		}
		
		return documents;
	}
	
	private String getLastKey() {
		String lastKey = null;
		FileInputStream in = null;
		
		try {
			in = new FileInputStream(lastKeyPath);
			Properties props = new Properties();
        
			props.load(in);
			MetadataFileUtils.closeReliably(in);
			
			lastKey = props.getProperty("lastKey");
		} catch (FileNotFoundException f) {
			MetadataFileUtils.closeReliably(in);
		}catch (IOException e) {
			MetadataFileUtils.closeReliably(in);
		}
        
		return lastKey;
	}
	
	private void setLastKey(String lastKey){
		if (!lastKey.equals("")) {
			Properties props = new Properties();

			FileOutputStream out = null;

			try {
				out = new FileOutputStream(new File(lastKeyPath));
				props.setProperty("lastKey", lastKey);
				props.store(out, null);
				MetadataFileUtils.closeReliably(out);
			} catch (FileNotFoundException f) {
				MetadataFileUtils.closeReliably(out);
			} catch (IOException e) {
				MetadataFileUtils.closeReliably(out);
			}
		}
    }
	
	@Override
	public void shutdown() {
		if (client != null) {
			client.shutdown();
		} else {
			LOGGER.warning("no connection to Couchbase");
		}
	}

}