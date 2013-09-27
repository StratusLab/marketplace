package eu.stratuslab.marketplace.server.store.rdf;

import java.util.List;
import java.util.logging.Logger;

import eu.stratuslab.marketplace.server.store.file.FileStore;

public class RdfStoreUpdater {
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
    private static final int DEFAULT_LIMIT = 1000;
	
	private int limit;	
	private FileStore store;
	private Processor processor;
	
	private boolean updating = false;

	public RdfStoreUpdater(FileStore fileStore, Processor processor) {
		this(fileStore, processor, DEFAULT_LIMIT);		
	}

	public RdfStoreUpdater(FileStore fileStore, Processor processor, int limit){
		store = fileStore;
		this.processor = processor;
		this.limit = limit;
	}
	
	public void update() {
		if (!updating) {
			LOGGER.info("update started");
			List<String> documents;
		
			do {
				documents = store.updates(limit);
				
				for (String document : documents){
					processor.processEntry(document);
				}
			
			} while (documents.size() == limit);
		
			updating = false;
			LOGGER.info("update completed");
		}
	}
	
}

