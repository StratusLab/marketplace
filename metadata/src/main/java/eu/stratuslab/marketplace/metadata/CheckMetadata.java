package eu.stratuslab.marketplace.metadata;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.XMLUtils;

public class CheckMetadata {

    public static void main(String[] args) {

        int rc = 0;

        for (String fname : args) {

            File file = new File(fname);

            if (!file.canRead()) {
                System.err.println("Cannot read " + fname);
                rc++;
                continue;
            }

            try {

                DocumentBuilder db = XMLUtils.newDocumentBuilder(false);

                Document doc = db.parse(file);

                ValidateMetadata.validate(doc);

                System.out.println("Valid: " + fname);

            } catch (MetadataException e) {

                rc++;
                System.err.println("Invalid: " + fname + "\n" + e.getMessage());

            } catch (SAXException e) {

                rc++;
                System.err.println("Invalid: " + fname + "\n" + e.getMessage());

            } catch (IOException e) {

                rc++;
                System.err.println("IO exception during read: " + fname + "\n"
                        + e.getMessage());

            }
        }

        System.exit(rc);
    }

}
