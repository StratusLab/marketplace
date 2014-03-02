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
import eu.stratuslab.marketplace.server.store.rdf.sesame.SesameNativeBackend;
import eu.stratuslab.marketplace.server.store.rdf.sesame.SesameRdfStore;
import eu.stratuslab.marketplace.server.store.rdf.solr.SolrRdfStore;

public class RdfStoreFactoryImpl implements RdfStoreFactory {
    
    public RdfStore createRdfStore(String provider, String type){
		RdfStore store = null;    	
    	
    	if (provider.equals(RdfStoreFactory.SESAME_PROVIDER)) {
    		SesameBackend backend = null;

    		if(type.equals("native")) {
    			backend = new SesameNativeBackend();
    		} else {
    			backend = new SesameMemoryBackend();
    		}

    		store = new SesameRdfStore(backend);
    	} else if (provider.equals(RdfStoreFactory.SOLR_PROVIDER)) {
    		store = new SolrRdfStore();
    	}
    	
    	return store;
	}
}
