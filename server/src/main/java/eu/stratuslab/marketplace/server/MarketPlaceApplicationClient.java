package eu.stratuslab.marketplace.server;

import java.io.IOException;
import java.io.File;

import org.restlet.representation.Representation;
import org.restlet.representation.FileRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.data.MediaType;

public class MarketPlaceApplicationClient {

    public static void main(String[] args) throws IOException,
            ResourceException {
        //get args
        String url = "";
        String identifier = "";
        String metadata = "";

        if (args.length != 3){
		System.out.println("Usage: MarketPlaceApplicationClient <URL> <Identifier> <Metadata>");
                System.exit(-1);
        } else {
                url = args[0];
                identifier = args[1];
                metadata = args[2];
        }

        // Define our Restlet client resources.
        ClientResource imagesResource = new ClientResource(url + "/images");
        ClientResource imageResource = new ClientResource(url + "/images/" + identifier);

        // Create a new item
        Representation rdf = new FileRepresentation(metadata, MediaType.APPLICATION_RDF_XML);
        
        try {
            if(imageResource != null){
                imageResource.put(rdf);
            }
        } catch (ResourceException e) {
            System.out.println("Error  status: " + e.getStatus());
            System.out.println("Error message: " + e.getMessage());
        }
        // Consume the response's entity which releases the connection
        imageResource.getResponseEntity().exhaust();

        if (imageResource != null) {
            // Prints the representation of the newly created resource.
            get(imageResource);

            // Prints the list of registered images.
            get(imagesResource);
            
            // delete the image
            imageResource.delete();

            //Print the list of registered items.
            get(imagesResource);
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
        try {
            clientResource.get().write(System.out);
        } catch (ResourceException e) {
            System.out.println("Error  status: " + e.getStatus());
            System.out.println("Error message: " + e.getMessage());
            // Consume the response's entity which releases the connection
            clientResource.getResponseEntity().exhaust();
        }
    }

}
