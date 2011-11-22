package eu.stratuslab.marketplace.metadata;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.X509Info;
import eu.stratuslab.marketplace.X509Utils;
import eu.stratuslab.marketplace.XMLUtils;

/*
 These tests depend on the test certificate being valid.  
 If these fail unexpectedly, check that the certificate 
 is still valid and generate a new one if necessary.  The 
 script for doing this is included in the src/main/scripts area.
 */
public class SignValidateCycleTest {

    private static void signAndValidate(String name) throws SAXException,
            IOException {

        InputStream is = null;

        try {

            // Read, sign, and put into string.
            String signedContents = signDocument(name);

            // Recreate signed document.
            Document signedDoc = XMLUtils.documentFromString(signedContents);

            // Check that all's OK.
            ValidateMetadata.validate(signedDoc);

        } finally {
            closeReliably(is);
        }

    }

    private static void signAndValidateTwice(String name) throws SAXException,
            IOException {

        InputStream is = null;

        try {

            // Read, sign, and put into string.
            String signedContents = signDocument(name);

            // Recreate signed document.
            Document signedDoc = XMLUtils.documentFromString(signedContents);

            // Sign again.
            String signedContents2 = signDocument(signedDoc);

            signedDoc = XMLUtils.documentFromString(signedContents2);

            // Check that all's OK.
            ValidateMetadata.validate(signedDoc);

        } finally {
            closeReliably(is);
        }

    }

    @Test
    public void testNormalCycleOK() throws SAXException, IOException {
        signAndValidate("valid-minimal.xml");
    }

    @Test
    public void testDoubleSignatureOK() throws SAXException, IOException {
        signAndValidateTwice("valid-minimal.xml");
    }

    @Test
    public void testNormalCycleOKFillingEndorsement() throws SAXException,
            IOException {
        signAndValidate("valid-minimal-empty-endorsement.xml");
    }

    @Test(expected = MetadataException.class)
    public void testModifiedDocFails() throws SAXException, IOException {

        InputStream is = null;

        try {

            String signedContents = signDocument("valid-minimal.xml");

            // CORRUPT THE INFORMATION.
            signedContents = signedContents.replace(">SHA-1", ">SHA-2");

            Document signedDoc = XMLUtils.documentFromString(signedContents);

            // This should now throw an exception.
            ValidateXMLSignature.validate(signedDoc);

        } finally {
            closeReliably(is);
        }

    }

    private static String signDocument(String name) throws SAXException,
            IOException {

        Document doc = readDocument(name);
        return signDocument(doc);
    }

    private static String signDocument(Document doc) throws SAXException,
            IOException {

        InputStream is = null;

        try {

            // Read the PKCS12 information.
            is = SignValidateCycleTest.class.getResourceAsStream("test.p12");

            String passwd = "XYZXYZ";

            KeyStore keyStore = X509Utils.pkcs12ToKeyStore(is, passwd);
            X509Info x509Info = X509Utils.x509FromKeyStore(keyStore, passwd);

            MetadataUtils.signMetadataEntry(doc, x509Info, null);

            // Store the signed content in a string.
            return XMLUtils.documentToString(doc);

        } finally {
            closeReliably(is);
        }

    }

    private static Document readDocument(String name) {
        return TestUtils
                .readResourceDocument(SignValidateCycleTest.class, name);
    }

    private static void closeReliably(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException consumed) {

            }
        }
    }

}
