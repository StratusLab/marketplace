package eu.stratuslab.marketplace.server.store.file;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class GitStoreTest extends ResourceTestBase {
	private static final int TEST_LIMIT = 15;

	private GitStore gitStore;
	
	private FileStore fileStore;
	private FileMonitor monitor;
	private GitManager manager;
	
	@Before
	public void setUp() throws Exception {
		fileStore = createStrictMock(FileStore.class);
		monitor = createStrictMock(FileMonitor.class);
		manager = createStrictMock(GitManager.class);
	}

	@Test
	public void testGetUpdates() throws Exception {
	    gitStore = new GitStore(fileStore, monitor, manager);
		
		List<File> files = new ArrayList<File>(); 
		
		int noOfUpdates = 20;
		
		for(int i = 0; i < noOfUpdates; i++){
			files.add(File.createTempFile("marketplace", ".xml"));
		}
		
	    expect(monitor.getFile())
	    	.andReturn(files.remove(0))
	    	.times(TEST_LIMIT)
	    	.andReturn(files.remove(0))
	    	.times(noOfUpdates - TEST_LIMIT)
	    	.andReturn(null)
	    	.times(1);
	    	
		replay(monitor);
		
		gitStore.updates(TEST_LIMIT);
		gitStore.updates(TEST_LIMIT);
	    		
		verify(monitor);
	}
	
}
