package eu.stratuslab.marketplace.server.resources;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.openrdf.OpenRDFException;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.restlet.resource.ServerResource;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;


/**
 *  Base resource class that supports common behaviours or attributes shared by
 *  all resources.
 */
/**
 * @author stkenny
 *
 */
public abstract class BaseResource extends ServerResource {

    protected Logger logger = getLogger();

    /**
     * Returns the store of metadata managed by this application.
     * 
     * @return the store of metadata managed by this application.
    */
    protected Repository getMetadataStore(){
		return ((MarketPlaceApplication) getApplication()).getMetadataStore();
    }

    /**
     * Stores a new metadata entry
     * 
     * @param iri identifier of the metadata entry
     * @param rdf the metadata entry to store
     */
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

    /**
     * Retrieve a particular metadata entry
     * 
     * @param iri identifier of the metadata entry
     * @return metadata entry as a Jena model
     */
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

    /**
     * Remove a metadata entry
     * 
     * @param iri identifier of the metadata entry
     */
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
   
    
    /**
     * Query the metadata
     * 
     * @param queryString query string
     * @param syntax the syntax of the query
     * @param format the output format of the resultset
     * @return
     */
    protected String query(String queryString, QueryLanguage syntax, String format){
        String resultString = null;

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bytes);
            SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);
        
            RepositoryConnection con = getMetadataStore().getConnection();
            try {
                TupleQuery tupleQuery = con.prepareTupleQuery(syntax, queryString);
                tupleQuery.evaluate(sparqlWriter);
                resultString = convertString(bytes.toString(), format);
            } finally {
                con.close();
            }
        }
        catch (OpenRDFException e) {
            e.printStackTrace();
        }

        return resultString;
    }
 
    /**
     * Query the metadata
     * 
     * @param queryString the query
     * @return the resultset as a Java Collection
     */
    protected Collection<HashMap<String, String>> query(String queryString){
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

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
		        HashMap<String, String> row = new HashMap<String, String>(cols,1);
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

        return ((Collection<HashMap<String, String>>) list); 
    }
    
    /**
     * Convert a Jena model to an RDF String
     * 
     * @param model the model to convert
     * @return the model as a String in RDF format
     */
    protected String modelToString(Model model){
    	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(bytes);

        RDFWriter writer = model.getWriter();
        writer.write(model, out, "");
        
        return bytes.toString();
    }
    
    /**
     * Convert a String to various output formats
     * 
     * @param toConvert the string to convert
     * @param format the output format
     * @return the String in the chosen format
     */
    private String convertString(String toConvert, String format){
        String resultString = "";

        if(format == null || format.equals("sse")){
            resultString = toConvert;
        } else if (format.equals("xml")) {
            ResultSet results = ResultSetFactory.fromXML(
                 new ByteArrayInputStream(toConvert.getBytes()));
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            BufferedOutputStream o = new BufferedOutputStream(b);
            ResultSetFormatter.outputAsXML(o,results);
            resultString = b.toString();
        } else if (format.equals("rdf")) {
             ResultSet results = ResultSetFactory.fromXML(
                 new ByteArrayInputStream(toConvert.getBytes()));
             ByteArrayOutputStream b = new ByteArrayOutputStream();
             BufferedOutputStream o = new BufferedOutputStream(b);
             ResultSetFormatter.outputAsRDF(o, "RDF/XML", results);
             resultString = b.toString();
        } else if (format.equals("json")){
             ResultSet results = ResultSetFactory.fromXML(
                 new ByteArrayInputStream(toConvert.getBytes()));
             ByteArrayOutputStream b = new ByteArrayOutputStream();
             BufferedOutputStream o = new BufferedOutputStream(b);
             ResultSetFormatter.outputAsJSON(o, results);
             resultString = b.toString();
        }

        return resultString;
    }

}
