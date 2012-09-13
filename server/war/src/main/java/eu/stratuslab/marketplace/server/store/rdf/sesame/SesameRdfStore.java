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

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLTERMS_NS_URI;

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
import org.openrdf.model.impl.LiteralImpl;
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

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.store.rdf.RdfStore;

public class SesameRdfStore extends RdfStore {

	private SailBase store = null;
	private Repository metadata = null;
	private ScheduledFuture<?> pingerHandle = null;
	private boolean repositoryLock = false;

	private static final Logger LOGGER = Logger.getLogger("org.restlet");

	private static final int HOUR_IN_SECONDS = 3600;
	
	private static final String ERROR_INITIALIZE = "error initializing repository: ";
	
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);

	public SesameRdfStore(SesameBackend backend) {
			store = backend.getSailBase();	
			if(backend.keepAlive()){
				createKeepRepositoryAlive();
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
		} catch (RepositoryLockedException locked) {
			if (locked.getCause() instanceof SailLockedException) {
				SailLockedException sle = (SailLockedException)locked.getCause();
				
				LockManager lockManager = sle.getLockManager();
				if (lockManager != null && lockManager.isLocked()) {
					LOGGER.warning("repository already locked, attempting to revoke.");
					
					boolean revoked = lockManager.revokeLock();
					if(revoked){
						try {
							metadata.initialize();
						} catch (RepositoryException e) {
							LOGGER.severe(ERROR_INITIALIZE
									+ e.getMessage());
						}
					}
				}
				
			} else {
				// nothing can be done
				LOGGER.severe(ERROR_INITIALIZE + locked.getMessage());
			}
		} catch (RepositoryException r) {
			LOGGER.severe(ERROR_INITIALIZE + r.getMessage());
		}
	}

	public void tag(String identifier, String tag){
		String idURI = "/" + identifier;
		
		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			ValueFactory vf = con.getValueFactory();
			
			try {
				con.add(vf.createURI(MARKETPLACE_URI + "#" + identifier),
						vf.createURI(SLTERMS_NS_URI + "tag"),
						new LiteralImpl(tag),
						vf.createURI(idURI));									
			} finally {
				con.close();
			}
		
		} catch (RepositoryException e) {
			LOGGER.severe("Unable to clear metadata entry: " + e.getMessage());
		}
	}
	
	public void removeTag(String identifier, String tag){
		String idURI = "/" + identifier;
		
		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			ValueFactory vf = con.getValueFactory();
			
			try {
				con.remove(vf.createURI(MARKETPLACE_URI + "#" + identifier),
						vf.createURI(SLTERMS_NS_URI + "tag"),
						new LiteralImpl(tag),
						vf.createURI(idURI));									
			} finally {
				con.close();
			}
		
		} catch (RepositoryException e) {
			LOGGER.severe("Unable to clear metadata entry: " + e.getMessage());
		}
	}
	
	public boolean store(String identifier, String entry) {
		boolean success = false;
		String idURI = "/" + identifier;
		
		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			ValueFactory vf = con.getValueFactory();
			Reader reader = new StringReader(entry);
			try {
				con.clear(vf.createURI(idURI));
				con.add(reader, MARKETPLACE_URI, RDFFormat.RDFXML,
						vf.createURI(idURI));				
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
		String idURI = "/" + identifier;
		
		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			ValueFactory vf = con.getValueFactory();
			try {
				con.clear(vf.createURI(idURI));
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			LOGGER.severe("Error removing metadata entry: " + e.getMessage());
		}
	}

	public int size(){
		int size = 0;
		try {
			RepositoryConnection con = getMetadataStore().getConnection();
			try {
				size = con.getContextIDs().asList().size();
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			LOGGER.severe("Error removing metadata entry: " + e.getMessage());
		}
		
		return size;
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
			metadata.shutDown();
			metadata.initialize();
		} catch (RepositoryException r) {
			LOGGER.severe(ERROR_INITIALIZE + r.getMessage());
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
