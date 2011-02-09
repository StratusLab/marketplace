package eu.stratuslab.marketplace.server.resources;

import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.data.Status;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.vocabulary.DCTerms;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;

/**
 * This resource represents all races in the appliction
 */
public class ImagesResource extends BaseResource {

    /**
     * Handle POST requests: create a new item.
     */
    @Post
    public Representation acceptItem(Representation entity) throws IOException {
        Representation result = null;

        ModelMaker mk = ModelFactory.createMemModelMaker();
        Model image = mk.createDefaultModel();
        image.read(entity.getStream(), "");
        image.setNsPrefix( "slterm", "http://stratuslab.eu/terms#" );
        image.setNsPrefix( "dcterm", "http://purl.org/dc/terms/" );

        String identifier = ((image.listStatements(
                              new SimpleSelector(null, DCTerms.identifier,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String endorser = ((image.listStatements(
                              new SimpleSelector(null, image.createProperty("http://stratuslab.eu/terms#", "email"),
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String created = ((image.listStatements(
                              new SimpleSelector(null, DCTerms.created,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();

        String ref = getRequest().getResourceRef().toString();
        String iri = ref + "/" + identifier + "/" + endorser + "/" + created;

        storeImage(iri, image);
        // Set the response's status and entity
        setStatus(Status.SUCCESS_CREATED);
        Representation rep = new StringRepresentation("Image created",
            MediaType.TEXT_PLAIN);
        // Indicates where is located the new resource.
        rep.setLocationRef(getRequest().getResourceRef().getIdentifier() + "/"
             + identifier + "/" + endorser + "/" + created);
        result = rep;

        return result;
    }
    
    /**
     * Returns a listing of all registered images.
     */
    @Get("xml")
    public Representation toXml() {
        // Generate the right representation according to its media type.
        try {
            DomRepresentation representation = new DomRepresentation(
                    MediaType.TEXT_XML);

            // Generate a DOM document representing the list of
            // items.
            Document d = representation.getDocument();
            Element r = d.createElement("images");
            d.appendChild(r);

            String queryString = "PREFIX slterms: <http://stratuslab.eu/terms#> " +
                        "SELECT ?identifier ?description " +
                        "WHERE {" +
                        " ?y <http://purl.org/dc/terms/identifier> ?identifier . " +
                        " ?y <http://purl.org/dc/terms/description> ?description . }";
            
            ArrayList results = (ArrayList)query(queryString);

            for ( int i = 0; i < results.size(); i++ ){
                Element eltItem = d.createElement("image");

                Element eltIdentifier = d.createElement("identifier");
                String identifier = (String)(((HashMap)results.get(i))).get("identifier");
                eltIdentifier.appendChild(d.createTextNode(identifier));
                eltItem.appendChild(eltIdentifier);
        
                Element eltDescription = d.createElement("description");
                String description = (String)(((HashMap)results.get(i))).get("description");
                eltDescription.appendChild(d.createTextNode(description));
                eltItem.appendChild(eltDescription);

                r.appendChild(eltItem);
            }
 
            d.normalizeDocument();

            // Returns the XML representation of this document.
            return representation;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
