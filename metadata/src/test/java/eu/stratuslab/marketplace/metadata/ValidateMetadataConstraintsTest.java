package eu.stratuslab.marketplace.metadata;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.w3c.dom.Document;

public class ValidateMetadataConstraintsTest {

    @Test
    public void checkValidDocuments() {

        String[] names = { "valid-minimal.xml", "valid-minimal-signature.xml" };

        for (String name : names) {
            Document doc = readDocument(name);
            ValidateMetadataConstraints.validate(doc);
        }
    }

    @Test
    public void checkInvalidDocuments() {

        String[] names = { "invalid-inconsistent-identifiers.xml",
                "invalid-inconsistent-checksum.xml",
                "invalid-bad-xml-base.xml", "invalid-multiple-deprecated.xml",
                "invalid-multiple-serial-numbers.xml",
                "invalid-multiple-versions.xml", "invalid-multiple-os.xml",
                "invalid-multiple-os-arch.xml",
                "invalid-multiple-os-version.xml", "invalid-dcterms-type.xml" };

        for (String name : names) {
            Document doc = readDocument(name);
            try {
                ValidateMetadataConstraints.validate(doc);
                fail("validation of " + name
                        + " succeeded but should have failed");
            } catch (MetadataException e) {
                // OK, expected problem.
            }
        }
    }

    private static Document readDocument(String name) {
        return TestUtils.readResourceDocument(
                ValidateMetadataConstraintsTest.class, name);
    }

}
