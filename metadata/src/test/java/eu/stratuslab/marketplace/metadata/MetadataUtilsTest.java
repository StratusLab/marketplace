package eu.stratuslab.marketplace.metadata;

import java.io.IOException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class MetadataUtilsTest {

    @Test(expected = MetadataException.class)
    public void checkNoNPE() throws SAXException, IOException {
        Document doc = readDocument("valid-minimal-signature.xml");

        ValidateXMLSignature.validate(doc);
    }

    private static Document readDocument(String name) {
        return TestUtils.readResourceDocument(MetadataUtilsTest.class, name);
    }

}
