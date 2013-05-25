/**
 * Created as part of the StratusLab project (http://stratuslab.eu),
 * co-funded by the European Commission under the Grant Agreement
 * INSFO-RI-261552.
 *
 * Copyright (c) 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratuslab.marketplace.server.resources;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;
import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;

public class SyncResource extends BaseResource {

	private String dataDir = Configuration.getParameterValue(DATA_DIR);
	
	private static final String ENCODING = "UTF-8";
	
	@Post
	public Representation syncEntry(Representation entity){
		Representation rep = null;
		
		Form form = new Form(entity);  
        String key = form.getFirstValue("path");
                
        if(key != null){
        	boolean success = syncMetadata(key); 

        	if(success){
        		setStatus(Status.SUCCESS_ACCEPTED);
        		rep = new StringRepresentation("synced metadata entry");
        	} else {
        		setStatus(Status.SERVER_ERROR_INTERNAL);
            	rep = new StringRepresentation("unable to sync metadata entry");
        	}
        } else {
        	rep = syncMetadataList();
        }
                
        return rep;
	}
	
	private Representation syncMetadataList() {
        Representation rep = null;
		boolean processed = false;
        
		File syncFile = new File(dataDir
				+ File.separator + ".sync");
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
		boolean success = true;
		
		try {
			syncFileListReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(syncFileList), ENCODING));

			String syncFilePath;
			while ((syncFilePath = syncFileListReader.readLine()) != null){
				success = syncMetadata(syncFilePath);
			}

		} catch (IOException e) {
			success = false;
			LOGGER.warning("Unable to process sync list: " + e.getMessage());
		} finally {
			MetadataFileUtils.closeReliably(syncFileListReader);
		}

		return success;
	}
	
	private boolean syncMetadata(String key){
		boolean success = true;
		
		String metadata = getMetadataFileStore().read(key);
		if(metadata != null){
			success = storeMetadata(metadata);
		} else {
			success = false;
			LOGGER.warning("Unable to sync metadata: " + key);
		}
		
		return success;
	}
	
	private boolean storeMetadata(String metadata){
		boolean success = true;
		
		InputStream stream = null;
		Document metadataXml = null;

		try {
			stream = new ByteArrayInputStream(metadata.getBytes(ENCODING));
			metadataXml = MetadataFileUtils.extractXmlDocument(stream);
			writeMetadataToRdfStore(metadataXml);
		}catch(UnsupportedEncodingException e){
		  success = false;
		} finally {
			MetadataFileUtils.closeReliably(stream);
		}
		
		return success;
	}
	
}