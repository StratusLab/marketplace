package eu.stratuslab.marketplace.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import eu.stratuslab.marketplace.metadata.CreateMetadata.MetadataFields;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CreateChecksums {

    final static private long SixMonths = 6L * 30L * 24L * 60L * 60L * 1000L;

    final static private SimpleDateFormat xmlDateTimeFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssZ");

    public enum Namespace {
        RDF("http://www.w3.org/1999/02/22-rdf-syntax-ns#"), DCTERMS(
                "http://purl.org/dc/terms/"), SLTERMS(
                "http://stratuslab.eu/terms#");

        final public String url;
        final public String abbrev;

        private Namespace(String url) {
            this.url = url;
            abbrev = name().toLowerCase();
        }
    }

    public static String documentToString(Document doc)
            throws ParserConfigurationException, TransformerException {

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute("indent-number", Integer.valueOf(4));

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");

        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();

    }

    public static Document createSkeletonRDF(File imageFile,
            Map<MetadataFields, String> userInput)
            throws ParserConfigurationException, IOException {

        FileInputStream fis = new FileInputStream(imageFile);

        Map<String, BigInteger> checksums = MetadataUtils
                .streamInfo(fis);

        String uuid = UUID.randomUUID().toString();
        String identifier = MetadataUtils.sha1ToIdentifier(checksums.get("SHA-1"));

        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        dbfac.setNamespaceAware(true);

        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element root = createRootElement(doc);

        root.appendChild(createMetadataElement(checksums, uuid, identifier,
                doc, userInput));

        root.appendChild(createEndorserElement(uuid, doc, userInput));

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
            Map<String, BigInteger> checksums, String uuid, String identifier,
            Document doc, Map<MetadataFields, String> userInput) {

        Element metadata = createRdfElement(doc, "Description");

        createAttr(Namespace.RDF, doc, metadata, "refID", uuid);

        createAttr(Namespace.RDF, doc, metadata, "about", "#" + identifier);

        dctermsElement(doc, metadata, "identifier", identifier);

        sltermsElement(doc, metadata, "bytes", checksums.get("BYTES")
                .toString());

        for (Map.Entry<String, BigInteger> entry : checksums.entrySet()) {
            if (!"BYTES".equals(entry.getKey())) {
                sltermsElement(doc, metadata, "checksum", entry.getKey() + ":"
                        + entry.getValue().toString(16));
            }
        }

        dctermsElement(doc, metadata, "valid", formattedTime(SixMonths));

        for (Map.Entry<MetadataFields, String> entry : userInput.entrySet()) {
            MetadataFields field = entry.getKey();
            if (field.isMetadataDesc) {
                createElement(field.ns, doc, metadata, field.tag, entry.getValue());
            }
        }

        return metadata;
    }

    private static Element createEndorserElement(String uuid, Document doc,
            Map<MetadataFields, String> userInput) {

        Element endorser = createRdfElement(doc, "Description");
        createAttr(Namespace.RDF, doc, endorser, "about", uuid);

        dctermsElement(doc, endorser, "created", formattedTime(0L));

        Element creator = dctermsElement(doc, endorser, "creator", null);

        for (Map.Entry<MetadataFields, String> entry : userInput.entrySet()) {
            MetadataFields field = entry.getKey();
            if (!field.isMetadataDesc) {
                createElement(field.ns, doc, creator, field.tag, entry.getValue());
            }
        }

        return endorser;
    }

    private static String formattedTime(long offset) {
        Date time = new Date(System.currentTimeMillis() + offset);
        return xmlDateTimeFormat.format(time);
    }

    public static Attr createAttr(Namespace ns, Document doc, Element element,
            String qname, String value) {

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

    public static Element dctermsElement(Document doc, Element parent,
            String qname, String text) {

        return createElement(Namespace.DCTERMS, doc, parent, qname, text);
    }

    public static Element sltermsElement(Document doc, Element parent,
            String qname, String text) {

        return createElement(Namespace.SLTERMS, doc, parent, qname, text);
    }

    public static Element createElement(Namespace ns, Document doc,
            Element parent, String qname, String text) {

        Element element = doc.createElementNS(ns.url, qname);
        element.setPrefix(ns.abbrev);

        if (text != null) {
            element.setTextContent(text);
        }

        parent.appendChild(element);

        return element;

    }

}
