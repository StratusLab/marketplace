package eu.stratuslab.marketplace.server.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

public class SyncResource extends BaseResource {

	@Get("txt")
	public Representation syncMetadata(Representation entity) {
        Representation rep = null;
		boolean processed = false;
        
		File syncFile = new File(getDataDir() + File.separator + ".sync");
		if(syncFile.exists()){    
			processed = processSyncList(syncFile);
			
			if(processed){
				setStatus(Status.SUCCESS_OK);
				rep = new StringRepresentation("sync list processed");
			} else {
				setStatus(Status.SERVER_ERROR_INTERNAL);
				rep = new StringRepresentation("unable to process sync list");
			}
			
		} else {
			setStatus(Status.SUCCESS_ACCEPTED);
			rep = new StringRepresentation("no sync list to process");
		}

		return rep;	
	}

	private boolean processSyncList(File syncFileList) {
		BufferedReader syncFileListReader = null;
		String dataDir = getDataDir();
		boolean success = true;
		
		try {
			syncFileListReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(syncFileList), "UTF-8"));

			String syncFilePath;
			while ((syncFilePath = syncFileListReader.readLine()) != null){
				File syncFile = new File(dataDir + File.separator + syncFilePath);
				if(syncFile.exists()){
					publishSyncFile(syncFile);
				} else {
					success = false;
					LOGGER.warning("Unable to load sync file: " + syncFile.getPath());
				}
			}

		} catch (IOException e) {
			success = false;
			LOGGER.warning("Unable to process sync list: " + e.getMessage());
		} finally {
			MetadataFileUtils.closeReliably(syncFileListReader);
		}

		return success;
	}

	private void publishSyncFile(File syncFile) {
		InputStream stream = null;
		Document metadataXml = null;

		try {

			stream = new FileInputStream(syncFile);
			metadataXml = MetadataFileUtils.extractXmlDocument(stream);
			writeMetadataToStore(metadataXml);

		} catch (FileNotFoundException e) {
			LOGGER.severe("unable to read metadata file: "
					+ syncFile.getAbsolutePath());
		} finally {
			MetadataFileUtils.closeReliably(stream);
		}
	}

}
