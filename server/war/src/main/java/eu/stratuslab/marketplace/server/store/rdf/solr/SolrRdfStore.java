package eu.stratuslab.marketplace.server.store.rdf.solr;

import static eu.stratuslab.marketplace.server.cfg.Parameter.SOLR_URL;

import static eu.stratuslab.marketplace.server.utils.XPathUtils.CREATED_DATE;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.EMAIL;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.IDENTIFIER_ELEMENT;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.DESCRIPTION;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.OS;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.OS_VERSION;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.OS_ARCH;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.DEPRECATED;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.LOCATION;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.VALID;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.KIND;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.ALTERNATIVE;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.TITLE;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.SUBJECT;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.ISSUER;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.servlet.SolrRequestParsers;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.query.SolrUtils;
import eu.stratuslab.marketplace.server.store.rdf.RdfStore;
import eu.stratuslab.marketplace.server.utils.MarketplaceUtils;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;
import eu.stratuslab.marketplace.server.utils.XPathUtils;

public class SolrRdfStore extends RdfStore {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");

	private SolrServer solr;

	private String solrUrl;

	@Override
	public void shutdown() {
		solr.shutdown();
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		solrUrl = Configuration.getParameterValue(SOLR_URL);
		solr = new HttpSolrServer(solrUrl);
	}

	@Override
	public boolean store(String identifier, String entry) {
		Document rdfDoc = MetadataFileUtils.extractXmlDocument(entry);
		
		SolrInputDocument document = new SolrInputDocument();
		document.addField("id", identifier);
		document.addField("identifier_ssi", XPathUtils.getValue(rdfDoc, IDENTIFIER_ELEMENT));
		document.addField("title_tesi", XPathUtils.getValue(rdfDoc, TITLE));
		document.addField("email_ssi", XPathUtils.getValue(rdfDoc, EMAIL));
		document.addField("created_dtsi", XPathUtils.getValue(rdfDoc, CREATED_DATE));
		document.addField("description_tesi", XPathUtils.getValue(rdfDoc, DESCRIPTION));
		document.addField("os_ssi", XPathUtils.getValue(rdfDoc, OS));
		document.addField("osversion_ssi", XPathUtils.getValue(rdfDoc, OS_VERSION));
		document.addField("arch_ssi", XPathUtils.getValue(rdfDoc, OS_ARCH));
		document.addField("valid_dtsi", XPathUtils.getValue(rdfDoc, VALID));
		document.addField("kind_ssi", XPathUtils.getValue(rdfDoc, KIND));
		document.addField("deprecated_tesi", XPathUtils.getValue(rdfDoc, DEPRECATED));
		document.addField("location_ssim", XPathUtils.getValue(rdfDoc, LOCATION));
		document.addField("alternative_ssi", XPathUtils.getValue(rdfDoc, ALTERNATIVE));
		document.addField("subject_ssi", XPathUtils.getValue(rdfDoc, SUBJECT));
		document.addField("issuer_ssi", XPathUtils.getValue(rdfDoc, ISSUER));
		
		return addToSolr(document);
	}

	@Override
	public void tag(String identifier, String tag) {
		SolrInputDocument document = new SolrInputDocument();
		Map<String, String> partialUpdate = new HashMap<String, String>();
		partialUpdate.put("set", tag);
		document.addField("id", identifier);
		document.addField("tag_ssi", partialUpdate);
		
		addToSolr(document);
	}

	@Override
	public void removeTag(String identifier, String tag) {
		SolrQuery query = new SolrQuery();
	    query.setQuery( "id:\"" + identifier + "\"" );
	    
	    QueryResponse rsp;
		try {
			rsp = solr.query( query );
			
			SolrDocumentList docs = rsp.getResults();
			
			if (docs.size() > 0) {
				SolrDocument document = docs.get(0);
				
				if (document.containsKey("tag_ssi")) {
					remove(identifier);
					
					document.remove("tag_ssi");
					SolrInputDocument input = ClientUtils.
							toSolrInputDocument(document);
					addToSolr(input);
				}
				
 			}
		} catch (SolrServerException e) {
			LOGGER.severe("Unable to clear metadata entry: " + e.getMessage());
		}    
	    
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getRdfEntriesAsMap(String query)
			throws MarketplaceException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		
		boolean countQuery = false;
		boolean groupQuery = false;
		
		SolrParams sQuery = SolrRequestParsers.parseQueryString(query);
		
		String rows = sQuery.get("rows");
		if (rows != null && rows.equals("0")){
			countQuery = true;
		}
		
		String group = sQuery.get("group");
		if (group != null && group.equals("true")){
			groupQuery = true;
		}
		
		QueryResponse rsp;
		try {
			rsp = solr.query( sQuery );

			SolrDocumentList docs = rsp.getResults();
			
			if (countQuery) {
				long count = docs.getNumFound();
				
				HashMap<String, String> row = new HashMap<String, String>(
						1, 1);
				row.put("count", Long.toString(count));
				list.add(row);
			} else if (groupQuery) {
				NamedList<Object> respNL = rsp.getResponse(); 
				NamedList<Object> groupInfo = (NamedList<Object>) respNL.get("grouped"); 
				NamedList<Object> thisGroupInfo = (NamedList<Object>) groupInfo.get(sQuery.get(GroupParams.GROUP_FIELD)); 
				List<Object> groupData = (List<Object>) thisGroupInfo.get("groups"); 
				for(Object o : groupData) { 
					NamedList<Object> thisGroup = (NamedList<Object>) o; 
					SolrDocumentList sdl = (SolrDocumentList) thisGroup.get("doclist");
					SolrDocument document = sdl.get(0); 
					
				    String groupValue = (String) thisGroup.get("groupValue"); 
				    
				    HashMap<String, String> row = new HashMap<String, String>();
					row.put(SolrUtils.getResultColumn(sQuery.get(GroupParams.GROUP_FIELD)), groupValue);
					
					Set<Entry<String, Object>> columns = document.entrySet();
					
					for (Entry<String, Object> column : columns) {
						String columnName = column.getKey();
						Object columnValue = column.getValue();
						
						if ((columnValue != null)) {
							String stringValue = "";
							if (columnValue instanceof Date) {
								stringValue = MarketplaceUtils.getFormattedDate((Date)columnValue);
							} else if (columnValue instanceof String) {
								stringValue = (String)columnValue;

							} else if (columnValue instanceof ArrayList) {
								StringBuilder builder = new StringBuilder();

								for (String entry : (ArrayList<String>)columnValue) {
									if (!entry.equals(""))
										builder.append(entry + ", ");
									else 
										builder.append("null");
								}

								stringValue = builder.toString();
							}
							stringValue = stringValue.trim().replaceAll(",$", "");
							row.put(SolrUtils.getResultColumn(columnName), stringValue);
						} else {
							row.put(SolrUtils.getResultColumn(columnName), "null");
						}	
					}
					
					list.add(row);
				} 
			} else {

				for (SolrDocument document : docs) {
					Set<Entry<String, Object>> columns = document.entrySet();
					HashMap<String, String> row = new HashMap<String, String>(
							columns.size(), 1);

					for (Entry<String, Object> column : columns) {
						String columnName = column.getKey();
						Object columnValue = column.getValue();
						if ((columnValue != null)) {
							String stringValue = "";
							if (columnValue instanceof Date) {
								stringValue = MarketplaceUtils.getFormattedDate((Date)columnValue);
							} else if (columnValue instanceof String) {
								stringValue = (String)columnValue;

							} else if (columnValue instanceof ArrayList) {
								StringBuilder builder = new StringBuilder();

								for (String value : (ArrayList<String>)columnValue) {
									if (!value.equals(""))
										builder.append(value + ", ");
									else 
										builder.append("null");
								}

								stringValue = builder.toString();
							}
							stringValue = stringValue.trim().replaceAll(",$", "");
							row.put(SolrUtils.getResultColumn(columnName), stringValue);
						} else {
							row.put(SolrUtils.getResultColumn(columnName), "null");
						}
					}
					list.add(row);	
				}
			}
			
		} catch (SolrServerException e) {
			throw new MarketplaceException(e.getMessage(), e);
		}
			
		return list;	
	}

	@Override
	public String getRdfEntriesAsXml(String query) throws MarketplaceException {
		URL url;
		String xmlResponse = "";
		
		try {
			url = new URL(solrUrl + "/select?" + query);
			xmlResponse = IOUtils.toString(url);
		} catch (IOException e) {
			throw new MarketplaceException(e.getMessage(), e);
		} 
		
		return xmlResponse;
	}

	@Override
	public String getRdfEntriesAsJson(String query) throws MarketplaceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRdfEntry(String uri) throws MarketplaceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(String identifier) {
		try {
			solr.deleteById(identifier);
			solr.commit();
		} catch (SolrServerException e) {
			LOGGER.severe("Error removing metadata entry: " + e.getMessage());
		} catch (IOException e) {
			LOGGER.severe("Error removing metadata entry: " + e.getMessage());
		}
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private boolean addToSolr(SolrInputDocument document) {
		try {
			solr.add(document);
			solr.commit();
		} catch (SolrServerException e) {
			LOGGER.severe("Error storing metadata entry: " + e.getMessage());
			return false;
		} catch (IOException e) {
			LOGGER.severe("Error storing metadata entry: " + e.getMessage());
			return false;
		}
		
		return true;
	}

}
