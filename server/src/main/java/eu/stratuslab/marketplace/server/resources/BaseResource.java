package eu.stratuslab.marketplace.server.resources;

import org.restlet.resource.ServerResource;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;

import org.restlet.data.Form;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.URI;

import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;


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
   
    protected String query(String queryString, String format){
        String resultString = null;

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bytes);
            SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);
        
            RepositoryConnection con = getMetadataStore().getConnection();
            try {
                TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
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

    
    protected String buildSelectQuery(Form form, String[] variables){
        Query query = QueryFactory.create();
        query.setQuerySelectType();
        
        ElementTriplesBlock triplePattern = new ElementTriplesBlock(); 

        for(int i = 0; i < variables.length; i++){        
            URI v = new URIImpl(variables[i]);
            String variable = v.getLocalName();
            String ns = v.getNamespace();
            
            if(form.getFirstValue(variable) == null){
                query.addResultVar(variable);
                triplePattern.addTriple(new Triple(Node.createAnon(),
                    Node.createURI(variables[i]),
                    Node.createVariable(variable)));
            } else {
                triplePattern.addTriple(new Triple(Node.createAnon(),
                    Node.createURI(variables[i]),
                    Node.createLiteral(form.getFirstValue(variable))));
            }
        }

        query.setQueryPattern(triplePattern);

        return query.toString();
    }

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
