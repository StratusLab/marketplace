package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataContentValidation.MarketplaceURI;
import static eu.stratuslab.marketplace.metadata.MetadataContentValidation.RdfNS;
import static javax.xml.XMLConstants.XML_NS_URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MetadataContentValidationTest {

    private static final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
            .newInstance();
    static {
        docBuilderFactory.setNamespaceAware(true);
    }

    @Test(expected = MetadataException.class)
    public void nullRootFails() {
        MetadataContentValidation.checkStructure(createEmptyRdfElement());
    }

    private static Document createEmptyXmlDocument() {
        try {
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            return docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Element createEmptyRdfElement() {
        Document doc = createEmptyXmlDocument();
        Element root = doc.createElementNS(RdfNS, "RDF");
        root.setAttributeNS(XML_NS_URI, "base", MarketplaceURI);
        doc.appendChild(root);
        return root;
    }

    private static Element appendNewChild(Element element, String ns,
            String name) {
        Element child = element.getOwnerDocument().createElementNS(ns, name);
        element.appendChild(child);
        return child;
    }
}
