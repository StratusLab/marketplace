package eu.stratuslab.marketplace.server.store.rdf.sesame;

import java.util.logging.Logger;

import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.memory.MemoryStore;

public class SesameMemoryBackend implements SesameBackend {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	private static final String MEMORY_STORE_WARNING = "memory store being used; data is NOT persistent";
	
	public SailBase getSailBase() {
		LOGGER.warning(MEMORY_STORE_WARNING);
		return new MemoryStore();
	}

	public boolean keepAlive() {
		return false;
	}
}
