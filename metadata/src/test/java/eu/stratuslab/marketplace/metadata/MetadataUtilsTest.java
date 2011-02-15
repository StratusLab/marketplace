package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLREQ_NS_URI;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.X509Info;
import eu.stratuslab.marketplace.X509Utils;
import eu.stratuslab.marketplace.XMLUtils;

public class MetadataUtilsTest {

    @Test(expected = MetadataException.class)
    public void checkNoNPE() throws SAXException, IOException {
        Document doc = readDocument("valid-minimal-signature.xml");

        ValidateXMLSignature.validate(doc);
    }

    @Test
    public void endorsementNotReplaced() {

        Document doc = readDocument("autogen-endorsement-not-done.xml");
        X509Info x509info = getX509Info();
        MetadataUtils.fillEndorsementElement(doc, x509info);
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
        MetadataUtils.fillEndorsementElement(doc, x509info);
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

    private static X509Info getX509Info() {

        InputStream is = MetadataUtilsTest.class
                .getResourceAsStream("test.p12");
        String password = "XYZXYZ";

        KeyStore keyStore = X509Utils.pkcs12ToKeyStore(is, password);
        return X509Utils.x509FromKeyStore(keyStore, password);
    }

    private static Document readDocument(String name) {
        return TestUtils.readResourceDocument(MetadataUtilsTest.class, name);
    }

}
