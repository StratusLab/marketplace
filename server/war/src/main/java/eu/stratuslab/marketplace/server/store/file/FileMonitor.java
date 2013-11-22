package eu.stratuslab.marketplace.server.store.file;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;

public class FileMonitor {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private SortedMap<String, File> newFiles;
	
    public FileMonitor(String directory, String suffix) {
    	newFiles = Collections.synchronizedSortedMap(new TreeMap<String, File>());
    	
        // The monitor will perform polling on the folder every 5 seconds
        final long pollingInterval = 5 * 1000;

        File folder = new File(directory);

        if (!folder.exists()) {
            // Test to see if monitored folder exists
            throw new RuntimeException("Directory not found: " + directory);
        }

        IOFileFilter directories = FileFilterUtils.and(
                                      FileFilterUtils.directoryFileFilter(),
                                      HiddenFileFilter.VISIBLE);
        IOFileFilter files = FileFilterUtils.and(
                                      FileFilterUtils.fileFileFilter(),
                                      FileFilterUtils.suffixFileFilter(suffix));
        IOFileFilter filter = FileFilterUtils.or(directories, files);

        FileAlterationObserver observer = new FileAlterationObserver(folder, filter);
        FileAlterationMonitor monitor =
                new FileAlterationMonitor(pollingInterval);
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            // Is triggered when a file is created in the monitored folder
            @Override
            public void onFileCreate(File file) {
                try {
                    String filename = FilenameUtils.
                    		getBaseName(file.getCanonicalPath());
                    newFiles.put(filename, file);
                } catch (IOException e) {
                    LOGGER.severe("Error reading new file: " + e.getMessage());
                }
            }
        };

        observer.addListener(listener);
        monitor.addObserver(observer);
        try {
			monitor.start();
		} catch (Exception e) {
			LOGGER.severe("Unable to start file monitor: " + e.getMessage());
		}
    }
    
    public File getFile(){
    	File file = null;
    	
    	try {
    		String key = newFiles.firstKey();
    		file = newFiles.remove(key);
    	} catch(NoSuchElementException e){    		
    	}
    	
    	return file;
    }
}