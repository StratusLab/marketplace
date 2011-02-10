package eu.stratuslab.marketplace.metadata;

import javax.xml.crypto.dsig.XMLSignature;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("restriction")
public class ValidateXMLSignature {

    private ValidateXMLSignature() {

    }

    public static String validate(Document doc) {

        boolean isValid = true;

        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
                "Signature");

        if (nl.getLength() > 0) {

            Node node = nl.item(0);

            Object[] result = MetadataUtils.isSignatureOK(node);

            isValid = ((Boolean) result[0]).booleanValue();
            String message = (String) result[1];

            if (isValid) {
                return message;
            } else {
                throw new MetadataException(message);
            }

        } else {
            throw new MetadataException("no signature");
        }

    }
}
