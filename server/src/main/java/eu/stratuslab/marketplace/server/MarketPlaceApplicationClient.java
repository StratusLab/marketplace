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
        ClientResource metadataResource = new ClientResource(url + "/metadata");
        ClientResource endorsersResource = new ClientResource(url + "/endorsers");

        // Create a new item
        Representation rdf = new FileRepresentation(metadata, MediaType.APPLICATION_RDF_XML);
        ModelMaker mk = ModelFactory.createMemModelMaker();
        Model image = mk.createDefaultModel();
        image.read(rdf.getStream(), "");

        /*
        String identifier = ((image.listStatements(
                              new SimpleSelector(null, DCTerms.identifier,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        
        String endorser = ((image.listStatements(
                              new SimpleSelector(null, image.createProperty("http://stratuslab.eu/terms#", "email"),
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String created = ((image.listStatements(
                              new SimpleSelector(null, DCTerms.created,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        */
        ClientResource metadatumResource = null;

        try {
            if(metadataResource != null){
                Representation r = metadataResource.post(rdf);
                metadatumResource = new ClientResource(r.getLocationRef());
            }
        } catch (ResourceException e) {
            System.out.println("Error  status: " + e.getStatus());
            System.out.println("Error message: " + e.getMessage());
        }

        // Consume the response's entity which releases the connection
        metadataResource.getResponseEntity().exhaust();

        if (metadatumResource != null) {
            // Prints the list of registered metadata.
            get(metadataResource);
            System.out.println();            

            // Prints a metadata entry.
            get(metadatumResource);
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
