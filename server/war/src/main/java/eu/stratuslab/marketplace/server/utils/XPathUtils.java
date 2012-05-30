/**
 * Created as part of the StratusLab project (http://stratuslab.eu),
 * co-funded by the European Commission under the Grant Agreement
 * INSFO-RI-261552.
 *
 * Copyright (c) 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratuslab.marketplace.server.utils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.metadata.MetadataException;
import eu.stratuslab.marketplace.metadata.MetadataNamespaceContext;

public final class XPathUtils {
    public static final XPathQuery IDENTIFIER_ELEMENT = new XPathQuery(
            "//rdf:RDF/rdf:Description/dcterms:identifier", "", "");
    public static final XPathQuery EMAIL = new XPathQuery(
            "//rdf:RDF/rdf:Description/slreq:endorsement/slreq:endorser/slreq:email",
            "", "");
    public static final XPathQuery CREATED_DATE = new XPathQuery(
            "//rdf:RDF/rdf:Description/slreq:endorsement/dcterms:created", "",
            "");
    public static final XPathQuery OS = new XPathQuery(
            "//rdf:RDF/rdf:Description/slterms:os", "", "");
    public static final XPathQuery OS_VERSION = new XPathQuery(
            "//rdf:RDF/rdf:Description/slterms:os-version", "", "");
    public static final XPathQuery OS_ARCH = new XPathQuery(
            "//rdf:RDF/rdf:Description/slterms:os-arch", "", "");
    public static final XPathQuery DESCRIPTION = new XPathQuery(
            "//rdf:RDF/rdf:Description/dcterms:description", "", "");
    public static final XPathQuery LOCATION = new XPathQuery(
            "//rdf:RDF/rdf:Description/slterms:location", "", "");
    public static final XPathQuery VALID = new XPathQuery(
            "//rdf:RDF/rdf:Description/dcterms:valid", "", "");
    
    private XPathUtils() {

    }

    public static String getValue(Document doc, XPathQuery query) {
        return query.result(doc);
    }

    private static XPath createXpath() {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(MetadataNamespaceContext.getInstance());
        return xpath;
    }

    private static class XPathQuery {

        private final XPathExpression query;
        private final Object correctResult;
        private final String message;

        public XPathQuery(String query, Object correctResult, String message) {

            if (correctResult == null || message == null) {
                throw new IllegalArgumentException();
            }

            this.correctResult = correctResult;
            this.message = message;

            XPath xpath = createXpath();
            try {
                this.query = xpath.compile(query);
            } catch (XPathExpressionException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public String result(Object item) {
            try {
                return query.evaluate(item);
            } catch (XPathExpressionException e) {
                throw new MetadataException(e);
            }
        }
        
        public void evaluate(Object item) {
            if (!(correctResult.equals(result(item)))) {
                throw new MetadataException(message);
            }
        }
    }

}
