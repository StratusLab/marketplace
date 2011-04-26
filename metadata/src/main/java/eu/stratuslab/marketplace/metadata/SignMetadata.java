package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataUtils.writeStringToFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.KeyStore;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.X509Info;
import eu.stratuslab.marketplace.X509Utils;
import eu.stratuslab.marketplace.XMLUtils;

public final class SignMetadata {

    private static final String USAGE = "Usage:\n"
            + "  java eu.stratuslab.marketplace.metadata.SignMetadata \\\n"
            + "    [metadata file] [signed metadata file] [P12 Certificate] [Password] [Default email]";

    private SignMetadata() {

    }

    public static void main(String[] args) throws Exception {

        if (args.length != 4 && args.length != 5) {
            System.err.println(USAGE);
            System.exit(1);
        }

        File metadataFile = new File(args[0]);
        File outputFile = new File(args[1]);
        File pkcs12File = new File(args[2]);
        String passwd = args[3];
        String email = null;
        if (args.length == 5) {
            email = args[4];
        }

        X509Info x509Info = null;
        Document doc = null;

        try {

            KeyStore keyStore = X509Utils.pkcs12ToKeyStore(pkcs12File, passwd);
            x509Info = X509Utils.x509FromKeyStore(keyStore, passwd);

        } catch (FileNotFoundException e) {
            System.err.println("certificate file not found: " + pkcs12File);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("error loading certificate (wrong password?)");
            System.exit(1);
        }

        try {

            // Instantiate the document to be signed
            DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
            doc = db.parse(metadataFile);

            // Fill in the endorsement element if it is empty.
            MetadataUtils.fillEndorsementElement(doc, x509Info, email);

            // Sign the document. The document is directly modified by method.
            X509Utils.signDocument(x509Info, doc);

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {

            String signedContents = XMLUtils.documentToString(doc);
            writeStringToFile(signedContents, outputFile);

        } catch (Exception e) {
            removeOutputFile(outputFile);
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void removeOutputFile(File file) {
        if (file.exists()) {
            boolean ok = file.delete();
            if (!ok) {
                System.err.println("error removing generated file: "
                        + file.toString());
            }
        }
    }

}
