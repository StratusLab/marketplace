package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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
import org.restlet.data.Form;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import eu.stratuslab.marketplace.PatternUtils;
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
    
    protected static final int ARG_EMAIL = 1;
    protected static final int ARG_DATE = 2;
    protected static final int ARG_OTHER = 3;

    protected final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
    
    /**
     * Returns the store of metadata managed by this application.
     * 
     * @return the store of metadata managed by this application.
    */
    protected Repository getMetadataStore(){
		return ((MarketPlaceApplication) getApplication()).getMetadataStore();
    }

    protected String getDataDir(){
    	return ((MarketPlaceApplication) getApplication()).getDataDir();
    }
    
    /**
     * Stores a new metadata entry
     * 
     * @param iri identifier of the metadata entry
     * @param rdf the metadata entry to store
     */
    protected void storeMetadatum(String iri, String rdf) {
        try {
            RepositoryConnection con = getMetadataStore().getConnection();
            ValueFactory vf = con.getValueFactory();
            Reader reader = new StringReader(rdf);
            try {
            	con.clear(vf.createURI(iri));
                con.add(reader, MARKETPLACE_URI, RDFFormat.RDFXML, 
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
    protected String getMetadatum(String iri) {
        String model = null;
        try {
            model = readFileAsString(iri);
        }
        catch (IOException e) {
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
    protected String query(String queryString, QueryLanguage syntax){
        String resultString = null;

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(bytes);
            SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);
        
            RepositoryConnection con = getMetadataStore().getConnection();
            try {
                TupleQuery tupleQuery = con.prepareTupleQuery(syntax, queryString);
                tupleQuery.evaluate(sparqlWriter);
                resultString = bytes.toString();
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

        return (list); 
    }
    
    protected StringBuffer formToString(Form form){
        StringBuffer predicate = new StringBuffer();
        
        for ( Iterator<String> formIter = form.getNames().iterator(); formIter.hasNext(); ){
        	String key = formIter.next();
        	predicate.append(" FILTER (?" + key + " = \"" + form.getFirstValue(key) + "\" ). ");
        }
        
        return predicate;
        
    }
    
    protected int classifyArg(String arg){
    	if(arg == null || arg.equals("null") || arg.equals("")){
    		return -1;
    	}else if(PatternUtils.isEmail(arg)){
    		return ARG_EMAIL;
    	} else if (PatternUtils.isDate(arg)){
    	    return ARG_DATE;
    	} else {
    		return ARG_OTHER;
    	}
    }
        
    protected String extractTextContent(Document doc, String namespace, String name) {
        NodeList nl = doc.getElementsByTagNameNS(namespace, name);
        if (nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        return null;
    }
      
    private static String readFileAsString(String filePath) throws java.io.IOException{
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(new File(filePath)));
            f.read(buffer);
        } finally {
            if (f != null) try { f.close(); } catch (IOException ignored) { }
        }
        return new String(buffer);
    }

}
