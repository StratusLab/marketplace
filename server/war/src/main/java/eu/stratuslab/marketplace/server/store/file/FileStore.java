package eu.stratuslab.marketplace.server.store.file;

import org.w3c.dom.Document;

public abstract class FileStore {
	
	public abstract void store(String key, Document metadata);
	public abstract void remove(String key);
	public abstract String read(String key);

}
