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
package eu.stratuslab.marketplace.server.utils;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.DCTERMS_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLREQ_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLTERMS_NS_URI;

public enum Schema {

    IDENTIFIER("identifier", DCTERMS_NS_URI, "string", "image identifier",
            "main"), //

    ISREPLACEDBY("isReplacedBy", DCTERMS_NS_URI, "string",
            "image identifier for replacement image", "main"), //

    REPLACES("replaces", DCTERMS_NS_URI, "string",
            "image identifier for image replaced by this one", "main"), //

    ISVERSIONOF("isVersionOf", DCTERMS_NS_URI, "string",
            "image identifier for parent image", "main"), //

    VALID("valid", DCTERMS_NS_URI, "string",
            "expiration date for image metadata", "main"), //

    TITLE("title", DCTERMS_NS_URI, "string", "short title for humans", "main"), //

    DESCRIPTION("description", DCTERMS_NS_URI, "string",
            "longer description of the image", "main"), //

    TYPE("type", DCTERMS_NS_URI, "string", "type of the described image",
            "main"), //

    CREATOR("creator", DCTERMS_NS_URI, "string",
            "name of image or metadata record creator", "main"), //

    CREATED("created", DCTERMS_NS_URI, "string",
            "date when metadata record was created", "main"), //

    PUBLISHER("publisher", DCTERMS_NS_URI, "string", "publisher of image",
            "main"), //

    FORMAT("format", DCTERMS_NS_URI, "string",
            "format of machine or disk image", "main"), //

    ENDORSEMENT("endorsement", SLREQ_NS_URI, "complex",
            "endorsement information", "main"), //

    ENDORSEMENT_CREATED("created", DCTERMS_NS_URI, "string",
            "date when endorsement was created", "endorsement"), //

    ENDORSER("endorser", SLREQ_NS_URI, "complex", "endorser information",
            "endorsement"), //

    BYTES("bytes", SLREQ_NS_URI, "positive integer",
            "bytes of described image", "main"), //

    CHECKSUM("checksum", SLREQ_NS_URI, "string",
            "checksum in hex with algorithm prefix", "main"), //

    EMAIL("email", SLREQ_NS_URI, "string",
            "email address of the metadata record creator", "endorser"), //

    SUBJECT("subject", SLREQ_NS_URI, "string", "certificate subject",
            "endorser"), //

    ISSUER("issuer", SLREQ_NS_URI, "complex", "certificate issuer", "endorser"), //

    LOCATION("location", SLTERMS_NS_URI, "URI",
            "location hint for download (none if unavailable)", "main"), //

    SERIALNUMBER("serial-number", SLTERMS_NS_URI, "non-negative integer",
            "numeric index of image within a series", "main"), //

    VERSION("version", SLTERMS_NS_URI, "string", "version of the image", "main"), //

    HYPERVISOR("hypervisor", SLTERMS_NS_URI, "string",
            "appropriate hypervisors for machine image", "main"), //

    INBOUND_PORT("inbound-port", SLTERMS_NS_URI, "unsigned short",
            "required inbound port", "main"), //

    OUTBOUND_PORT("outbound-port", SLTERMS_NS_URI, "unsigned short",
            "required outbound port", "main"), //

    ICMP("icmp", SLTERMS_NS_URI, "unsigned byte", "ICMP packet types", "main"), //

    OS_ARCH("os-arch", SLTERMS_NS_URI, "string", "OS architecture", "main"), //

    OS_VERSION("os-version", SLTERMS_NS_URI, "string", "OS version", "main"), //

    OS("os", SLTERMS_NS_URI, "string", "OS", "main"), //

    DEPRECATED("deprecated", SLTERMS_NS_URI, "string",
            "reason that image is deprecated (missing if OK)", "main");

    private static final String WHERE_TEMPLATE = "?%s <%s%s> ?%s .";

    private static final String FILTER_TEMPLATE = "FILTER REGEX(str(?%s), \"%s\", \"i\") .";

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

    public String getWhere() {
        return String.format(WHERE_TEMPLATE, node, ns, qname, qname);
    }

    public String getFilter(String value) {
        return String.format(FILTER_TEMPLATE, qname, value);
    }
}
