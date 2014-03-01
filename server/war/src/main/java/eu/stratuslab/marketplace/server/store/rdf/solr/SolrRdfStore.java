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
import static eu.stratuslab.marketplace.server.utils.XPathUtils.ALTERNATIVE;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.store.rdf.RdfStore;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;
import eu.stratuslab.marketplace.server.utils.XPathUtils;

public class SolrRdfStore extends RdfStore {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");

	private SolrServer solr;

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		String solrUrl = Configuration.getParameterValue(SOLR_URL);
		solr = new HttpSolrServer(solrUrl);
	}

	@Override
	public boolean store(String identifier, String entry) {
		Document rdfDoc = MetadataFileUtils.extractXmlDocument(entry);
		
		SolrInputDocument document = new SolrInputDocument();
		document.addField("id", identifier);
		document.addField("identifier", XPathUtils.getValue(rdfDoc, IDENTIFIER_ELEMENT));
		document.addField("email", XPathUtils.getValue(rdfDoc, EMAIL));
		document.addField("created", XPathUtils.getValue(rdfDoc, CREATED_DATE));
		document.addField("description", XPathUtils.getValue(rdfDoc, DESCRIPTION));
		document.addField("os", XPathUtils.getValue(rdfDoc, OS));
		document.addField("osversion", XPathUtils.getValue(rdfDoc, OS_VERSION));
		document.addField("osarch", XPathUtils.getValue(rdfDoc, OS_ARCH));
		document.addField("valid", XPathUtils.getValue(rdfDoc, VALID));
		document.addField("deprecated", XPathUtils.getValue(rdfDoc, DEPRECATED));
		document.addField("location", XPathUtils.getValue(rdfDoc, LOCATION));
		document.addField("alternative", XPathUtils.getValue(rdfDoc, ALTERNATIVE));
		
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

	@Override
	public void tag(String identifier, String tag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeTag(String identifier, String tag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Map<String, String>> getRdfEntriesAsMap(String query)
			throws MarketplaceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRdfEntriesAsXml(String query) throws MarketplaceException {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
