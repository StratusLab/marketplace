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
package eu.stratuslab.marketplace.server.routers;

import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;

import eu.stratuslab.marketplace.server.resources.ActionResource;
import eu.stratuslab.marketplace.server.resources.InvalidActionResource;

public class ActionRouter extends Router {

    public ActionRouter() {
        super();

        // Routing is tolerant of extraneous leading and trailing slashes.
        // TODO: Determine a better mechanism for being tolerant of slashes.
        TemplateRoute route;

        route = attach("/{uuid}/{command}/", ActionResource.class);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

        route = attach("/{uuid}/{command}", ActionResource.class);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

        route = attach("{uuid}/{command}/", ActionResource.class);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

        route = attach("{uuid}/{command}", ActionResource.class);
        route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);

        attachDefault(InvalidActionResource.class);
    }

}
