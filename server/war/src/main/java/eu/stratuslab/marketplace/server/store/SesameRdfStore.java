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

import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBNAME;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBPASS;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_DBUSER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_HOST;
import static eu.stratuslab.marketplace.server.cfg.Parameter.RDBMS_PORT;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriterFactory;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriterFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryLockedException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.LockManager;
import org.openrdf.sail.SailException;
import org.openrdf.sail.SailLockedException;
import org.openrdf.sail.helpers.SailBase;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.rdbms.mysql.MySqlStore;
import org.openrdf.sail.rdbms.postgresql.PgSqlStore;

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;

public class SesameRdfStore extends RdfStore {

	private SailBase store = null;
	private Repository metadata = null;
	private ScheduledFuture<?> pingerHandle = null;
	private boolean repositoryLock = false;

	private static final Logger LOGGER = Logger.getLogger("org.restlet");

	private static final String MYSQL_URL_MESSAGE = "using mysql datastore: mysql://%s:xxxxxx@%s:%d/%s";
	private static final String PGSQL_URL_MESSAGE = "using postgres datastore: postgresql://%s:xxxxxx@%s:%d/%s";
	private static final String MEMORY_STORE_WARNING = "memory store being used; data is NOT persistent";

	private static final int HOUR_IN_SECONDS = 3600;
	
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);

	public SesameRdfStore(String storeType) {
		if (storeType.equals(RdfStoreFactory.MYSQL_BACKEND)) {
			store = createMysqlStore();
			createKeepRepositoryAlive();
		} else if (storeType.equals(RdfStoreFactory.POSTGRESQL_BACKEND)) {
			store = createPostgresStore();
			createKeepRepositoryAlive();
		} else {
			LOGGER.warning(MEMORY_STORE_WARNING);
			store = new MemoryStore();
		}
		
	}

	private void createKeepRepositoryAlive(){
		final Runnable pinger = new Runnable() {
			public void run() {
				lockRepository();
				reInitialize();
				unlockRepository();
			}
		};

		/*
		 * Ping the repository once an hour to make sure 
		 * the connection is not closed
		 */
		pingerHandle = scheduler.scheduleAtFixedRate(pinger, HOUR_IN_SECONDS, HOUR_IN_SECONDS,
				TimeUnit.SECONDS);
	}
	
	public void initialize() {
		metadata = new SailRepository(store);
		try {
			metadata.initialize();
		} catch (RepositoryLockedException l) {
			if (l.getCause() instanceof SailLockedException) {
				SailLockedException sle = (SailLockedException)l.getCause();
				
				LockManager lockManager = sle.getLockManager();
				if (lockManager != null && lockManager.isLocked()) {
					LOGGER.warning("repository already locked, attempting to revoke.");
					
					boolean revoked = lockManager.revokeLock();
					if(revoked){
						try {
							metadata.initialize();
						} catch (RepositoryException e) {
							LOGGER.severe("error initializing repository: " 
									+ e.getMessage());
						}
					}
				}
				
			} else {
				// nothing can be done
				LOGGER.severe("error initializing repository: " + l.getMessage());
			}
		} catch (RepositoryException r) {
			LOGGER.severe("error initializing repository: " + r.getMessage());
		}
	}

	public boolean store(String identifier, String entry) {
		boolean success = false;
		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			ValueFactory vf = con.getValueFactory();
			Reader reader = new StringReader(entry);
			try {
				con.clear(vf.createURI(identifier));
				con.add(reader, MARKETPLACE_URI, RDFFormat.RDFXML,
						vf.createURI(identifier));
			} finally {
				con.close();
			}
			success = true;
		} catch (RepositoryException e) {
			LOGGER.severe("Unable to clear metadata entry: " + e.getMessage());
		} catch (java.io.IOException e) {
			LOGGER.severe("Error storing metadata entry: " + e.getMessage());
		} catch (org.openrdf.rio.RDFParseException e) {
			LOGGER.severe(e.getMessage());
		}

		return success;
	}

	@Override
	public String getRdfEntry(String uri) throws MarketplaceException {
		return null;
	}
	
	public List<Map<String, String>> getRdfEntriesAsMap(String query)
			throws MarketplaceException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			try {
				TupleQuery tupleQuery = con.prepareTupleQuery(
						QueryLanguage.SPARQL, query);
				TupleQueryResult results = tupleQuery.evaluate();
				try {
					List<String> columnNames = results.getBindingNames();
					int cols = columnNames.size();

					while (results.hasNext()) {
						BindingSet solution = results.next();
						HashMap<String, String> row = new HashMap<String, String>(
								cols, 1);
						for (Iterator<String> namesIter = columnNames
								.listIterator(); namesIter.hasNext();) {
							String columnName = namesIter.next();
							Value columnValue = solution.getValue(columnName);
							if (columnValue != null) {
								row.put(columnName, (solution
										.getValue(columnName)).stringValue());
							} else {
								row.put(columnName, "null");
							}
						}
						list.add(row);
					}
				} finally {
					results.close();
				}
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch (IllegalStateException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch (MalformedQueryException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch (QueryEvaluationException e) {
			throw new MarketplaceException(e.getMessage(), e);
		}

		return list;
	}

	public String getRdfEntriesAsXml(String query) throws MarketplaceException {
		TupleQueryResultWriterFactory writerFactory = new SPARQLResultsXMLWriterFactory();
		
		return getRdfEntriesAsString(query, writerFactory);
	}
	
	public String getRdfEntriesAsJson(String query) throws MarketplaceException {
		TupleQueryResultWriterFactory writerFactory = new SPARQLResultsJSONWriterFactory();
		
		return getRdfEntriesAsString(query, writerFactory);
	}
	
	public String getRdfEntriesAsString(String query,
			TupleQueryResultWriterFactory writerFactory)
			throws MarketplaceException {
		String resultString = null;

		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			BufferedOutputStream out = new BufferedOutputStream(bytes);
			TupleQueryResultWriter writer = writerFactory.getWriter(out);

			RepositoryConnection con = getMetadataStore().getConnection();

			try {
				TupleQuery tupleQuery = con.prepareTupleQuery(
						QueryLanguage.SPARQL, query);
				tupleQuery.evaluate(writer);
				resultString = bytes.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new MarketplaceException(e.getMessage(), e);
			} finally {
				con.close();
			}

		} catch (RepositoryException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch (MalformedQueryException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch (QueryEvaluationException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch (org.openrdf.query.TupleQueryResultHandlerException e) {
			throw new MarketplaceException(e.getMessage(), e);
		}
		return resultString;
	}

	public void remove(String identifier) {
		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			ValueFactory vf = con.getValueFactory();
			try {
				con.clear(vf.createURI(identifier));
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			LOGGER.severe("Error removing metadata entry: " + e.getMessage());
		}
	}

	private void reInitialize() {
		if (store != null) {
			try {
				store.shutDown();
			} catch (SailException e) {
				LOGGER.warning("error shutting down repository: "
						+ e.getMessage());
			}
		}

		try {
			metadata.initialize();
		} catch (RepositoryException r) {
			LOGGER.severe("error initializing repository: " + r.getMessage());
		}
	}

	private void lockRepository() {
		this.repositoryLock = true;
	}

	private void unlockRepository() {
		this.repositoryLock = false;
	}

	private Repository getMetadataStore() {
		
		while (this.repositoryLock) {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
			}
		}
		return this.metadata;
	}

	private static MySqlStore createMysqlStore() {
		String mysqlDb = Configuration.getParameterValue(RDBMS_DBNAME);
		String mysqlHost = Configuration.getParameterValue(RDBMS_HOST);
		int mysqlPort = Configuration.getParameterValueAsInt(RDBMS_PORT);
		String mysqlUser = Configuration.getParameterValue(RDBMS_DBUSER);
		String mysqlPass = Configuration.getParameterValue(RDBMS_DBPASS);

		LOGGER.info(String.format(MYSQL_URL_MESSAGE, mysqlUser, mysqlHost,
				mysqlPort, mysqlDb));

		MySqlStore mysqlStore = new MySqlStore();
		mysqlStore.setDatabaseName(mysqlDb);
		mysqlStore.setServerName(mysqlHost);
		mysqlStore.setPortNumber(mysqlPort);
		mysqlStore.setUser(mysqlUser);
		mysqlStore.setPassword(mysqlPass);

		return mysqlStore;
	}

	private static PgSqlStore createPostgresStore() {
		String pgsqlDb = Configuration.getParameterValue(RDBMS_DBNAME);
		String pgsqlHost = Configuration.getParameterValue(RDBMS_HOST);
		int pgsqlPort = Configuration.getParameterValueAsInt(RDBMS_PORT);
		String pgsqlUser = Configuration.getParameterValue(RDBMS_DBUSER);
		String pgsqlPass = Configuration.getParameterValue(RDBMS_DBPASS);

		LOGGER.info(String.format(PGSQL_URL_MESSAGE, pgsqlUser, pgsqlHost,
				pgsqlPort, pgsqlDb));

		PgSqlStore pgsqlStore = new PgSqlStore();
		pgsqlStore.setDatabaseName(pgsqlDb);
		pgsqlStore.setServerName(pgsqlHost);
		pgsqlStore.setPortNumber(pgsqlPort);
		pgsqlStore.setUser(pgsqlUser);
		pgsqlStore.setPassword(pgsqlPass);

		return pgsqlStore;
	}

	public void shutdown() {
		if (store != null) {
			try {
				store.shutDown();
			} catch (SailException e) {
				LOGGER.warning("error shutting down repository: "
						+ e.getMessage());
			}
		}

		if(pingerHandle != null){
			pingerHandle.cancel(true);
		}
	}

}
