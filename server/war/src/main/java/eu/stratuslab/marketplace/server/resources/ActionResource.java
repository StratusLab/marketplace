/*
 Created as part of the StratusLab project (http://stratuslab.eu),
 co-funded by the European Commission under the Grant Agreement
 INSFO-RI-261552.

 Copyright (c) 2011, SixSq Sarl

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.server.cfg.Parameter.PENDING_DIR;
import static org.restlet.data.MediaType.TEXT_PLAIN;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;

import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.cfg.Configuration;

public class ActionResource extends BaseResource {

    private static final Logger LOGGER = Logger.getLogger("org.restlet");

    private Document doc;

    private String uuid;

    private String command;

    @Override
    protected void doInit() {

        Request request = getRequest();

        Map<String, Object> attributes = request.getAttributes();

        uuid = attributes.get("uuid").toString();
        command = attributes.get("command").toString();

        doc = retrieveMetadata(uuid);

    }

    @Get("txt")
    public Representation toText() {
        return doAction();
    }

    private Representation doAction() {

        Representation representation = null;

        if ("confirm".equals(command)) {
            representation = confirmEntry();
        } else if ("abort".equals(command)) {
            representation = abortEntry();
        } else if ("abuse".equals(command)) {
            representation = reportAbuse();
        }

        return representation;
    }

    private Representation confirmEntry() {
        File uploadedFile = getUploadedFile(uuid);

        String iri = commitMetadataEntry(uploadedFile, doc);

        setStatus(Status.SUCCESS_CREATED);
        Representation rep = new StringRepresentation(
                "metadata entry created\n", TEXT_PLAIN);
        rep.setLocationRef(getRequest().getResourceRef().getIdentifier() + iri);

        return rep;

    }

    private Representation abortEntry() {
        // The file was deleted from disk when it was read, so no cleanup is
        // necessary here.
        return new StringRepresentation("aborted addition of metadata entry "
                + uuid, MediaType.TEXT_PLAIN);
    }

    private Representation reportAbuse() {
        // FIXME: This needs to be logged with an email sent.
        LOGGER.severe("abuse reported:" + uuid);
        return new StringRepresentation(
                "administrators have been notified of the problem and may contact you during the investigation",
                MediaType.TEXT_PLAIN);
    }

    private static File getUploadedFile(String uuid) {
        String dir = Configuration.getParameterValue(PENDING_DIR);
        return new File(dir, uuid);
    }

    private static Document retrieveMetadata(String uuid) {

        InputStream stream = null;

        try {

            File file = getUploadedFile(uuid);

            stream = new FileInputStream(file);
            Document doc = extractXmlDocument(stream);

            if (!file.delete()) {
                LOGGER.severe("cannot delete file: " + file);
            }
            return doc;

        } catch (IOException e) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e
                    .getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException consumed) {
                }
            }
        }
    }
}
