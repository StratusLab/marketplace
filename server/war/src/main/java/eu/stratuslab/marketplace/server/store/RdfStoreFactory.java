package eu.stratuslab.marketplace.server.store;

public interface RdfStoreFactory {
	public RdfStore createRdfStore(String provider, String type);
}
