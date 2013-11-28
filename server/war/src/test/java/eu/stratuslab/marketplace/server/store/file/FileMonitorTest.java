package eu.stratuslab.marketplace.server.store.file;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

public class FileMonitorTest extends ResourceTestBase {
	
	private static String tmpDir;

	@BeforeClass
	public static void setUp() throws Exception {
		tmpDir = getTempDir("marketplace");
		MetadataFileUtils.createIfNotExists(tmpDir);
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		FileUtils.deleteDirectory(new File(tmpDir));
	}
	
	@Test
	public void testGetFiles() throws Exception {
		FileMonitor monitor = new FileMonitor(tmpDir, ".xml");
		
		File tmpFile = File.createTempFile("test", ".xml", new File(tmpDir));
		
		Thread.sleep(6 * 1000L);
		
		File monitorFile =  monitor.getFile();
		
		assertNotNull(monitorFile);
		assertThat(monitorFile.getCanonicalPath(), is(tmpFile.getCanonicalPath()));
	}
	
	@Test
	public void testFileOrder() throws Exception {
		FileMonitor monitor = new FileMonitor(tmpDir, ".xml");
		
		File file1 = new File(tmpDir + File.separator + "2012-08-02T10:25:09Z.xml");
		File file2 = new File(tmpDir + File.separator + "2013-08-02T10:25:09Z.xml");
		File file3 = new File(tmpDir + File.separator + "2013-11-02T10:25:09Z.xml");
		
		file1.createNewFile();
		file3.createNewFile();
		file2.createNewFile();
		
		Thread.sleep(6 * 1000L);
		
		assertThat(monitor.getFile().getCanonicalPath(), is(file1.getCanonicalPath()));
		assertThat(monitor.getFile().getCanonicalPath(), is(file2.getCanonicalPath()));
		assertThat(monitor.getFile().getCanonicalPath(), is(file3.getCanonicalPath()));
	}
	

}
