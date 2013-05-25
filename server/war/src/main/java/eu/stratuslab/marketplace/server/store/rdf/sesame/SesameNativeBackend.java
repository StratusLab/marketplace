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
