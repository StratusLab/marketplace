package eu.stratuslab.marketplace.metadata;

import org.w3c.dom.Document;

public class ValidateMetadata {

    private ValidateMetadata() {

    }

    public static void validate(Document doc) {
        validate(doc, true);
    }

    public static void validate(Document doc, boolean checkSignature) {

        if (checkSignature) {
            ValidateXMLSignature.validate(doc);
        }
        ValidateXMLSchema.validate(doc);
        ValidateMetadataConstraints.validate(doc);
        ValidateRDFModel.validate(doc);

    }

}
