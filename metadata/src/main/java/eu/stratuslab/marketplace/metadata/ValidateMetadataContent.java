package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.math.BigInteger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

/**
 * Static utilities to validate the content of an image metadata descriptor.
 * 
 * @author loomis
 * 
 */
public class ValidateMetadataContent {

    private static final XPathQuery IDENTIFIER_ABOUT = new XPathQuery(
            "//rdf:RDF/rdf:Description/@rdf:about", "", "");

    private static final XPathQuery IDENTIFIER_ELEMENT = new XPathQuery(
            "//rdf:RDF/rdf:Description/dcterms:identifier", "", "");

    private static final XPathQuery SHA1_CHECKSUM = new XPathQuery(
            "//rdf:RDF/rdf:Description/slreq:checksum[@slreq:type='SHA-1']",
            "", "");

    private static final XPathQuery[] XPATH_CHECKS = {

            new XPathQuery("//rdf:RDF/@xml:base", MARKETPLACE_URI,
                    "root element must have xml:base attribute with value "
                            + MARKETPLACE_URI),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/dcterms:identifier)", "1",
                    "description must have exactly 1 dcterms:identifier element"),

            new XPathQuery("count(//rdf:RDF/rdf:Description/slreq:checksum)>0",
                    "true",
                    "description must have at least one slreq:checksum element"),

            new XPathQuery("count(//rdf:RDF/rdf:Description/dcterms:type)=1",
                    "true",
                    "description must have exactly 1 dcterms:type element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:serial-number)>1",
                    "true",
                    "description must have at most 1 slterms:serial-number element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:version)>1",
                    "true",
                    "description must have at most 1 slterms:version element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:hypervisor)>1",
                    "true",
                    "description must have at most 1 slterms:hypervisor element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:deprecated)>1",
                    "true",
                    "description must have at most 1 slterms:deprecated element"),

            new XPathQuery("count(//rdf:RDF/rdf:Description/slterms:os)>1",
                    "true",
                    "description must have at most 1 slterms:os element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:os-arch)>1",
                    "true",
                    "description must have at most 1 slterms:os-arch element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:os-version)>1",
                    "true",
                    "description must have at most 1 slterms:os-version element"),

    };

    private ValidateMetadataContent() {

    }

    public static void validate(Document doc) {

        for (XPathQuery check : XPATH_CHECKS) {
            check.evaluate(doc);
        }

        checkConsistentIdentifiers(doc);

        checkConsistentChecksum(doc);
    }

    private static void checkConsistentIdentifiers(Document doc) {

        String idAbout = IDENTIFIER_ABOUT.result(doc);
        String idElement = IDENTIFIER_ELEMENT.result(doc);

        if (!idAbout.equals("#" + idElement)) {
            throw new MetadataException("rdf:about attribute (" + idAbout
                    + ") and dcterms:identifier (" + idElement
                    + ") are not consistent");
        }
    }

    private static void checkConsistentChecksum(Document doc) {

        String identifier = IDENTIFIER_ELEMENT.result(doc);
        BigInteger checksumIdentifier = MetadataUtils
                .identifierToSha1(identifier);
        String sha1ChecksumIdentifier = checksumIdentifier.toString(16);

        String sha1Checksum = SHA1_CHECKSUM.result(doc);

        if (!sha1Checksum.equals(sha1ChecksumIdentifier)) {
            throw new MetadataException("checksum from identifier ("
                    + sha1ChecksumIdentifier + ") and SHA-1 checksum ("
                    + sha1Checksum + ") are not consistent");

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

        public String result(Object item) {
            try {
                return query.evaluate(item);
            } catch (XPathExpressionException e) {
                throw new MetadataException(e);
            }
        }

        public void evaluate(Object item) {
            if (!(correctResult.equals(result(item)))) {
                throw new MetadataException(message);
            }
        }
    }
}