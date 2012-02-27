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
package eu.stratuslab.marketplace.server.store;

public interface RdfStoreFactory {
	
	public static final String SESAME_PROVIDER = "sesame";
	public static final String MYSQL_BACKEND = "mysql";
	public static final String POSTGRESQL_BACKEND = "postgres";
	public static final String MEMORY_BACKEND = "memory";
	
	public RdfStore createRdfStore(String provider, String type);
}
