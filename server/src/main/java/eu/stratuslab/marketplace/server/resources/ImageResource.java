package eu.stratuslab.marketplace.server.resources;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.ext.xml.DomRepresentation;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This resource preprents an individual race instance
 */
public class ImageResource extends BaseResource {

    private String identifier;
    
    @Override
    protected void doInit() throws ResourceException {
        // Get the "identifier" attribute value taken from the URI template
        // /{identifier}.
        this.identifier = (String) getRequest().getAttributes().get("identifier");
    }
    
    @Get("xml")
    public Representation toXml() {
       try {
            DomRepresentation representation = new DomRepresentation(
                    MediaType.TEXT_XML);

             Document d = representation.getDocument();
            Element r = d.createElement("metadata");
            d.appendChild(r);

            String queryString = "PREFIX slterms: <http://stratuslab.eu/terms#> " +
                        "SELECT ?endorser ?created " +
                        "WHERE {" +
                        " ?y <http://purl.org/dc/terms/identifier> \"" + this.identifier + "\" . " +
                        " ?z slterms:email ?endorser . " +
                        " ?x <http://purl.org/dc/terms/created> ?created . }";

            ArrayList results = (ArrayList)query(queryString);

            for ( int i = 0; i < results.size(); i++ ){
                Element eltItem = d.createElement("entry");

                Element eltEndorser = d.createElement("endorser");
                String endorser = (String)(((HashMap)results.get(i))).get("endorser");
                eltEndorser.appendChild(d.createTextNode(endorser));
                eltItem.appendChild(eltEndorser);

                Element eltCreated = d.createElement("created");
                String created = (String)(((HashMap)results.get(i))).get("created");
                eltCreated.appendChild(d.createTextNode(created));
                eltItem.appendChild(eltCreated);

                r.appendChild(eltItem);
            }

            d.normalizeDocument();

            return representation;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; 
    }
  
}
