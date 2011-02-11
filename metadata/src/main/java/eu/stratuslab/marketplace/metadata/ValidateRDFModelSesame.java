package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.io.Reader;
import java.io.StringReader;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;

public class ValidateRDFModelSesame {

    private ValidateRDFModelSesame() {

    }

    public static void validate(Document doc) {

        Repository rep = null;

        try {

            // Create a deep copy of the document and strip signature elements.
            Document copy = (Document) doc.cloneNode(true);
            MetadataUtils.stripSignatureElements(copy);

            // Create a reader for the content for loading into repository.
            String rdfEntry = XMLUtils.documentToString(copy);
            Reader reader = new StringReader(rdfEntry);

            rep = new SailRepository(new MemoryStore());
            rep.initialize();

            RepositoryConnection con = rep.getConnection();
            try {
                con.add(reader, MARKETPLACE_URI, RDFFormat.RDFXML);
            } finally {
                con.close();
            }

            rep.shutDown();

        } catch (OpenRDFException e) {
            throw new MetadataException(e.getMessage());
        } catch (java.io.IOException e) {
            throw new MetadataException(e.getMessage());
        } finally {
            if (rep != null) {
                try {
                    rep.shutDown();
                } catch (RepositoryException consumed) {
                }
            }
        }
    }

}
