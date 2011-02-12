package eu.stratuslab.marketplace.metadata;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.w3c.dom.Document;

public class ValidateXMLSchemaTest {

    @Test
    public void ensureSchemaInitializationWorks() {
        System.out.println(ValidateXMLSchema.schema);
    }

    @Test
    public void checkValidDocuments() {

        String[] names = { "valid-minimal.xml", "valid-minimal-signature.xml" };

        for (String name : names) {
            Document doc = readDocument(name);
            ValidateXMLSchema.validate(doc);
        }
    }

    @Test
    public void checkInvalidDocuments() {

        String[] names = { "invalid-no-xml-base.xml", "invalid-no-content.xml",
                "invalid-bad-about-id.xml",
                "invalid-bad-element-for-signature.xml",
                "invalid-bad-checksum-parsetype.xml",
                "invalid-bad-endorsement-parsetype.xml",
                "invalid-bad-endorser-parsetype.xml",
                "invalid-checksum-value.xml", "invalid-email.xml",
                "invalid-no-endorsement.xml", "invalid-no-endorser.xml",
                "invalid-bad-serial-number.xml",
                "invalid-bad-inbound-port.xml",
                "invalid-bad-outbound-port.xml", "invalid-bad-icmp-value.xml" };

        for (String name : names) {
            Document doc = readDocument(name);
            try {
                ValidateXMLSchema.validate(doc);
                fail("validation of " + name
                        + " succeeded but should have failed");
            } catch (MetadataException e) {
                // OK, expected problem.
            }
        }
    }

    private static Document readDocument(String name) {
        return TestUtils
                .readResourceDocument(ValidateXMLSchemaTest.class, name);
    }

}
