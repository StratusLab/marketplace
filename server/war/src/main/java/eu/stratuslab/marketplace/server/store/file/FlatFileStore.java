package eu.stratuslab.marketplace.server.store.file;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.metadata.MetadataUtils;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;

public class FlatFileStore extends FileStore {
	
	private String dataDir;
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	public FlatFileStore(){
		dataDir = Configuration.getParameterValue(DATA_DIR);
		MetadataFileUtils.createIfNotExists(dataDir);
	}
	
	@Override
	public void store(String key, Document metadata) {
		File rdfFile = new File(dataDir, File.separator + key + ".xml");

		File rdfFileParent = rdfFile.getParentFile();
		if (!rdfFileParent.exists()) {
			if (!rdfFileParent.mkdirs()) {
				LOGGER.severe("Unable to create directory structure for file.");
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
			}
		}

		String contents = XMLUtils.documentToString(metadata);
		MetadataUtils.writeStringToFile(contents, rdfFile);			
	}

	@Override
	public void remove(String key) {
		// TODO Auto-generated method stub
		File rdfFile = new File(dataDir, File.separator + key + ".xml");

		if (rdfFile.exists()) {
			if (!rdfFile.delete()) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
			}
		}
	}
	
	@Override
	public String read(String key) {
		String model = null;
		try {
			model = MetadataFileUtils.readFileAsString(dataDir 
					+ File.separator + key + ".xml");
		} catch (IOException e) {
			LOGGER.severe("Unable to read metadata: " + key);
		}

		return model;
	}
	
}
