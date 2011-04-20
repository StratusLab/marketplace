package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataUtils.writeStringToFile;

import java.io.File;
import java.security.KeyStore;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.X509Info;
import eu.stratuslab.marketplace.X509Utils;
import eu.stratuslab.marketplace.XMLUtils;

public class SignMetadata {

    final static private String USAGE = "Usage:\n"
            + "  java eu.stratuslab.marketplace.metadata.SignMetadata \\\n"
            + "    [metadata file] [signed metadata file] [P12 Certificate] [Password]";

    private SignMetadata() {

    }

    public static void main(String[] args) throws Exception {

        if (args.length != 4) {
            System.err.println(USAGE);
            System.exit(1);
        }

        File metadataFile = new File(args[0]);
        File outputFile = new File(args[1]);
        File pkcs12File = new File(args[2]);
        String passwd = args[3];

        // Read the PKCS12 information.
        KeyStore keyStore = X509Utils.pkcs12ToKeyStore(pkcs12File, passwd);
        X509Info x509Info = X509Utils.x509FromKeyStore(keyStore, passwd);

        // Instantiate the document to be signed
        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document doc = db.parse(metadataFile);

        // Fill in the endorsement element if it is empty.
        MetadataUtils.fillEndorsementElement(doc, x509Info);

        // Sign the document. The document is directly modified by method.
        X509Utils.signDocument(x509Info, doc);

        // Write the signed output to disk.
        String signedContents = XMLUtils.documentToString(doc);
        writeStringToFile(signedContents, outputFile);
    }

}
