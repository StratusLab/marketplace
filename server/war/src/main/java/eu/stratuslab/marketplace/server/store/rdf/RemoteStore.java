package eu.stratuslab.marketplace.server.store.rdf;

import static eu.stratuslab.marketplace.server.cfg.Parameter.MASTER_URL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParser;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;

public class RemoteStore extends RdfStore {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private URI master = null;
	
	public RemoteStore() {
		try {
			master = new URI(Configuration.getParameterValue(MASTER_URL));
		} catch (URISyntaxException e) {
			LOGGER.severe("Incorrect master url configured: " 
					+ Configuration.getParameterValue(MASTER_URL));
		}
	}
	
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean store(String identifier, String entry) {
		return false;
	}

	public String getRdfEntry(String iri) throws MarketplaceException {
		ClientResource metadatumResource = new ClientResource(master 
				+ "/metadata/" + iri);
		
		String metadata = null;
		try {
			metadata = get(metadatumResource, MediaType.APPLICATION_RDF_XML);
		} catch (IOException e) {
			throw new MarketplaceException(e.getMessage(), e);
		}
		metadatumResource.release();
		
		return metadata;
	}
	
	@Override
	public List<Map<String, String>> getRdfEntriesAsMap(String query)
			throws MarketplaceException {
		List<Map<String, String>> results = null;
		
		ClientResource queryResource = null;
		
		try {
			String escapedQuery = URIUtil.encodeQuery(query);
			
			queryResource = new ClientResource(master
				+ "/query?query=" + escapedQuery);
	 		
			String resultXml = get(queryResource, MediaType.APPLICATION_RDF_XML);
			
			TupleQueryResult tuples = stringToTupleQueryResult(resultXml);
			
			results = tuplesToMap(tuples);
						
			queryResource.release();
			
		} catch (URIException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch(IOException e){
			throw new MarketplaceException(e.getMessage(), e);
		} finally {
			if(queryResource != null){
				queryResource.release();
			}
		}
			
		return results;
	}

	@Override
	public String getRdfEntriesAsXml(String query) throws MarketplaceException {
		String results = null;
		
		ClientResource queryResource = null;
		
		try {
			String escapedQuery = URIUtil.encodeQuery(query);
			
			queryResource = new ClientResource(master
				+ "/query?query=" + escapedQuery);
	 		
			results = get(queryResource, MediaType.APPLICATION_RDF_XML);
			
			queryResource.release();
			
		} catch (URIException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch(IOException e){
			throw new MarketplaceException(e.getMessage(), e);
		} finally {
			if(queryResource != null){
				queryResource.release();
			}
		}
			
		return results;
	}

	@Override
	public String getRdfEntriesAsJson(String query) throws MarketplaceException {
		String results = null;
		
		ClientResource queryResource = null;
		
		try {
			String escapedQuery = URIUtil.encodeQuery(query);
			
			queryResource = new ClientResource(master
				+ "/query?query=" + escapedQuery);
	 		
			results = get(queryResource, MediaType.APPLICATION_JSON);
			
			queryResource.release();
			
		} catch (URIException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} catch(IOException e){
			throw new MarketplaceException(e.getMessage(), e);
		} finally {
			if(queryResource != null){
				queryResource.release();
			}
		}
			
		return results;
	}

	@Override
	public void remove(String identifier) {
	}

	/**
     * Prints the resource's representation.
     * 
     * @param clientResource
     *            The Restlet client resource.
     * @throws IOException
     * @throws ResourceException
     */
    private static String get(ClientResource clientResource, MediaType type) throws IOException {
        String metadata = null;
    	
        try {
        	clientResource.get(type);
        	if (clientResource.getStatus().isSuccess()
        			&& clientResource.getResponseEntity().isAvailable()) {
        		Representation response = clientResource.getResponseEntity();
        		metadata = response.getText();
        		response.release();
        	}
        } catch (ResourceException e) {
        	LOGGER.severe("Error  status: " + e.getStatus());
        	LOGGER.severe("Error message: " + e.getMessage());
        }
                
        return metadata;
    }
    
    private static TupleQueryResult stringToTupleQueryResult(String results) {
		SPARQLResultsXMLParser xmlRes = new SPARQLResultsXMLParser();
		TupleQueryResultBuilder build = new TupleQueryResultBuilder();
		xmlRes.setTupleQueryResultHandler(build);
		
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(results.getBytes("UTF-8"));
		
			xmlRes.parse(is);
		} catch (UnsupportedEncodingException e) {
			
		} catch (QueryResultParseException e) {
			
		} catch (TupleQueryResultHandlerException e) {
			
		} catch (IOException e) {
			
		}

		TupleQueryResult tupleRes = build.getQueryResult();
		
		return tupleRes;
	}
	
	private static List<Map<String, String>> tuplesToMap(TupleQueryResult tuples) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		List<String> columnNames = tuples.getBindingNames();
		int cols = columnNames.size();

		try {
			while (tuples.hasNext()) {
				BindingSet solution = tuples.next();
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
			
			tuples.close();
		} catch (QueryEvaluationException e) {
			LOGGER.severe("Error evaluating query: " + e.getMessage());
		}

		return list;
	}
}
