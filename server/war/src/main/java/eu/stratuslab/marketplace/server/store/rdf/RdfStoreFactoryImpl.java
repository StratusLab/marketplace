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
package eu.stratuslab.marketplace.server.store.rdf;

import eu.stratuslab.marketplace.server.store.rdf.sesame.SesameBackend;
import eu.stratuslab.marketplace.server.store.rdf.sesame.SesameMemoryBackend;
import eu.stratuslab.marketplace.server.store.rdf.sesame.SesameMySQLBackend;
import eu.stratuslab.marketplace.server.store.rdf.sesame.SesameNativeBackend;
import eu.stratuslab.marketplace.server.store.rdf.sesame.SesamePostgresBackend;
import eu.stratuslab.marketplace.server.store.rdf.sesame.SesameRdfStore;

public class RdfStoreFactoryImpl implements RdfStoreFactory {
	public RdfStore createRdfStore(String provider, String type){
		if(type.equals("remote")){
			return new RemoteStore();
		} else {
			SesameBackend backend = null;
			
			if(type.equals("mysql")){
				backend = new SesameMySQLBackend();
			} else if(type.equals("postgres")){
				backend = new SesamePostgresBackend();
			} else if(type.equals("native")) {
				backend = new SesameNativeBackend();
			} else {
				backend = new SesameMemoryBackend();
			}
			
			return new SesameRdfStore(backend);
		}
	}
}
