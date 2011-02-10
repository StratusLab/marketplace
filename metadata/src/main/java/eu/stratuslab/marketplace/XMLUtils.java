package eu.stratuslab.marketplace;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class XMLUtils {

    private XMLUtils() {

    }

    public static DocumentBuilder newDocumentBuilder() {
        return newDocumentBuilder(true);
    }

    public static DocumentBuilder newDocumentBuilder(boolean validating) {

        try {

            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            dbfac.setNamespaceAware(true);
            dbfac.setValidating(validating);

            return dbfac.newDocumentBuilder();

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Document newDocument() {

        DocumentBuilder db = newDocumentBuilder();
        return db.newDocument();
    }

    public static Transformer newTransformer() {

        try {

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute("indent-number", Integer.valueOf(4));

            Transformer transformer;
            transformer = factory.newTransformer();
            transformer
                    .setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");

            return transformer;

        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static String documentToString(Document doc) {

        try {

            Transformer transformer = XMLUtils.newTransformer();

            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();

        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

    }

}
