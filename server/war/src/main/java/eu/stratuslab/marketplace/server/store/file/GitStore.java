package eu.stratuslab.marketplace.server.store.file;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

public class GitStore extends FileStore {

	private FileStore fileStore;
	private String dataDir;
	
	private HashMap<String, Document> localUpdates = new HashMap<String, Document>();
	
	private final ScheduledExecutorService repoUpdater = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> updaterHandle;
	
	private GitManager manager;
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private FileMonitor monitor;
	
	public GitStore(FileStore store) {
		dataDir = Configuration.getParameterValue(DATA_DIR);
		
		monitor = new FileMonitor(dataDir, ".xml");
		
		manager = new GitManager(dataDir);
		
		fileStore = store;
		
		final Runnable updater = new Runnable() {
        	public void run() {
        		LOGGER.info("Updating from git repository.");
        		manager.pull();
        	}
        };
        
        updaterHandle = repoUpdater.scheduleWithFixedDelay(updater, 
    			5, 5, TimeUnit.MINUTES);
    }
	
	private String getKeyFromPath(String path) {
		String[] elements = path.split(File.separatorChar == '\\' ? "\\\\"
				: File.separator);
		int length = elements.length;
		String key = elements[length - 3] + "/" + elements[length - 2] + "/"
				+ FilenameUtils.getBaseName(elements[length - 1]);

		return key;
	}
	
	@Override
	public void store(String key, Document metadata) {
		fileStore.store(key,  metadata);
		localUpdates.put(key, metadata);
		
		manager.addToRepo(key);
	}

	@Override
	public void remove(String key) {
		manager.removeFromRepo(key);
		fileStore.remove(key);
	}

	@Override
	public String read(String key) {
		return fileStore.read(key);
	}

	@Override
	public List<String> updates(int limit) {
		List<String> updates = new ArrayList<String>();
		int i = 0;

		File file = monitor.getFile();

		while(i < limit && file != null){
			try {
				String key = getKeyFromPath(file.getCanonicalPath());

				if (!localUpdates.containsKey(key)) {
					String document = MetadataFileUtils.
							readFileAsString(file.getCanonicalPath());
					updates.add(document);

					i++;
				} else {
					localUpdates.remove(key);
				}
				file = monitor.getFile();

			} catch (IOException e) {
				LOGGER.severe("Unable to read file: " + e.getMessage());
			}
		}

		return updates;
	}

	@Override
	public void shutdown() {
		manager.close();
		fileStore.shutdown();
		updaterHandle.cancel(true);
	}
	
}
