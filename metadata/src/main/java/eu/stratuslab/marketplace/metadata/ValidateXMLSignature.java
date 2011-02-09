package eu.stratuslab.marketplace.metadata;

import javax.xml.crypto.dsig.XMLSignature;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ValidateXMLSignature {

    private ValidateXMLSignature() {

    }

    public static void validate(Document doc) {

        boolean isValid = true;

        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
                "Signature");

        if (nl.getLength() == 0) {
            isValid = false;
        }

        for (int i = 0; i < nl.getLength(); i++) {

            Node node = nl.item(i);

            if (!MetadataUtils.isSignatureOK(node)) {
                isValid = false;
            }
        }

        if (!isValid) {
            throw new MetadataException("invalid signature");
        }
    }

}
