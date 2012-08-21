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

import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.util.Scanner;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import eu.stratuslab.marketplace.XMLUtils;
import eu.stratuslab.marketplace.metadata.MetadataUtils;
import eu.stratuslab.marketplace.server.cfg.Configuration;

public final class MetadataFileUtils {

	private static final String ENCODING = "UTF-8";
	
	private MetadataFileUtils(){}
	
	public static File writeContentsToDisk(Representation entity) {

        char[] buffer = new char[4096];

        File storeDirectory = Configuration
                .getParameterValueAsFile(PENDING_DIR);

        File output = new File(storeDirectory, UUID.randomUUID().toString());

        Reader reader = null;
        Writer writer = null;

        try {
        	reader = Channels.newReader(entity.getChannel(), ENCODING);
        	writer = new OutputStreamWriter(
            		new FileOutputStream(output), ENCODING);

            int nchars = reader.read(buffer);
            while (nchars >= 0) {
                writer.write(buffer, 0, nchars);
                nchars = reader.read(buffer);
            }

        } catch (IOException consumed) {

        } finally {
            closeReliably(reader);
            closeReliably(writer);
        }
        return output;
    }
	
	public static String readFileAsString(String filePath)
	throws IOException {
		
		StringBuilder text = new StringBuilder();
	    String NL = System.getProperty("line.separator");
	    Scanner scanner = new Scanner(new FileInputStream(filePath), 
	    		ENCODING);
	    try {
	      while (scanner.hasNextLine()){
	        text.append(scanner.nextLine() + NL);
	      }
	    }
	    finally{
	      scanner.close();
	    }
	    
	    return text.toString();
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
                    .getBytes(ENCODING)));

            // Create a deep copy of the document and strip signature elements.
            Document copy = (Document) datumDoc.cloneNode(true);
            MetadataUtils.stripSignatureElements(copy);
            rdfEntry = XMLUtils.documentToString(copy);
        } catch (SAXException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Unable to parse metadata: " + e.getMessage(), e);
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
                    "unable to parse metadata: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ResourceException(e);
        }

        return datumDoc;
    }
	
	public static boolean createIfNotExists(String path) {
		
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return false;
            }
        }
        
        return true;
    }
}
