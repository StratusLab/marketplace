package eu.stratuslab.marketplace.server;

import java.io.IOException;
import java.io.File;

import org.restlet.representation.Representation;
import org.restlet.representation.FileRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.data.MediaType;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class MarketPlaceApplicationClient {

    public static void main(String[] args) throws IOException,
            ResourceException {
        //get args
        String url = "";
        String metadata = "";

        if (args.length != 2){
		System.out.println("Usage: MarketPlaceApplicationClient <URL> <Metadata>");
                System.exit(-1);
        } else {
                url = args[0];
                metadata = args[1];
        }

        // Define our Restlet client resources.
        ClientResource imagesResource = new ClientResource(url + "/images");
        ClientResource imageResource = null;

        // Create a new item
        Representation rdf = new FileRepresentation(metadata, MediaType.APPLICATION_RDF_XML);
        ModelMaker mk = ModelFactory.createMemModelMaker();
        Model image = mk.createDefaultModel();
        image.read(rdf.getStream(), "");

        Statement state = (image.listStatements(
                  new SimpleSelector(null,
                  ResourceFactory.createProperty("http://stratuslab.eu/terms#", "email"),
                         (RDFNode)null))).nextStatement();
        String endorser = state.getObject().toString();
        System.out.println("Endorser:  " + endorser);

        Statement id = (image.listStatements(
                   new SimpleSelector(null, DCTerms.identifier,
                         (RDFNode)null))).nextStatement();
        String identifier = id.getObject().toString();
        System.out.println("Identifier:  " + identifier);
        
        try {
            if(imagesResource != null){
                Representation r = imagesResource.post(rdf);
                imageResource = new ClientResource(r.getLocationRef());
            }
        } catch (ResourceException e) {
            System.out.println("Error  status: " + e.getStatus());
            System.out.println("Error message: " + e.getMessage());
        }
        // Consume the response's entity which releases the connection
        imagesResource.getResponseEntity().exhaust();

        if (imageResource != null) {
            // Prints the representation of the newly created resource.
            get(imageResource);
            System.out.println();

            // Prints the list of registered images.
            get(imagesResource);
            System.out.println();            

            // delete the image
            //imageResource.delete();

            //Print the list of registered items.
            //get(imagesResource);
            //System.out.println();
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
