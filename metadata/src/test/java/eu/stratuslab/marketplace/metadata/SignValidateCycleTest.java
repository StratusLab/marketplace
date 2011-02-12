package eu.stratuslab.marketplace.metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.xml.parsers.DocumentBuilder;

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

    @Test
    public void testNormalCycleOK() throws SAXException, IOException {

        InputStream is = null;

        try {

            // Read, sign, and put into string.
            String signedContents = signDocument("valid-minimal.xml");

            // Recreate signed document.
            is = new ByteArrayInputStream(signedContents.getBytes());
            DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
            Document signedDoc = db.parse(is);

            // Check that all's OK.
            ValidateXMLSignature.validate(signedDoc);

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException consumed) {

                }
            }
        }

    }

    @Test(expected = MetadataException.class)
    public void testModifiedDocFails() throws SAXException, IOException {

        InputStream is = null;

        try {

            // Read, sign, and put into string.
            String signedContents = signDocument("valid-minimal.xml");

            signedContents = signedContents.replace(">SHA-1", ">SHA-2");

            // Recreate signed document.
            is = new ByteArrayInputStream(signedContents.getBytes());
            DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
            Document signedDoc = db.parse(is);

            // This should now throw an exception.
            ValidateXMLSignature.validate(signedDoc);

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException consumed) {

                }
            }
        }

    }

    private static String signDocument(String name) throws SAXException,
            IOException {

        InputStream is = null;

        try {

            // Read the PKCS12 information.
            is = SignValidateCycleTest.class.getResourceAsStream("test.p12");

            String passwd = "XYZXYZ";

            KeyStore keyStore = X509Utils.pkcs12ToKeyStore(is, passwd);
            X509Info x509Info = X509Utils.x509FromKeyStore(keyStore, passwd);

            // Instantiate the document to be signed
            Document doc = readDocument(name);

            // Sign the document. The document is directly modified by method.
            X509Utils.signDocument(x509Info, doc);

            // Store the signed content in a string.
            return XMLUtils.documentToString(doc);

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException consumed) {

                }
            }
        }

    }

    private static Document readDocument(String name) {
        return TestUtils
                .readResourceDocument(SignValidateCycleTest.class, name);
    }

}
