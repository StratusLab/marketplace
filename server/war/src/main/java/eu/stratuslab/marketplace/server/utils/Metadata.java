package eu.stratuslab.marketplace.server.utils;

import static eu.stratuslab.marketplace.server.utils.XPathUtils.CREATED_DATE;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.EMAIL;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.IDENTIFIER_ELEMENT;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.OS;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.OS_VERSION;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.DESCRIPTION;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.LOCATION;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.TITLE;
import static eu.stratuslab.marketplace.server.utils.XPathUtils.VALID;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;

public class Metadata {
	
	private Document document;
	private String metadata;
	
	private String identifier;
	private String email;
	private String created;
	private String os;
	private String osVersion;
	private String description;
	private String location;

	private String title;

	private String valid;
	
	public Metadata(String metadata) {
	    this.metadata = metadata;
		document  = MetadataFileUtils.extractXmlDocument(metadata);
		setFields();
	}
	
	public Metadata(Document document){
		this.document = document;
		metadata = XMLUtils.documentToString(document);
		setFields();
	}
	
	private void setFields() {
		identifier = XPathUtils.getValue(document, IDENTIFIER_ELEMENT);
		email = XPathUtils.getValue(document, EMAIL);
		created = XPathUtils.getValue(document, CREATED_DATE);
		os = XPathUtils.getValue(document, OS);
		osVersion = XPathUtils.getValue(document, OS_VERSION);
		description = XPathUtils.getValue(document, DESCRIPTION);
		location = XPathUtils.getValue(document, LOCATION);
		title = XPathUtils.getValue(document, TITLE);
		valid = XPathUtils.getValue(document, VALID);
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getIdentifier(){
		return identifier;
	}
	
	public String getEmail(){
		return email;
	}
	
	public String getCreated(){
		return created;
	}
	
	public String getOS(){
		return os;
	}
	
	public String getOSVersion(){
		return osVersion;
	}
	
	public String getDescription(){
		return description;
	}
	
	public String getLocation(){
		return location;
	}
	
	public String getValid(){
		return valid;
	}
	
	public JSONObject toJson(){
		HashMap<String, String> metadata = new HashMap<String, String>();
		metadata.put("identifier", identifier);
		metadata.put("title", title);
		metadata.put("description", description);
		metadata.put("email", email);
		metadata.put("created", created);
		metadata.put("os", os);
		metadata.put("os_version", osVersion);
		metadata.put("valid", valid);
		metadata.put("location", location);
		
		return new JSONObject(metadata);
	}
	
	public String toXml(){
		return metadata;
	}
	
	public Document toDocument(){
		return document;
	}

}
