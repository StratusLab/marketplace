package eu.stratuslab.marketplace.metadata;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.XMLUtils;

public class TestUtils {

    public static Document readResourceDocument(Class<?> c, String name) {

        Document doc = null;

        InputStream is = null;
        try {

            is = c.getResourceAsStream(name);

            DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
            doc = db.parse(is);

        } catch (SAXException e) {
            doc = null;
            e.printStackTrace();

        } catch (IOException e) {
            doc = null;
            e.printStackTrace();

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException consumed) {

                }
            }
        }

        return doc;
    }

    @Test
    public void ensureSchemaInitializationWorks() {
        // Dummy test to keep maven happy.
    }

}
