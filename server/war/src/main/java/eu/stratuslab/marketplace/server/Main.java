package eu.stratuslab.marketplace.server;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Protocol;



public class Main {

	/**
	 * @param args
	 * @throws Exception 
	 */
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
			System.err.println("\nStarting Marketplace FAILED!\n");
			System.exit(1);
		}
		System.out.println("\nMarketplace started!\n");


	}

}
