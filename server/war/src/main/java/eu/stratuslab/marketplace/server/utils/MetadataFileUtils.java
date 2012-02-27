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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.metadata.MetadataUtils;

public class MetadataFileUtils {

	public static String readFileAsString(String filePath)
	throws IOException {

		File file = new File(filePath);
		int bytes = (int) file.length();

		byte[] buffer = new byte[bytes];

		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(new FileInputStream(file));
			int remaining = bytes;
			int offset = 0;
			while (remaining > 0) {
				int readBytes = f.read(buffer, offset, remaining);
				offset += readBytes;
				remaining -= readBytes;
			}
		} finally {
			closeReliably(f);
		}
		return new String(buffer);
	}

	public static void closeReliably(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException consumed) {
			}
		}
	}
	
	public static String stripSignature(String signedString) {
        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document datumDoc = null;
        String rdfEntry = "";
        try {
            datumDoc = db.parse(new ByteArrayInputStream(signedString
                    .getBytes("UTF-8")));

            // Create a deep copy of the document and strip signature elements.
            Document copy = (Document) datumDoc.cloneNode(true);
            MetadataUtils.stripSignatureElements(copy);
            rdfEntry = XMLUtils.documentToString(copy);
        } catch (SAXException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Unable to parse metadata: " + e.getMessage());
        } catch (IOException e) {
            throw new ResourceException(e);
        }

        return rdfEntry;
    }

	public static Document extractXmlDocument(InputStream stream) {

        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document datumDoc = null;

        try {

            datumDoc = db.parse(stream);

        } catch (SAXException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "unable to parse metadata: " + e.getMessage());
        } catch (IOException e) {
            throw new ResourceException(e);
        }

        return datumDoc;
    }
}
