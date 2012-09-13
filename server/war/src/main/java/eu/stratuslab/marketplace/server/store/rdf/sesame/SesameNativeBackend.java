package eu.stratuslab.marketplace.server.store.rdf.sesame;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;

import java.io.File;
import java.util.logging.Logger;

import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.nativerdf.NativeStore;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

public class SesameNativeBackend implements SesameBackend {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	private static final String NATIVE_STORE_MESSAGE = "using native datastore: file://%s";
	
	public SailBase getSailBase() {
		String dataDir = Configuration.getParameterValue(DATA_DIR);
		String dataStore = dataDir + File.separator + "datastore";
        boolean success = MetadataFileUtils.createIfNotExists(dataStore);
        if(!success){
        	LOGGER.severe("Unable to create directory: " + dataStore);
        }
        
        LOGGER.info(String.format(NATIVE_STORE_MESSAGE, dataStore));
        
		return new NativeStore(new File(dataStore));
	}

	public boolean keepAlive() {
		return false;
	}
}
