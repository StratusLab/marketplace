package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.DCTERMS_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.RDF_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLREQ_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLTERMS_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.getInstance;
import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import javax.xml.crypto.dsig.XMLSignature;

import org.junit.Test;

@SuppressWarnings("restriction")
public class MetadataNamespaceContextTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullPrefixThrowsException() {
        MetadataNamespaceContext context = getInstance();
        context.getNamespaceURI(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullNsUriThrowsException() {
        MetadataNamespaceContext context = getInstance();
        context.getPrefix(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPrefixesThrowsException() {
        MetadataNamespaceContext context = getInstance();
        context.getPrefixes(null);
    }

    @Test
    public void defaultNsOK() {
        MetadataNamespaceContext context = getInstance();
        String nsUri = context.getNamespaceURI(DEFAULT_NS_PREFIX);
        String nsPrefix = context.getPrefix(NULL_NS_URI);

        assertEquals(nsUri, NULL_NS_URI);
        assertEquals(nsPrefix, DEFAULT_NS_PREFIX);
    }

    @Test
    public void xmlMapOK() {
        MetadataNamespaceContext context = getInstance();
        String nsUri = context.getNamespaceURI(XML_NS_PREFIX);
        String nsPrefix = context.getPrefix(XML_NS_URI);

        assertEquals(nsUri, XML_NS_URI);
        assertEquals(nsPrefix, XML_NS_PREFIX);
    }

    @Test
    public void xmlnsMapOK() {
        MetadataNamespaceContext context = getInstance();
        String nsUri = context.getNamespaceURI(XMLNS_ATTRIBUTE);
        String nsPrefix = context.getPrefix(XMLNS_ATTRIBUTE_NS_URI);

        assertEquals(nsUri, XMLNS_ATTRIBUTE_NS_URI);
        assertEquals(nsPrefix, XMLNS_ATTRIBUTE);
    }

    @Test
    public void allPrefixListsOK() {
        MetadataNamespaceContext context = getInstance();
        String[] uris = { RDF_NS_URI, DCTERMS_NS_URI, SLTERMS_NS_URI,
                SLREQ_NS_URI, NULL_NS_URI, XML_NS_URI, XMLNS_ATTRIBUTE_NS_URI,
                XMLSignature.XMLNS };

        for (String uri : uris) {
            String p1 = context.getPrefix(uri);
            String p2 = "";

            Iterator<String> iterator = context.getPrefixes(uri);
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                p2 = iterator.next();
            }

            assertEquals(count, 1);
            assertEquals(p1, p2);
        }
    }

}
