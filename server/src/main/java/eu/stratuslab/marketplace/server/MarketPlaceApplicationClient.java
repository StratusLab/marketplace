package eu.stratuslab.marketplace.server;

import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

public class MarketPlaceApplicationClient {

    public static void main(String[] args) throws IOException,
            ResourceException {
        //get args
        String url = "";
        String metadata = "";
        MediaType mediaType = MediaType.APPLICATION_RDF_XML;
        
        if (args.length != 2 && args.length != 3){
		System.out.println("Usage: MarketPlaceApplicationClient <URL> <Metadata> <rdf|json>");
                System.exit(-1);
        } else {
                url = args[0];
                metadata = args[1];
                if(args.length == 3){
                	String format = args[2];
                	if(format.equals("json")){
                		mediaType = MediaType.APPLICATION_JSON;
                	} else if (format.equals("rdf")){
                		mediaType = MediaType.APPLICATION_RDF_XML;
                	} else if (format.equals("html")){
                		mediaType = MediaType.TEXT_HTML;
                	}
                }
        }

        // Define our Restlet client resources.
        ClientResource metadataResource = new ClientResource(url + "/metadata");
        ClientResource endorsersResource = new ClientResource(url + "/endorsers");

        // Create a new item
        Representation rdf = new FileRepresentation(metadata, MediaType.APPLICATION_RDF_XML);
               
        ClientResource metadatumResource = null;

        try {
            if(metadataResource != null){
                Representation r = metadataResource.post(rdf);
                metadataResource.getResponseEntity().write(System.out);
                metadatumResource = new ClientResource(r.getLocationRef());
            }
        } catch (ResourceException e) {
            System.out.println("Error  status: " + e.getStatus());
            System.out.println("Error message: " + e.getMessage());
            if(metadataResource.getResponseEntity().isAvailable())
            	metadataResource.getResponseEntity().write(System.out);
        }
                               
        // Consume the response's entity which releases the connection
        metadataResource.getResponseEntity().exhaust();

        if (metadatumResource != null) {
            // Prints the list of registered metadata.
            get(metadataResource);
            System.out.println();            

            // Prints a metadata entry.
            get(metadatumResource, mediaType);
            System.out.println();
            
            // Prints the current list of endorsers
            get(endorsersResource);
            System.out.println();
       }

}

    /**
     * Prints the resource's representation.
     * 
     * @param clientResource
     *            The Restlet client resource.
     * @throws IOException
     * @throws ResourceException
     */
    public static void get(ClientResource clientResource) throws IOException,
            ResourceException {
    	clientResource.get();  
    	if (clientResource.getStatus().isSuccess()  
    			&& clientResource.getResponseEntity().isAvailable()) {  
    		clientResource.getResponseEntity().write(System.out);  
    	}
    }
    
    /**
     * Prints the resource's representation.
     * 
     * @param clientResource
     *            The Restlet client resource.
     * @throws IOException
     * @throws ResourceException
     */
    public static void get(ClientResource clientResource, MediaType type) throws IOException,
            ResourceException {
    	clientResource.get(type);  
    	if (clientResource.getStatus().isSuccess()  
    			&& clientResource.getResponseEntity().isAvailable()) {  
    		clientResource.getResponseEntity().write(System.out);  
    	}
    }

}
