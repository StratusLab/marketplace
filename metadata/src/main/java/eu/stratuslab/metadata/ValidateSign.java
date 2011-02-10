package eu.stratuslab.metadata;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This is a simple example of validating an XML Signature using the JSR 105
 * API. It assumes the key needed to validate the signature is contained in a
 * KeyValue KeyInfo.
 */
@SuppressWarnings("restriction")
public class ValidateSign {

    //
    // Synopsis: java Validate [document]
    //
    // where "document" is the name of a file containing the XML document
    // to be validated.
    //
    public static void main(String[] args) throws Exception {

        // Instantiate the document to be validated
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(
                new FileInputStream(args[0]));

        // Find Signature element
        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
                "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Cannot find Signature element");
        }

        // Create a DOM XMLSignatureFactory that will be used to unmarshal the
        // document containing the XMLSignature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Create a DOMValidateContext and specify a KeyValue KeySelector
        // and document context
        DOMValidateContext valContext = new DOMValidateContext(
                new X509KeySelector(), nl.item(0));

        // unmarshal the XMLSignature
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);

        // Validate the XMLSignature (generated above)
        boolean coreValidity = signature.validate(valContext);

        // Check core validation status
        if (coreValidity == false) {
            System.err.println("Signature failed core validation");
            boolean sv = signature.getSignatureValue().validate(valContext);
            System.out.println("signature validation status: " + sv);
            // check the validation status of each Reference
            Iterator i = signature.getSignedInfo().getReferences().iterator();
            for (int j = 0; i.hasNext(); j++) {
                boolean refValid = ((Reference) i.next()).validate(valContext);
                System.out.println("ref[" + j + "] validity status: "
                        + refValid);
            }
        } else {
            System.out.println("Signature passed core validation");
        }
    }

    /**
     * KeySelector which retrieves the public key out of the KeyValue element
     * and returns it. NOTE: If the key algorithm doesn't match signature
     * algorithm, then the public key will be ignored.
     */
    public static class X509KeySelector extends KeySelector {
        public KeySelectorResult select(KeyInfo keyInfo,
                KeySelector.Purpose purpose, AlgorithmMethod method,
                XMLCryptoContext context) throws KeySelectorException {
            Iterator ki = keyInfo.getContent().iterator();
            PublicKey key = null;
            while (ki.hasNext()) {
                XMLStructure info = (XMLStructure) ki.next();
                if (!(info instanceof X509Data) && !(info instanceof KeyValue))
                    continue;
                if (info instanceof X509Data) {
                    X509Data x509Data = (X509Data) info;
                    Iterator xi = x509Data.getContent().iterator();
                    while (xi.hasNext()) {
                        Object o = xi.next();
                        if (!(o instanceof X509Certificate))
                            continue;
                        System.out.println("Signed by : "
                                + ((X509Certificate) o)
                                        .getSubjectX500Principal().getName());
                        key = ((X509Certificate) o).getPublicKey();
                    }
                }
                if (info instanceof KeyValue) {
                    try {
                        key = ((KeyValue) info).getPublicKey();
                    } catch (KeyException ke) {
                        throw new KeySelectorException(ke);
                    }
                }
                // Make sure the algorithm is compatible
                // with the method.
                if (algEquals(method.getAlgorithm(), key.getAlgorithm())) {
                    return new SimpleKeySelectorResult(key);
                }
            }

            throw new KeySelectorException("No key found!");
        }

        static boolean algEquals(String algURI, String algName) {
            if ((algName.equalsIgnoreCase("DSA") && algURI
                    .equalsIgnoreCase(SignatureMethod.DSA_SHA1))
                    || (algName.equalsIgnoreCase("RSA") && algURI
                            .equalsIgnoreCase(SignatureMethod.RSA_SHA1))) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static class SimpleKeySelectorResult implements KeySelectorResult {
        private PublicKey pk;

        SimpleKeySelectorResult(PublicKey pk) {
            this.pk = pk;
        }

        public Key getKey() {
            return pk;
        }
    }
}
