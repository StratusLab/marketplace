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
public class MDataResource extends BaseResource {

    /**
     * Handle POST requests: create a new item.
     */
    @Post
    public Representation acceptMetadatum(Representation entity) throws IOException {
        Representation result = null;

        ModelMaker mk = ModelFactory.createMemModelMaker();
        Model datum = mk.createDefaultModel();
        datum.read(entity.getStream(), "");
        datum.setNsPrefix( "slterm", "http://stratuslab.eu/terms#" );
        datum.setNsPrefix( "dcterm", "http://purl.org/dc/terms/" );

        String identifier = ((datum.listStatements(
                              new SimpleSelector(null, DCTerms.identifier,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String endorser = ((datum.listStatements(
                              new SimpleSelector(null, datum.createProperty("http://stratuslab.eu/terms#", "email"),
                                                 (RDFNode)null))).nextStatement()).getObject().toString();
        String created = ((datum.listStatements(
                              new SimpleSelector(null, DCTerms.created,
                                                 (RDFNode)null))).nextStatement()).getObject().toString();

        String ref = getRequest().getResourceRef().toString();
        String iri = ref + "/" + identifier + "/" + endorser + "/" + created;

        storeMetadatum(iri, datum);
        // Set the response's status and entity
        setStatus(Status.SUCCESS_CREATED);
        Representation rep = new StringRepresentation("Metadata entry created",
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
            Element r = d.createElement("metadata");
            d.appendChild(r);

            String queryString = "PREFIX slterms: <http://stratuslab.eu/terms#> " +
                        "SELECT ?identifier ?endorser ?created ?description " +
                        "WHERE {" +
                        " ?y <http://purl.org/dc/terms/identifier> ?identifier . " +
                        " ?y <http://purl.org/dc/terms/description> ?description ." + 
                        " ?z slterms:email ?endorser . " +
                        " ?x <http://purl.org/dc/terms/created> ?created . }";
            
            ArrayList results = (ArrayList)query(queryString);

            for ( int i = 0; i < results.size(); i++ ){
                Element eltItem = d.createElement("entry");

                Element eltIdentifier = d.createElement("identifier");
                String identifier = (String)(((HashMap)results.get(i))).get("identifier");
                eltIdentifier.appendChild(d.createTextNode(identifier));
                eltItem.appendChild(eltIdentifier);

                Element eltEndorser = d.createElement("endorser");
                String endorser = (String)(((HashMap)results.get(i))).get("endorser");
                eltEndorser.appendChild(d.createTextNode(endorser));
                eltItem.appendChild(eltEndorser);

                Element eltCreated = d.createElement("created");
                String created = (String)(((HashMap)results.get(i))).get("created");
                eltCreated.appendChild(d.createTextNode(created));
                eltItem.appendChild(eltCreated);

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
