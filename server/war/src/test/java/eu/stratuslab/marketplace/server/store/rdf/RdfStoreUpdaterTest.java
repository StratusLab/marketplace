package eu.stratuslab.marketplace.server.store.rdf;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.server.store.file.FileStore;
import eu.stratuslab.marketplace.server.util.ResourceTestBase;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class RdfStoreUpdaterTest extends ResourceTestBase {
	
	private static final int TEST_LIMIT = 15;
	private String classPrefix = "/eu/stratuslab/marketplace/server/resources/";

	private RdfStoreUpdater rdfUpdater;
	private FileStore fileStore;
	private Processor processor;
	
	@Before
	public void setUp() throws Exception {
		fileStore = createStrictMock(FileStore.class);
	    processor = createStrictMock(Processor.class);
	}
	
	@Test
	public void testGetUpdates() throws Exception {
	    rdfUpdater = new RdfStoreUpdater(fileStore, processor, TEST_LIMIT);
		
		Document metadata = extractXmlDocument(
				this.getClass().getResourceAsStream(classPrefix + "valid-indate-signature.xml"));
		List<String> updates = new ArrayList<String>(); 
		
		int noOfUpdates = 20;
		
		for(int i = 0; i < noOfUpdates; i++){
			updates.add(XMLUtils.documentToString(metadata));
		}
		
	    expect(fileStore.updates(TEST_LIMIT))
	    	.andReturn(updates.subList(0, TEST_LIMIT))
	    	.times(1)
	    	.andReturn(updates.subList(TEST_LIMIT, noOfUpdates))
	    	.times(1);
	    
	    processor.processEntry(updates.get(0));
	    EasyMock.expectLastCall().times(noOfUpdates);
	    
		replay(fileStore);
		replay(processor);
		
		rdfUpdater.update();
		
		verify(fileStore);
		verify(processor);
	}

}
