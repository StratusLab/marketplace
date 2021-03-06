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
package eu.stratuslab.marketplace.server.resources;

import static org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND;

import org.restlet.Request;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class InvalidActionResource extends ServerResource {

    @Get("txt|html|xml")
    public Representation toError() {

        Request request = getRequest();
        Reference resourceRef = request.getResourceRef();

        String msg = String.format("invalid action: %s", resourceRef);

        throw new ResourceException(CLIENT_ERROR_NOT_FOUND, msg);
    }
}
