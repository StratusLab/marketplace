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

import java.util.Map;

import org.restlet.Request;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ActionResource extends ServerResource {

    private String metadata;

    private String command;

    @Override
    protected void doInit() throws ResourceException {

        Request request = getRequest();

        Map<String, Object> attributes = request.getAttributes();

        String uuid = attributes.get("uuid").toString();
        command = attributes.get("command").toString();

        metadata = retrieveMetadata(uuid);

    }

    @Get("txt")
    public Representation toText() {
        return doAction(getRequest());
    }

    private Representation doAction(Request request) {
        // FIXME: Add correct action here; need to provide error returns.
        String msg = command + " " + metadata;
        Representation representation = new StringRepresentation(msg);
        return representation;
    }

    private String retrieveMetadata(String uuid) {
        // FIXME: Add implementation to retrieve metadata from disk.
        return "fake metadata";
    }
}
