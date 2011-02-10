package eu.stratuslab.marketplace.server.resources;

import org.restlet.resource.ServerResource;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.RDFHandler;

/**
 *  Base resource class that supports common behaviours or attributes shared by
 *  all resources.
 */
public abstract class BaseResource extends ServerResource {

    protected Logger logger = getLogger();

    /**
     * Returns the map of images managed by this application.
     * @return the map of images managed by this application.
    */
    protected Repository getMetadataStore(){
		return ((MarketPlaceApplication) getApplication()).getMetadataStore();
    }

    protected void storeMetadatum(String iri, Model rdf) {
        try {
            RepositoryConnection con = getMetadataStore().getConnection();
            ValueFactory vf = con.getValueFactory();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bytes);
            rdf.getWriter().write(rdf, out, "");

            try {
                con.add(new ByteArrayInputStream(bytes.toString().getBytes()), "", RDFFormat.RDFXML, 
                               vf.createURI(iri));
            }
            finally {
                con.close();
            }
        }
        catch (OpenRDFException e) {
            e.printStackTrace();
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    protected Model getMetadatum(String iri) {
        Model model = null;
        try {
            RepositoryConnection con = getMetadataStore().getConnection();
            ValueFactory vf = con.getValueFactory();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bytes);
            RDFHandler rdfxmlWriter = new RDFXMLWriter(out);
            
            try {
                con.export(rdfxmlWriter, vf.createURI(iri));
            }
            finally {
                con.close();
            }
            model = ModelFactory.createMemModelMaker().createDefaultModel();
            model.read(new ByteArrayInputStream(bytes.toString().getBytes()), "");
            model.setNsPrefix( "slterm", "http://stratuslab.eu/terms#" );
            model.setNsPrefix( "dcterm", "http://purl.org/dc/terms/" );
        }
        catch (OpenRDFException e) {
            e.printStackTrace();
        }
       
        return model;
    }

    protected void removeMetadatum(String iri) {
        try {
            RepositoryConnection con = getMetadataStore().getConnection();
            ValueFactory vf = con.getValueFactory();
            try {
                con.clear(vf.createURI(iri));
            }
            finally {
                con.close();
            }
        }
        catch (OpenRDFException e) {
            e.printStackTrace();
        }
    }
    
    protected Collection query(String queryString){
        ArrayList list = new ArrayList();

        try {
            RepositoryConnection con = getMetadataStore().getConnection();
            try {
                TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
                TupleQueryResult results = tupleQuery.evaluate();
                try {
                    List<String> columnNames = results.getBindingNames();
                    int cols = columnNames.size();

                    while(results.hasNext()){
		        BindingSet solution = results.next();
		        HashMap row = new HashMap(cols,1);
                        for ( Iterator<String> namesIter = columnNames.listIterator(); namesIter.hasNext(); ){
                           String columnName = namesIter.next();
                           row.put(columnName, (solution.getValue(columnName)).stringValue());
                        }
                        list.add(row);
                     }   
                  } finally {
                      results.close();
                  }
              }
              finally {
                  con.close();
              }
          }
          catch (OpenRDFException e) {
              e.printStackTrace();
          }

        return ((Collection) list); 
    }

}
