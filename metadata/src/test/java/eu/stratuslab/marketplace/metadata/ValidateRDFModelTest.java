package eu.stratuslab.marketplace.metadata;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.w3c.dom.Document;

public class ValidateRDFModelTest {

    @Test
    public void checkValidDocuments() {

        String[] names = { "valid-minimal.xml", "valid-minimal-signature.xml" };

        for (String name : names) {
            Document doc = readDocument(name);
            ValidateRDFModel.validate(doc);
        }
    }

    @Test
    public void checkInvalidDocuments() {

        String[] names = { "invalid-rdf-attribute.xml",
                "invalid-rdf-resource.xml" };

        for (String name : names) {
            Document doc = readDocument(name);
            try {
                ValidateRDFModel.validate(doc);
                fail("validation of " + name
                        + " succeeded but should have failed");
            } catch (MetadataException e) {
                // OK, expected problem.
            }
        }
    }

    private static Document readDocument(String name) {
        return TestUtils.readResourceDocument(ValidateRDFModelTest.class, name);
    }

}
