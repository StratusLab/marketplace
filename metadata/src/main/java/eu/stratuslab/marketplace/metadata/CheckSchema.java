package eu.stratuslab.marketplace.metadata;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class CheckSchema {

    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    static final String schemaSource = "src/main/resources/eu/stratuslab/marketplace/metadata/image-metadata.xsd";

    static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    public static void main(String[] args) throws ParserConfigurationException,
            SAXException, IOException, TransformerException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);

        try {
            factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        } catch (IllegalArgumentException consumed) {
            // No support for JAXP 1.2
        }

        factory.setAttribute(JAXP_SCHEMA_SOURCE, new File(schemaSource));

        DocumentBuilder db = factory.newDocumentBuilder();

        Document doc = db.parse("ttylinux-9.7-i486-base-1.0.xml");

        System.out.println("Document has been validated.");

        String contents = CreateChecksums.documentToString(doc);

        System.err.println(contents);

    }
}
