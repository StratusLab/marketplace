package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataUtils.sha1ToIdentifier;
import static eu.stratuslab.marketplace.metadata.MetadataUtils.streamInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.metadata.CreateMetadata.MetadataFields;

public class CreateChecksums {

    final static private long SixMonths = 6L * 30L * 24L * 60L * 60L * 1000L;

    final static private SimpleDateFormat xmlDateTimeFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssZ");
    static {
        xmlDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public enum Namespace {
        RDF("http://www.w3.org/1999/02/22-rdf-syntax-ns#"), DCTERMS(
                "http://purl.org/dc/terms/"), SLTERMS(
                "http://stratuslab.eu/terms#"), SLREQ(
                "http://mp.stratuslab.eu/slreq#");

        final public String url;
        final public String abbrev;

        private Namespace(String url) {
            this.url = url;
            abbrev = name().toLowerCase();
        }
    }

    public static Document createSkeletonRDF(File imageFile,
            Map<MetadataFields, String> userInput)
            throws ParserConfigurationException, IOException {

        FileInputStream fis = new FileInputStream(imageFile);

        Map<String, BigInteger> checksums = streamInfo(fis);

        String identifier = sha1ToIdentifier(checksums.get("SHA-1"));

        Document doc = XMLUtils.newDocument();

        Element root = createRootElement(doc);

        root.appendChild(createMetadataElement(checksums, identifier, doc,
                userInput));

        root.appendChild(createEndorsement(doc, userInput));

        return doc;

    }

    private static Element createRootElement(Document doc) {
        Element root = createRdfElement(doc, "RDF");
        doc.appendChild(root);

        for (Namespace ns : Namespace.values()) {
            Attr nsmap = doc.createAttribute("xmlns:" + ns.abbrev);
            nsmap.setValue(ns.url);
            root.setAttributeNode(nsmap);
        }

        return root;
    }

    private static Element createMetadataElement(
            Map<String, BigInteger> checksums, String identifier, Document doc,
            Map<MetadataFields, String> userInput) {

        Element metadata = createRdfElement(doc, "Description");

        createAttr(metadata, Namespace.RDF, "refID", "dummy");

        createAttr(metadata, Namespace.RDF, "about", "#" + identifier);

        dctermsElement(metadata, "identifier", identifier);

        sltermsElement(metadata, "bytes", checksums.get("BYTES").toString());

        for (Map.Entry<String, BigInteger> entry : checksums.entrySet()) {
            if (!"BYTES".equals(entry.getKey())) {
                sltermsElement(metadata, "checksum", entry.getKey() + ":"
                        + entry.getValue().toString(16));
            }
        }

        dctermsElement(metadata, "valid", formattedTime(SixMonths));

        for (Map.Entry<MetadataFields, String> entry : userInput.entrySet()) {
            MetadataFields field = entry.getKey();
            if (field.isMetadataDesc) {
                createElement(field.ns, metadata, field.tag, entry.getValue());
            }
        }

        return metadata;
    }

    private static Element createEndorsement(Document doc,
            Map<MetadataFields, String> userInput) {

        Element endorsement = doc.createElementNS(Namespace.SLREQ.url,
                "endorsement");
        createAttr(endorsement, Namespace.RDF, "parseType", "Resource");

        dctermsElement(endorsement, "created", formattedTime(0L));

        Element endorser = dctermsElement(endorsement, "creator", null);
        createAttr(endorser, Namespace.RDF, "parseType", "Resource");

        for (Map.Entry<MetadataFields, String> entry : userInput.entrySet()) {
            MetadataFields field = entry.getKey();
            if (!field.isMetadataDesc) {
                createElement(field.ns, endorser, field.tag, entry.getValue());
            }
        }

        return endorsement;
    }

    private static String formattedTime(long offset) {
        Date time = new Date(System.currentTimeMillis() + offset);
        return xmlDateTimeFormat.format(time);
    }

    public static Attr createAttr(Element element, Namespace ns, String qname,
            String value) {

        Document doc = element.getOwnerDocument();

        Attr attr = doc.createAttributeNS(ns.url, qname);
        attr.setPrefix(ns.abbrev);
        attr.setValue(value);
        element.setAttributeNodeNS(attr);

        return attr;
    }

    private static Element createRdfElement(Document doc, String qname) {

        Element element = doc.createElementNS(Namespace.RDF.url, qname);
        element.setPrefix(Namespace.RDF.abbrev);

        return element;
    }

    public static Element dctermsElement(Element parent, String qname,
            String text) {

        return createElement(Namespace.DCTERMS, parent, qname, text);
    }

    public static Element sltermsElement(Element parent, String qname,
            String text) {

        return createElement(Namespace.SLTERMS, parent, qname, text);
    }

    public static Element slreqElement(Element parent, String qname, String text) {

        return createElement(Namespace.SLREQ, parent, qname, text);
    }

    public static Element createElement(Namespace ns, Element parent,
            String qname, String text) {

        Document doc = parent.getOwnerDocument();

        Element element = doc.createElementNS(ns.url, qname);
        element.setPrefix(ns.abbrev);

        if (text != null) {
            element.setTextContent(text);
        }

        parent.appendChild(element);

        return element;

    }

}
