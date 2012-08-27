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

import java.util.List;
import java.util.Map;

import eu.stratuslab.marketplace.server.MarketplaceException;

public abstract class RdfStore {
	
	public abstract void shutdown();
	public abstract void initialize();
    public abstract boolean store(String identifier, String entry);
    public abstract List<Map<String, String>> getRdfEntriesAsMap(String query) throws MarketplaceException;
    public abstract String getRdfEntriesAsXml(String query) throws MarketplaceException;
    public abstract String getRdfEntriesAsJson(String query) throws MarketplaceException;
    public abstract String getRdfEntry(String uri) throws MarketplaceException;
    public abstract void remove(String identifier);
    public abstract int size();
}
	
