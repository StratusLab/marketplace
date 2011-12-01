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
