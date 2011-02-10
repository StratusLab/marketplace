package eu.stratuslab.marketplace.server.resources;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;

import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This resource represents all races in the appliction
 */
public class EndorserResource extends BaseResource {
    
    private String email = null;

   @Override
    protected void doInit() throws ResourceException {
        this.email = (String) getRequest().getAttributes().get("email");
    } 

    /**
     * Returns a listing of endorser.
     */
    @Get("xml")
    public Representation toXml() {
        // Generate the right representation according to its media type.
        try {
            DomRepresentation representation = new DomRepresentation(
                    MediaType.TEXT_XML);

            Document d = representation.getDocument();
            Element r = d.createElement("endorser");
            d.appendChild(r);
            
            String queryString = "PREFIX slterms: <http://stratuslab.eu/terms#> " +
                                 "SELECT DISTINCT ?fullname " +
                                 " { _:z slterms:email \"" + this.email +"\" . " +
                                 " _:z slterms:full-name ?fullname }";

            ArrayList results = (ArrayList)query(queryString);

            Element eltName = d.createElement("full-name");
            String fullName = (String)(((HashMap)results.get(0))).get("fullname");
            eltName.appendChild(d.createTextNode(fullName));

            Element eltEmail = d.createElement("email");
            eltEmail.appendChild(d.createTextNode(email));

            r.appendChild(eltName);
            r.appendChild(eltEmail);                    
    
            d.normalizeDocument();

            // Returns the XML representation of this document.
            return representation;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
