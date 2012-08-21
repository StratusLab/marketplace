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
package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.server.utils.XPathUtils.CREATED_DATE;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.EMAIL;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.IDENTIFIER_ELEMENT;
import static org.restlet.data.MediaType.TEXT_PLAIN;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.metadata.MetadataUtils;
import eu.stratuslab.marketplace.server.MarketPlaceApplication;
import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.store.FileStore;
import eu.stratuslab.marketplace.server.store.RdfStore;
import eu.stratuslab.marketplace.server.utils.EndorserWhitelist;
import eu.stratuslab.marketplace.server.utils.XPathUtils;

/**

 *  Base resource class that supports common behaviours or attributes shared by
 *  all resources.
 */
/**
 * @author Stuart Kenny
 * 
 */
public abstract class BaseResource extends ServerResource {

	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	protected static final Logger LOGGER = Logger.getLogger("org.restlet");

	protected static final int ARG_EMAIL = 1;
	protected static final int ARG_DATE = 2;
	protected static final int ARG_OTHER = 3;

	protected static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
	protected static final String NO_TITLE = null;
	
	protected RdfStore getMetadataRdfStore() {
		return ((MarketPlaceApplication) getApplication()).getMetadataRdfStore();
	}
	
	protected FileStore getMetadataFileStore() {
		return ((MarketPlaceApplication) getApplication()).getMetadataFileStore();
	}

	protected String getDataDir() {
		return ((MarketPlaceApplication) getApplication()).getDataDir();
	}

	protected EndorserWhitelist getWhitelist() {
		return ((MarketPlaceApplication) getApplication()).getWhitelist();
	}

	protected freemarker.template.Configuration getFreeMarkerConfiguration() {
		return ((MarketPlaceApplication) getApplication())
				.getFreeMarkerConfiguration();
	}

	protected TemplateRepresentation createTemplateRepresentation(String tpl,
			Map<String, Object> info, MediaType mediaType) {

		freemarker.template.Configuration freeMarkerConfig = getFreeMarkerConfiguration();

		return new TemplateRepresentation(tpl, freeMarkerConfig, info,
				mediaType);
	}

	protected Map<String, Object> createInfoStructure(String title) {

		Map<String, Object> info = new HashMap<String, Object>();

		// Add the standard base URL declaration.
		info.put("baseurl", getRequest().getRootRef().toString());

		// Add the title if appropriate.
		if (title != null && !"".equals(title)) {
			info.put("title", title);
		}

		return info;
	}

	protected String commitMetadataEntry(File uploadedFile, Document doc) {
		writeMetadataToFileStore(doc);
		
		String metadataPath = null;
		try {
			metadataPath = writeMetadataToRdfStore(doc);
		} catch (ResourceException e) {
			// transaction has failed, so rollback
			deleteMetadataFromFileStore(doc);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}

		if (!uploadedFile.delete()) {
			LOGGER.severe("uploaded file could not be deleted: " + uploadedFile);
		}

		return metadataPath;
	}
	
	protected void writeMetadataToFileStore(Document doc) {

		String[] coordinates = getMetadataEntryCoordinates(doc);

		String identifier = coordinates[0];
		String endorser = coordinates[1];
		String created = coordinates[2];

		String key = 
			identifier + File.separator 
		+ endorser + File.separator 
		+ created;
		
		getMetadataFileStore().store(key, doc);
	}

	protected void deleteMetadataFromFileStore(Document doc) {

		String[] coordinates = getMetadataEntryCoordinates(doc);

		String identifier = coordinates[0];
		String endorser = coordinates[1];
		String created = coordinates[2];

		String key = identifier + File.separator + endorser
				+ File.separator + created;

		getMetadataFileStore().remove(key);
	}
	
	// Create a deep copy of the document and strip signature elements.
	protected String createRdfEntry(Document doc) {
		Document copy = (Document) doc.cloneNode(true);
		MetadataUtils.stripSignatureElements(copy);
		String rdfEntry = XMLUtils.documentToString(copy);
		String[] coords = getMetadataEntryCoordinates(copy);
		rdfEntry = rdfEntry.replaceFirst("<rdf:Description rdf:about=\"#"
				+ coords[0] + "\">", "<rdf:Description rdf:about=\"#"
				+ coords[0] + "/" + coords[1] + "/" + coords[2] + "\">");
		return rdfEntry;
	}

	protected String[] getMetadataEntryCoordinates(Document doc) {

		String[] coords = new String[3];

		coords[0] = XPathUtils.getValue(doc, IDENTIFIER_ELEMENT);
		coords[1] = XPathUtils.getValue(doc, EMAIL);
		coords[2] = XPathUtils.getValue(doc, CREATED_DATE);

		return coords;
	}

	protected String writeMetadataToRdfStore(Document datumDoc) {

		String[] coordinates = getMetadataEntryCoordinates(datumDoc);

		String identifier = coordinates[0];
		String endorser = coordinates[1];
		String created = coordinates[2];

		String metadataPath = identifier + "/" + endorser + "/" + created;

		String rdfEntry = createRdfEntry(datumDoc);
		if (!storeMetadatum(metadataPath, rdfEntry)) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}

		return metadataPath;
	}

	/**
	 * Stores a new metadata entry
	 * 
	 * @param metadataPath
	 *            identifier of the metadata entry
	 * @param rdf
	 *            the metadata entry to store
	 */
	protected boolean storeMetadatum(String metadataPath, String rdf) {
		boolean success = getMetadataRdfStore().store(metadataPath, rdf);

		return success;
	}

	/**
	 * Retrieve a particular metadata entry
	 * 
	 * @param metadataPath
	 *            identifier of the metadata entry
	 * @return metadata entry
	 */
	protected String getMetadatum(String metadataPath) {
		String model = getMetadataFileStore().read(metadataPath);

		return model;
	}

	/**
	 * Remove a metadata entry
	 * 
	 * @param iri
	 *            identifier of the metadata entry
	 */
	protected void removeMetadatum(String metadataPath) {
		getMetadataRdfStore().remove(metadataPath);
	}

	protected String queryResultsAsXml(String queryString)
			throws MarketplaceException {
		String resultString = getMetadataRdfStore()
				.getRdfEntriesAsXml(queryString);

		return resultString;
	}

	protected String queryResultsAsJson(String queryString)
			throws MarketplaceException {
		String resultString = getMetadataRdfStore().getRdfEntriesAsJson(
				queryString);

		return resultString;
	}

	/**
	 * Query the metadata
	 * 
	 * @param queryString
	 *            the query
	 * @return the resultset as a Java Collection
	 */
	protected List<Map<String, String>> query(String queryString)
			throws MarketplaceException {
		List<Map<String, String>> list = getMetadataRdfStore().getRdfEntriesAsMap(
				queryString);

		return list;
	}
	
	protected Representation createStatusRepresentation(String title,
			String message) {
		Representation status = null;
		if (getRequest().getClientInfo().getAcceptedMediaTypes().size() > 0
			&& getRequest().getClientInfo().getAcceptedMediaTypes()
				.get(0).getMetadata().equals(MediaType.TEXT_HTML)) {
			Map<String, Object> dataModel = createInfoStructure(title);
			dataModel.put("statusName", getResponse().getStatus().getReasonPhrase());
			dataModel.put("statusDescription", message);
			status = createTemplateRepresentation("status.ftl", dataModel,
				MediaType.TEXT_HTML);
		} else {
			status = new StringRepresentation(message, TEXT_PLAIN);
		}

		return status;
	}

	protected String getCurrentDate() {
		return getFormattedDate(new Date());
	}

	protected String getFormattedDate(Date date){
		return getDateFormat().format(date);
	}
	
	private DateFormat getDateFormat() {
		SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
		format.setLenient(false);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));

		return format;
	}

}
