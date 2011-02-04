package eu.stratuslab.marketplace.server.resources;

import org.restlet.resource.ServerResource;

import eu.stratuslab.marketplace.server.MarketPlaceApplication;

import com.hp.hpl.jena.query.DataSource;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QuerySolution;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import com.hp.hpl.jena.sdb.Store;
import com.hp.hpl.jena.sdb.SDBFactory;
import com.hp.hpl.jena.sdb.store.DatasetStore;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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
    protected Store getImageStore(){
		return ((MarketPlaceApplication) getApplication()).getImageStore();
    }

    protected void storeImage(String iri, String rdf) {
        Model sdbModel = SDBFactory.connectNamedModel(getImageStore(), iri);
        sdbModel.read(new ByteArrayInputStream(rdf.getBytes()), "");
        sdbModel.close();
        getImageStore().close();
    }

    protected DataSource getImages() {
        Dataset ds = DatasetStore.create(getImageStore());
        DataSource images = DatasetFactory.create(ds);
        images.setDefaultModel((ModelFactory.createMemModelMaker()).createDefaultModel());
 
        return images;
    }

    protected Collection query(String queryString, Dataset data){
        QueryExecution qe = QueryExecutionFactory.create(queryString, data);
        ResultSet results = qe.execSelect();

        List<String> columnNames = results.getResultVars();
        int cols = columnNames.size();

        ArrayList list = new ArrayList();

        while(results.hasNext()){
		QuerySolution solution = results.next();
		HashMap row = new HashMap(cols,1);
                for ( Iterator<String> namesIter = columnNames.listIterator(); namesIter.hasNext(); ){
                   String columnName = namesIter.next();
                   row.put(columnName,
                          (solution.getLiteral(columnName)).getString());
                }
                list.add(row);
        } 

        //qe.close();
        //data.close();
        //getImageStore().close();

        return ((Collection) list); 
    }
}
