package eu.stratuslab.marketplace.server.store;

public class RdfStoreFactoryImpl implements RdfStoreFactory {
	public RdfStore createRdfStore(String provider, String type){
		//if(provider.equals("sesame")){
			return new SesameRdfStore(type);
		//}
	}

}
