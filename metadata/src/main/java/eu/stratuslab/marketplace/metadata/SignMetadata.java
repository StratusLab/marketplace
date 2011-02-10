package eu.stratuslab.marketplace.metadata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyStore;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.X509Info;
import eu.stratuslab.marketplace.X509Utils;
import eu.stratuslab.marketplace.XMLUtils;

public class SignMetadata {

    final static private String USAGE = "Usage: java eu.stratuslab.marketplace.metadata.SignMetadata [metadata file] [P12 GridCertificate] [P12 GridCertificate Passwd]";

    public static final Pattern cnExtractionPattern = Pattern
            .compile(".*CN=([^,]*?),.*");

    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
            System.err.println(USAGE);
            System.exit(1);
        }

        File metadataFile = new File(args[0]);
        File outputFile = new File(args[0] + ".signed");
        File pkcs12File = new File(args[1]);
        String passwd = args[2];

        // Read the PKCS12 information.
        KeyStore keyStore = X509Utils.pkcs12ToKeyStore(pkcs12File, passwd);
        X509Info x509Info = X509Utils.x509FromKeyStore(keyStore, passwd);

        // Instantiate the document to be signed
        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document doc = db.parse(metadataFile);

        // Sign the document. The document is directly modified by method.
        X509Utils.signDocument(x509Info, doc);

        // Write the signed output to disk.
        String signedContents = XMLUtils.documentToString(doc);
        writeStringToFile(signedContents, outputFile);
    }

    private static void writeStringToFile(String contents, File outputFile) {

        FileWriter os = null;
        try {

            os = new FileWriter(outputFile);
            os.write(contents);

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

    }

}
