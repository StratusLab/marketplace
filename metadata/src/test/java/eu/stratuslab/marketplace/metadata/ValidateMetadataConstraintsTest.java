package eu.stratuslab.marketplace.metadata;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;
import org.w3c.dom.Document;

public class ValidateMetadataConstraintsTest {

    @Test
    public void checkValidDocuments() {

        String[] names = { "valid-minimal.xml", "valid-minimal-signature.xml",
                "valid-minimal-leading-zero-sha1.xml" };

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
                "invalid-multiple-os-version.xml", "invalid-dcterms-type.xml",
                "invalid-created-date-1.xml", "invalid-created-date-2.xml" };

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

    public void checkLeadingZerosOK() {
        String s = "024a8581d43fbf76d96ae3ec53b260bcddddad46";
        BigInteger v1 = ValidateMetadataConstraints.hexToBigInteger(s);
        BigInteger v2 = ValidateMetadataConstraints.hexToBigInteger("0" + s);
        assertEquals(v1, v2);
    }

    private static Document readDocument(String name) {
        return TestUtils.readResourceDocument(
                ValidateMetadataConstraintsTest.class, name);
    }

}
