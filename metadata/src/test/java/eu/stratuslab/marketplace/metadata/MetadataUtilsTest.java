package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.RDF_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLREQ_NS_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.X509Info;
import eu.stratuslab.marketplace.X509Utils;
import eu.stratuslab.marketplace.X509UtilsTest;
import eu.stratuslab.marketplace.XMLUtils;

public class MetadataUtilsTest {

    public static final String EXAMPLE_EMAIL = "example@example.org";

    @Test(expected = MetadataException.class)
    public void checkNoNPE() throws SAXException, IOException {
        Document doc = readDocument("valid-minimal-signature.xml");

        ValidateXMLSignature.validate(doc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkInvalidSHA1Checksum1() {
        MetadataUtils.sha1ToIdentifier(BigInteger.valueOf(-1L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkInvalidSHA1Checksum2() {
        BigInteger value = BigInteger.TEN;
        while (value.bitLength() <= MetadataUtils.SHA1_BITS) {
            value = value.multiply(BigInteger.TEN);
        }
        MetadataUtils.sha1ToIdentifier(value);
    }

    @Test
    public void checkInvalidIdentifier1() {
        String[] invalidIds = new String[] { "A",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAA:AAAAAAAA" };

        for (String id : invalidIds) {
            try {
                MetadataUtils.identifierToSha1(id);
                fail(id + " is illegal but did not raise exception");
            } catch (IllegalArgumentException consumed) {
                // OK
            }
        }
    }

    public void checkIdEncodeDecodeCycle() {
        String data = "data for SHA1 digest";
        byte[] buffer = data.getBytes();

        ByteArrayInputStream is = new ByteArrayInputStream(buffer);

        Map<String, BigInteger> info = MetadataUtils.streamInfo(is);

        BigInteger sha1Digest = info.get("SHA-1");

        String identifier = MetadataUtils.sha1ToIdentifier(sha1Digest);

        BigInteger sha1DigestCheck = MetadataUtils.identifierToSha1(identifier);

        assertEquals(sha1Digest, sha1DigestCheck);

    }

    @Test
    public void endorsementNotReplaced() {

        Document doc = readDocument("autogen-endorsement-not-done.xml");
        X509Info x509info = getX509Info();
        MetadataUtils.fillEndorsementElement(doc, x509info, EXAMPLE_EMAIL);
        NodeList nl = doc.getElementsByTagNameNS("http://example.org/",
                "marker");
        if (nl.getLength() != 1) {
            fail("endorsement was replaced when it should not have been\n"
                    + XMLUtils.documentToString(doc));
        }
    }

    @Test
    public void endorsementGenerated() {

        Document doc = readDocument("autogen-endorsement-done.xml");
        X509Info x509info = getX509Info();
        MetadataUtils.fillEndorsementElement(doc, x509info, EXAMPLE_EMAIL);
        NodeList nl = doc.getElementsByTagNameNS(SLREQ_NS_URI, "endorsement");
        if (nl.getLength() == 1) {
            if (!nl.item(0).hasChildNodes()) {
                fail("endorsement was not replaced when it should have been\n"
                        + XMLUtils.documentToString(doc));
            }
        } else {
            fail("wrong number of endorsement elements\n"
                    + XMLUtils.documentToString(doc));
        }

    }

    @Test(expected = MetadataException.class)
    public void endorsementWithoutEmail() throws Exception {

        Document doc = readDocument("autogen-endorsement-done.xml");
        X509Info x509info = getX509InfoWithoutEmail();
        MetadataUtils.fillEndorsementElement(doc, x509info, null);

    }

    @Test(expected = MetadataException.class)
    public void endorsementWithInvalidEmail() throws Exception {

        Document doc = readDocument("autogen-endorsement-done.xml");
        X509Info x509info = getX509InfoWithoutEmail();
        MetadataUtils.fillEndorsementElement(doc, x509info,
                "not@a@valid@address");

    }

    @Test
    public void endorserHasParseTypeAttr() {

        Document doc = readDocument("autogen-endorsement-done.xml");
        X509Info x509info = getX509Info();
        MetadataUtils.fillEndorsementElement(doc, x509info, EXAMPLE_EMAIL);

        NodeList nl = doc.getElementsByTagNameNS(SLREQ_NS_URI, "endorser");
        if (nl.getLength() == 1) {
            NamedNodeMap map = nl.item(0).getAttributes();
            Node value = map.getNamedItemNS(RDF_NS_URI, "parseType");
            if (value != null) {
                if (!"Resource".equals(value.getTextContent())) {
                    fail("endorser rdf:parseType attribute has incorrect value\n"
                            + XMLUtils.documentToString(doc));
                }
            } else {
                fail("endorser did not have rdf:parseType attribute\n"
                        + XMLUtils.documentToString(doc));
            }
        } else {
            fail("wrong number of endorser elements\n"
                    + XMLUtils.documentToString(doc));
        }

    }

    private static X509Info getX509Info() {

        InputStream is = MetadataUtilsTest.class
                .getResourceAsStream("test.p12");
        String password = "XYZXYZ";

        KeyStore keyStore = X509Utils.pkcs12ToKeyStore(is, password);
        return X509Utils.x509FromKeyStore(keyStore, password);
    }

    private static X509Info getX509InfoWithoutEmail() throws Exception {
        X509Certificate[] certs = X509UtilsTest.getCertificateChain(null);
        return new X509Info(certs[1]);
    }

    private static Document readDocument(String name) {
        return TestUtils.readResourceDocument(MetadataUtilsTest.class, name);
    }

}
