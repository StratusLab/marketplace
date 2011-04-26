package eu.stratuslab.marketplace.server.utils;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.DCTERMS_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLTERMS_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLREQ_NS_URI;

public enum Schema {

	IDENTIFIER("identifier",DCTERMS_NS_URI,
			"string", "image identifier", "description"),
	ISREPLACEDBY("isReplacedBy",DCTERMS_NS_URI,
			"string", "image identifier for replacement image", "description"),
	REPLACES("replaces",DCTERMS_NS_URI,
			"string", "image identifier for image replaced by this one", "description"),
	ISVERSIONOF("isVersionOf",DCTERMS_NS_URI,
			"string", "image identifier for parent image", "description"),
	VALID("valid",DCTERMS_NS_URI,
			"string", "expiration date for image metadata", "description"),
	TITLE("title",DCTERMS_NS_URI,
			"string", "short title for humans", "description"),
	DESCRIPTION("description",DCTERMS_NS_URI,
			"string", "longer description of the image", "description"),
	TYPE("type",DCTERMS_NS_URI,
			"string", "type of the described image", "description"),
	CREATOR("creator",DCTERMS_NS_URI,
			"string", "name of image or metadata record creator", "description"),
	CREATED("created",DCTERMS_NS_URI,
			"string", "date when metadata record was created", "description"),
	PUBLISHER("publisher",DCTERMS_NS_URI,
			"string", "publisher of image", "description"),
	FORMAT("format",DCTERMS_NS_URI,
			"string", "format of machine or disk image", "description"),
	ENDORSEMENT("endorsement",SLREQ_NS_URI,
			"complex", "endorsement information", "description"),
	ENDORSER("endorser",SLREQ_NS_URI,
			"complex", "endorser information", "endorsement"),
	BYTES("bytes",SLREQ_NS_URI,
			"positive integer", "bytes of described image", "description"),
	CHECKSUM("checksum",SLREQ_NS_URI,
			"string", "checksum in hex with algorithm prefix", "description"),
	EMAIL("email",SLREQ_NS_URI,
			"string", "email address of the metadata record creator", "endorser"),
	SUBJECT("subject",SLREQ_NS_URI,
	    	"string", "certificate subject", "endorser"),
	ISSUER("issuer",SLREQ_NS_URI,
			"complex", "certificate issuer", "endorser"),
	LOCATION("location",SLTERMS_NS_URI,
			"URI", "location hint for download (none if unavailable)", "description"),
	SERIALNUMBER("serial-number",SLTERMS_NS_URI,
			"non-negative integer", "numeric index of image within a series", "description"),
	VERSION("version",SLTERMS_NS_URI,
			"string", "version of the image", "description"),
	HYPERVISOR("hypervisor",SLTERMS_NS_URI,
			"string", "appropriate hypervisors for machine image", "description"),
	INBOUND_PORT("inbound-port",SLTERMS_NS_URI,
			"unsigned short", "required inbound port", "description"),
	OUTBOUND_PORT("outbound-port",SLTERMS_NS_URI,
			"unsigned short", "required outbound port", "description"),
	ICMP("icmp",SLTERMS_NS_URI,
			"unsigned byte", "ICMP packet types", "description"),
	OS_ARCH("os-arch",SLTERMS_NS_URI,
			"string", "OS architecture", "description"),
	OS_VERSION("os-version",SLTERMS_NS_URI,
			"string", "OS version", "description"),
	OS("os",SLTERMS_NS_URI,
			"string", "OS", "description"),
	DEPRECATED("deprecated",SLTERMS_NS_URI,
			"string", "reason that image is deprecated (missing if OK)", "description");
	
	private final String qname;
	private final String ns;
	private final String notes;
	private final String xsd;
	private final String node;		
		
	private Schema(String qname, String ns, String xsd, String notes,
			String node) {
		this.qname = qname;
		this.ns = ns;
		this.xsd = xsd;
		this.node = node;
		this.notes = notes;
	}

	public String getQName() {
		return this.qname;
	}

	public String getNS() {
		return this.ns;
	}

	public String getXSD() {
		return this.xsd;
	}

	public String getNotes() {
		return this.notes;
	}

	public String getNode() {
		return this.node;
	}
	
	public String getWhere(){
		String where = "?" + this.node + " <" + this.ns + this.qname + ">" + " ?" + this.qname + " ."; 
		return where;
	}
	
	public String getFilter(String value){
		String filter = "FILTER (?" + this.qname + " = \"" + value + "\") .";
		return filter;
	}
}

