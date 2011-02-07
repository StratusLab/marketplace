package eu.stratuslab.marketplace.metadata;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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

    private static final XPathQuery[] XPATH_CHECKS = {

            new XPathQuery("count(//rdf:RDF)", "1",
                    "description must be a single rdf:RDF element"),

            new XPathQuery("//rdf:RDF/@xml:base", MarketplaceURI,
                    "root element must have xml:base attribute with value "
                            + MarketplaceURI),

            new XPathQuery("count(//rdf:RDF/rdf:Description)", "1",
                    "description must have exactly 1 rdf:Description element"),

            new XPathQuery("count(//rdf:RDF/rdf:Description/@rdf:about)", "1",
                    "rdf:Description element must have an rdf:about attribute"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/dcterms:identifier)", "1",
                    "description must have exactly 1 dcterms:identifier element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:checksum)>0",
                    "true",
                    "description must have at least one slterms:checksum element"),

            new XPathQuery("count(//rdf:RDF/rdf:Description/dcterms:type)",
                    "1", "description must have exactly 1 dcterms:type element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:endorsement)",
                    "1",
                    "description must have exactly 1 slterms:endorsement element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:endorsement/dcterms:created)",
                    "1",
                    "slterms:endorsement must have exactly 1 dcterms:created element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:endorsement/dcterms:creator)",
                    "1",
                    "slterms:endorsement must have exactly 1 dcterms:creator element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:endorsement/dcterms:creator/slterms:email)",
                    "1",
                    "endorsement dcterms:creator must have exactly 1 slterms:email element"),

    };

    private MetadataContentValidation() {

    }

    public static void checkStructure(Element root) {

        for (XPathQuery check : XPATH_CHECKS) {
            check.evaluate(root);
        }

    }

    private static XPath createXpath() {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(MetadataNamespaceContext.getInstance());
        return xpath;
    }

    private static class XPathQuery {

        private final XPathExpression query;
        private final Object correctResult;
        private final String message;

        public XPathQuery(String query, Object correctResult, String message) {

            if (correctResult == null || message == null) {
                throw new IllegalArgumentException();
            }

            this.correctResult = correctResult;
            this.message = message;

            XPath xpath = createXpath();
            try {
                this.query = xpath.compile(query);
            } catch (XPathExpressionException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public void evaluate(Object item) {

            try {
                String result = query.evaluate(item);
                if (!(correctResult.equals(result))) {
                    throw new MetadataException(message);
                }
            } catch (XPathExpressionException e) {
                throw new MetadataException(e);
            }
        }
    }
}
