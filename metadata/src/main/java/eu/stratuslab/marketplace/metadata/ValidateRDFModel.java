package eu.stratuslab.marketplace.metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.w3c.dom.Document;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

import eu.stratuslab.marketplace.XMLUtils;

public class ValidateRDFModel {

    private ValidateRDFModel() {

    }

    public static void validate(Document doc) {

        InputStream is = null;

        try {

            String contents = XMLUtils.documentToString(doc);
            is = new ByteArrayInputStream(contents.getBytes());

            // Create a model and read it from the given document.
            ModelMaker maker = ModelFactory.createMemModelMaker();
            Model model = maker.createModel(UUID.randomUUID().toString());
            model.read(is, "");

        } catch (Exception e) {

            throw new MetadataException(e);

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException consumed) {
                }
            }
        }

    }

}
