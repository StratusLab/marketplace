package eu.stratuslab.marketplace.metadata;

import org.junit.Test;

public class MetadataContentValidationTest {

    @Test(expected = MetadataException.class)
    public void nullRootFails() {
        MetadataContentValidation.checkRootElementType(null);
    }

}
