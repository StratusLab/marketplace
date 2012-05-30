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
package eu.stratuslab.marketplace.server;

import java.util.logging.Logger;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Protocol;



public class Main {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");

	public static void main(String[] args) throws Exception {

		Component component = new Component();

		component.getServers().add(Protocol.HTTP, 8183);
//		component.getServers().add(Protocol.FILE);
		component.getClients().add(Protocol.HTTP);
		component.getClients().add(Protocol.HTTPS);
		component.getClients().add(Protocol.FILE);
		component.getClients().add(Protocol.CLAP);

		Application application = new MarketPlaceApplication();
		component.getDefaultHost().attach("", application);		
		
		try {
			component.start();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Starting Marketplace FAILED!");
			System.exit(1);
		}
		LOGGER.info("Marketplace started!");
	}

}
