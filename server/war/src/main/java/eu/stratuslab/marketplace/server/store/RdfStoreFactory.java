package eu.stratuslab.marketplace.server.store;

public interface RdfStoreFactory {
	
	public static final String SESAME_PROVIDER = "sesame";
	public static final String MYSQL_BACKEND = "mysql";
	public static final String POSTGRESQL_BACKEND = "postgres";
	public static final String MEMORY_BACKEND = "memory";
	
	public RdfStore createRdfStore(String provider, String type);
}
