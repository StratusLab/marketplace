package eu.stratuslab.marketplace.metadata;

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.namespace.NamespaceContext;

@SuppressWarnings("restriction")
public class MetadataNamespaceContext implements NamespaceContext {

    public static final String MARKETPLACE_URI = "http://mp.stratuslab.eu/";

    public static final String RDF_NS_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final String DCTERMS_NS_URI = "http://purl.org/dc/terms/";

    public static final String SLTERMS_NS_URI = MARKETPLACE_URI + "slterms#";

    public static final String SLREQ_NS_URI = MARKETPLACE_URI + "slreq#";

    public static final String RDF_PREFIX = "rdf";

    public static final String DCTERMS_PREFIX = "dcterms";

    public static final String SLTERMS_PREFIX = "slterms";

    public static final String SLREQ_PREFIX = "slreq";

    public static final String XMLDSIG_PREFIX = "xmldsig";

    private static final Map<String, String> PREFIX_TO_NS = new HashMap<String, String>();

    private static final Map<String, String> NS_TO_PREFIX = new HashMap<String, String>();

    private static final MetadataNamespaceContext instance = new MetadataNamespaceContext();

    static {
        PREFIX_TO_NS.put(DEFAULT_NS_PREFIX, NULL_NS_URI);
        PREFIX_TO_NS.put(XML_NS_PREFIX, XML_NS_URI);
        PREFIX_TO_NS.put(XMLNS_ATTRIBUTE, XMLNS_ATTRIBUTE_NS_URI);
        PREFIX_TO_NS.put(RDF_PREFIX, RDF_NS_URI);
        PREFIX_TO_NS.put(DCTERMS_PREFIX, DCTERMS_NS_URI);
        PREFIX_TO_NS.put(SLTERMS_PREFIX, SLTERMS_NS_URI);
        PREFIX_TO_NS.put(SLREQ_PREFIX, SLREQ_NS_URI);
        PREFIX_TO_NS.put(XMLDSIG_PREFIX, XMLSignature.XMLNS);

        NS_TO_PREFIX.put(NULL_NS_URI, DEFAULT_NS_PREFIX);
        NS_TO_PREFIX.put(XML_NS_URI, XML_NS_PREFIX);
        NS_TO_PREFIX.put(XMLNS_ATTRIBUTE_NS_URI, XMLNS_ATTRIBUTE);
        NS_TO_PREFIX.put(RDF_NS_URI, RDF_PREFIX);
        NS_TO_PREFIX.put(DCTERMS_NS_URI, DCTERMS_PREFIX);
        NS_TO_PREFIX.put(SLTERMS_NS_URI, SLTERMS_PREFIX);
        NS_TO_PREFIX.put(SLREQ_NS_URI, SLREQ_PREFIX);
        NS_TO_PREFIX.put(XMLSignature.XMLNS, XMLDSIG_PREFIX);
    }

    private MetadataNamespaceContext() {

    }

    public static MetadataNamespaceContext getInstance() {
        return instance;
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        String value = PREFIX_TO_NS.get(prefix);
        return (value != null) ? value : NULL_NS_URI;
    }

    public String getPrefix(String nsUri) {
        if (nsUri == null) {
            throw new IllegalArgumentException();
        }
        return NS_TO_PREFIX.get(nsUri);
    }

    public Iterator<String> getPrefixes(String nsUri) {
        String prefix = getPrefix(nsUri);

        List<String> prefixes = new ArrayList<String>();
        if (prefix != null) {
            prefixes.add(prefix);
        }

        return Collections.unmodifiableList(prefixes).iterator();
    }

}
