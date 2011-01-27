package eu.stratuslab.marketplace.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import eu.stratuslab.marketplace.metadata.CreateChecksums.Namespace;
import org.w3c.dom.Document;

public class CreateMetadata {

    public enum MetadataFields {
        TYPE(Namespace.DCTERMS, "type", true), PUBLISHER(Namespace.SLTERMS,
                "publisher", true), TITLE(Namespace.SLTERMS, "title", true), DESCRIPTION(
                Namespace.SLTERMS, "description", true), VERSION(
                Namespace.SLTERMS, "version", true), SERIAL_NUMBER(
                Namespace.SLTERMS, "serial-number", true), OS(
                Namespace.SLTERMS, "os", true), OS_VERSION(Namespace.SLTERMS,
                "os-version", true), OS_ARCH(Namespace.SLTERMS, "os-arch", true), HYPERVISOR(
                Namespace.SLTERMS, "hypervisor", true), EMAIL(
                Namespace.SLTERMS, "email", false), FULL_NAME(
                Namespace.SLTERMS, "full-name", false);

        final public Namespace ns;
        final public String tag;
        final public boolean isMetadataDesc;

        private MetadataFields(Namespace ns, String tag, boolean isMetadataDesc) {
            this.ns = ns;
            this.tag = tag;
            this.isMetadataDesc = isMetadataDesc;
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException,
            ParserConfigurationException, IOException, TransformerException {

        Map<MetadataFields, String> userInput = new HashMap<MetadataFields, String>();

        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Image filename:");
        String filename = r.readLine();

        File imageFile = new File(filename);

        for (MetadataFields field : MetadataFields.values()) {

            System.out.println(field.tag);
            String data = r.readLine();

            if (data!=null && !data.trim().equals("")) {
                userInput.put(field, data);
            }

        }

        Document doc = CreateChecksums.createSkeletonRDF(imageFile, userInput);
        String result = CreateChecksums.documentToString(doc);

        System.out.println(result);

    }
}
