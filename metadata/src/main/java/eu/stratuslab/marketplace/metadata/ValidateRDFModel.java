package eu.stratuslab.marketplace.metadata;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;

public final class ValidateRDFModel {

    private ValidateRDFModel() {

    }

    public static void validate(Document doc) {

        Reader reader = null;

        try {

            Document copy = (Document) doc.cloneNode(true);
            MetadataUtils.stripSignatureElements(copy);

            String rdfEntry = XMLUtils.documentToString(copy);
            reader = new StringReader(rdfEntry);

            RDFParser rdfParser = Rio.createParser(RDFFormat.RDFXML);

            rdfParser.setRDFHandler(new RDFHandlerBase());

            rdfParser.parse(reader, "http://example.org/metadata");

        } catch (OpenRDFException e) {
            throw new MetadataException(e.getMessage());
        } catch (java.io.IOException e) {
            throw new MetadataException(e.getMessage());
        } finally {
            closeReader(reader);
        }
    }

    public static void closeReader(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException consumed) {

            }
        }
    }

}
