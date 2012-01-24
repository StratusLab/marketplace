package eu.stratuslab.marketplace.server.store;

import java.util.List;
import java.util.Map;

import eu.stratuslab.marketplace.server.MarketplaceException;

public abstract class RdfStore {
	
	public abstract void shutdown();
	public abstract void initialize();
    public abstract boolean store(String identifier, String entry);
    public abstract List<Map<String, String>> getRdfEntriesAsMap(String query) throws MarketplaceException;
    public abstract String getRdfEntriesAsString(String query) throws MarketplaceException;
    public abstract void remove(String identifier);
}
	
