package eu.stratuslab.marketplace.metadata;

import org.junit.Test;
import org.w3c.dom.Document;

public class ValidateMetadataTest {

    @Test
    public void checkValidDocuments() {

        String[] names = { "valid-minimal.xml", "valid-full.xml" };

        for (String name : names) {
            Document doc = readDocument(name);
            ValidateXMLSchema.validate(doc);
            ValidateMetadataConstraints.validate(doc);
            ValidateRDFModel.validate(doc);
        }
    }

    private static Document readDocument(String name) {
        return TestUtils.readResourceDocument(ValidateMetadataTest.class, name);
    }

}
