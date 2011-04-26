package eu.stratuslab.marketplace.metadata;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class ValidateXMLSchema {

    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String[] XSD_FILES = { "xml.xsd", "dcmitype.xsd",
            "dc.xsd", "dcterms.xsd", "slreq.xsd", "image-metadata.xsd",
            "slterms.xsd" };

    public static final Schema SCHEMA;

    static {

        try {

            SchemaFactory factory = SchemaFactory.newInstance(W3C_XML_SCHEMA);

            Source[] sources = new Source[XSD_FILES.length];

            for (int i = 0; i < XSD_FILES.length; i++) {
                InputStream is;
                is = ValidateXMLSchema.class.getResourceAsStream(XSD_FILES[i]);
                sources[i] = new StreamSource(is);
            }

            SCHEMA = factory.newSchema(sources);

        } catch (SAXException e) {
            throw new ExceptionInInitializerError(e);
        }

    }

    private ValidateXMLSchema() {

    }

    public static void validate(Document doc) {

        Validator validator = SCHEMA.newValidator();

        Source source = new DOMSource(doc);

        try {

            validator.validate(source);

        } catch (SAXException e) {
            throw new MetadataException("XML schema validation exception: "
                    + e.getMessage());

        } catch (IOException e) {
            throw new MetadataException(
                    "IO exception during schema validation: " + e.getMessage());
        }

    }

}
