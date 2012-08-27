package eu.stratuslab.marketplace.server.store.rdf.sesame;

import org.openrdf.sail.helpers.SailBase;

public interface SesameBackend {
	
	SailBase getSailBase();
	boolean keepAlive();
	
}
