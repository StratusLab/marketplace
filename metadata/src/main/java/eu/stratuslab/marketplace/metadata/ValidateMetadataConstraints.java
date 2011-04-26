package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.PatternUtils;

public final class ValidateMetadataConstraints {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        DATE_FORMAT.setLenient(false);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String TRUE = "true";

    private static final XPathQuery IDENTIFIER_ABOUT = new XPathQuery(
            "//rdf:RDF/rdf:Description/@rdf:about", "", "");

    private static final XPathQuery IDENTIFIER_ELEMENT = new XPathQuery(
            "//rdf:RDF/rdf:Description/dcterms:identifier", "", "");

    private static final XPathQuery SHA1_CHECKSUM = new XPathQuery(
            "//rdf:RDF/rdf:Description/slreq:checksum/slreq:value[preceding-sibling::slreq:algorithm='SHA-1']",
            "", "");

    private static final XPathQuery VALID_DATE = new XPathQuery(
            "//rdf:RDF/rdf:Description/dcterms:valid", "", "");

    private static final XPathQuery CREATED_DATE = new XPathQuery(
            "//rdf:RDF/rdf:Description/slreq:endorsement/dcterms:created", "",
            "");

    private static final XPathQuery[] XPATH_CHECKS = {

            new XPathQuery("//rdf:RDF/@xml:base", MARKETPLACE_URI,
                    "root element must have xml:base attribute with value "
                            + MARKETPLACE_URI),

            new XPathQuery("count(//rdf:RDF/rdf:Description/dcterms:type)=1",
                    TRUE,
                    "description must have exactly 1 dcterms:type element"),

            new XPathQuery("count(//rdf:RDF/rdf:Description/dcterms:type/*)=0",
                    TRUE, "dcterms:type cannot have child elements"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slreq:checksum/slreq:algorithm[text()='SHA-1'])=1",
                    TRUE,
                    "description must have exactly 1 slterms:checksum element using SHA-1 algorithm"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:serial-number)<=1",
                    TRUE,
                    "description must have at most 1 slterms:serial-number element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:version)<=1",
                    TRUE,
                    "description must have at most 1 slterms:version element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:deprecated)<=1",
                    TRUE,
                    "description must have at most 1 slterms:deprecated element"),

            new XPathQuery("count(//rdf:RDF/rdf:Description/slterms:os)<=1",
                    TRUE, "description must have at most 1 slterms:os element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:os-arch)<=1",
                    TRUE,
                    "description must have at most 1 slterms:os-arch element"),

            new XPathQuery(
                    "count(//rdf:RDF/rdf:Description/slterms:os-version)<=1",
                    TRUE,
                    "description must have at most 1 slterms:os-version element"),

    };

    private ValidateMetadataConstraints() {

    }

    public static void validate(Document doc) {

        for (XPathQuery check : XPATH_CHECKS) {
            check.evaluate(doc);
        }

        checkConsistentIdentifiers(doc);

        checkConsistentChecksum(doc);

        checkDates(doc);

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

        BigInteger sha1Checksum = hexToBigInteger(SHA1_CHECKSUM.result(doc));

        if (!sha1Checksum.equals(checksumIdentifier)) {
            throw new MetadataException("checksum from identifier ("
                    + checksumIdentifier + ") and SHA-1 checksum ("
                    + sha1Checksum + ") are not consistent");

        }
    }

    public static BigInteger hexToBigInteger(String hex) {
        try {
            return new BigInteger(hex, 16);
        } catch (NumberFormatException e) {
            throw new MetadataException(hex + " is not a valid SHA-1 checksum");
        }
    }

    private static void checkDates(Document doc) {

        String validDate = VALID_DATE.result(doc);
        if (!isValidDate(validDate)) {
            throw new MetadataException("dcterms:valid value (" + validDate
                    + ") is not a valid date");
        }

        String creationDate = CREATED_DATE.result(doc);
        if (!isValidDate(creationDate)) {
            throw new MetadataException("dcterms:created value (" + validDate
                    + ") is not a valid date");
        }

    }

    private static boolean isValidDate(String date) {
        if (date != null && (!"".equals(date))) {
            Matcher m = PatternUtils.DATE.matcher(date);
            if (m.matches()) {
                try {
                    DATE_FORMAT.parse(date);
                    return true;
                } catch (ParseException e) {
                    throw new MetadataException(e.getMessage());
                }
            } else {
                return false;
            }
        } else {
            return true;
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
