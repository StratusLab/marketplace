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

import java.util.logging.Logger;

import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.memory.MemoryStore;

public class SesameMemoryBackend implements SesameBackend {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	private static final String MEMORY_STORE_WARNING = "memory store being used; data is NOT persistent";
	
	public SailBase getSailBase() {
		LOGGER.warning(MEMORY_STORE_WARNING);
		return new MemoryStore();
	}

	public boolean keepAlive() {
		return false;
	}
}
