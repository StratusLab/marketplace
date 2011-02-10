package eu.stratuslab.marketplace.metadata;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;

public class CheckXMLSignatures {

    public static void main(String[] args) throws Exception {

        int rc = 0;

        for (String fname : args) {

            File file = new File(fname);

            if (!file.canRead()) {
                System.err.println("Cannot read " + fname);
                rc++;
                continue;
            }

            DocumentBuilder db = XMLUtils.newDocumentBuilder(false);

            Document doc = db.parse(file);

            try {
                ValidateXMLSignature.validate(doc);
            } catch (MetadataException e) {
                e.printStackTrace();
                System.err.println("signature for " + fname + " is INVALID");
                rc = 1;
            }

        }

        System.exit(rc);
    }

}
