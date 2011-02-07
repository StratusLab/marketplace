package eu.stratuslab.marketplace.metadata;

import org.w3c.dom.Element;

/**
 * Static utilities to validate the content of an image metadata descriptor.
 * 
 * @author loomis
 * 
 */
public class MetadataContentValidation {

    public static final String RdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final String DctermsNS = "http://purl.org/dc/terms/";

    public static final String SltermsNS = "http://stratuslab.eu/terms#";

    public static final String MarketplaceURI = "http://mp.stratuslab.eu/";

    private MetadataContentValidation() {

    }

    public static void checkRootElementType(Element root) {

        if (root == null) {
            throw new MetadataException("root element cannot be null");
        }

        String ns = root.getNamespaceURI();
        if (!RdfNS.equals(ns)) {
            throw new MetadataException("root element must be in RDF namespace");
        }

        String name = root.getLocalName();
        if (!"RDF".equals(name)) {
            throw new MetadataException("root element must be rdf:RDF");
        }

        String xmlbase = root.getAttributeNS("xml", "base");
        if (!MarketplaceURI.equals(xmlbase)) {
            throw new MetadataException(
                    "root element must have xml:base attribute with value "
                            + MarketplaceURI);
        }

    }
}
