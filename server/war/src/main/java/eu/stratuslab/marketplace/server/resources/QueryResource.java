/**
 * Created as part of the StratusLab project (http://stratuslab.eu),
 * co-funded by the European Commission under the Grant Agreement
 * INSFO-RI-261552.
 *
 * Copyright (c) 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratuslab.marketplace.server.resources;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.MARKETPLACE_URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserUtil;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.lang.ParserSPARQL11;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;

import eu.stratuslab.marketplace.server.MarketplaceException;

/**
 * This resource represents a query on the rdf data
 */
public class QueryResource extends BaseResource {
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	@SuppressWarnings("unchecked")
	@Get("html")
    public Representation toHtml() {
        Representation representation = null;

        try {
        	SparqlQuery query = new SparqlQuery(
        			getQueryFromRequest(), 
        			SparqlQuery.OUTPUT_MAP);
        	
        	List<Map<String, String>> results = (List<Map<String, String>>)executeQuery(query);
        	
			// return representation;
			Map<String, Object> queryMap = createInfoStructure("Query");
			queryMap.put("query", query.getQuery());
			queryMap.put(
					"results",
					(results == null) ? new ArrayList<List<Map<String, String>>>()
							: results);
            
        	representation = createTemplateRepresentation("query.ftl", queryMap,
                    MediaType.TEXT_HTML);
        	
        } catch (MalformedQueryException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e
                    .getMessage(), e);
        }
        
        return representation;
    }

	private String getQueryFromRequest(){
		Form form = getRequest().getResourceRef().getQueryAsForm();
		String query = form.getFirstValue("query");

		return query;
	}
	
   	@Get("xml")
    public Representation toXml() {
		Representation representation = null;

        try {
        	SparqlQuery query = new SparqlQuery(
        			getQueryFromRequest(), 
        			SparqlQuery.OUTPUT_XML);
        	
        	String results = (String)executeQuery(query);
        	if(results == null){
        		results = "";
        	}
        	
            representation = new StringRepresentation(results,
                    MediaType.APPLICATION_SPARQL_RESULTS_XML);
        } catch (MalformedQueryException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e
                    .getMessage(), e);
        }
        
        return representation;	
    }
	
	@Get("json")
    public Representation toJson() {
        Representation representation = null;

        try {
        	SparqlQuery query = new SparqlQuery(
        			getQueryFromRequest(), 
        			SparqlQuery.OUTPUT_JSON);
        	
        	String results = (String)executeQuery(query);
        	if(results == null){
        		results = "";
        	}
        	        	        	
            representation = new StringRepresentation(results,
                    MediaType.APPLICATION_SPARQL_RESULTS_JSON);
        } catch (MalformedQueryException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e
                    .getMessage(), e);
        }
        
        return representation;
    }
       
	private Object executeQuery(SparqlQuery query){
		//check that query is allowed.
    	if(!query.isAllowed()){
    		throw new ResourceException(
    				Status.CLIENT_ERROR_BAD_REQUEST,
    				"query not allowed.");
    	}
         
    	Object results = null;
		try {
			results = getFormattedQueryResults(query);
		} catch (MarketplaceException e) {
			LOGGER.severe(e.getMessage());
		}
		
		return results;
	}
	
	private Object getFormattedQueryResults(SparqlQuery query) 
	throws MarketplaceException {
		Object results = null;
		
		if (!query.isEmpty()) {
			switch (query.getOutputFormat()) {
			case SparqlQuery.OUTPUT_JSON:
				results = queryResultsAsJson(query.getQuery());
				break;

			case SparqlQuery.OUTPUT_XML:
				results = queryResultsAsXml(query.getQuery());
				break;
				
			case SparqlQuery.OUTPUT_MAP:
				results = query(query.getQuery());
				break;
				
			default:
				results = null;
				break;
				
			}
		}
		
		return results;
	}
	
	static class SparqlQuery {	

		public static final int OUTPUT_MAP = 0;
		private static final int OUTPUT_XML = 1;
		private static final int OUTPUT_JSON = 2;
		
		private String query = "";
		private boolean allowed = true;
		private boolean empty = true;
		
		private int outputFormat;
		
		SparqlQuery(String query, int outputFormat) throws MalformedQueryException {
			this.query = query;
			this.outputFormat = outputFormat;
			
			if(query == null){
				this.query = "";
				this.empty = true;
			} else if (!"".equals(query)) {
				this.empty = false;
				isQueryWellFormed();
				isQueryAllowed();
			}
			
		}

		private void isQueryAllowed() {
			ParserSPARQL11 jenaParser = new ParserSPARQL11();
			Query select = jenaParser.parse(new Query(), query);

			if(!select.isSelectType() || 
					select.hasDatasetDescription()){
				this.allowed = false;
			} else {
				Element queryElement = select.getQueryPattern();
				if(queryElement.getClass() == ElementGroup.class){
					List<Element> elements = ((ElementGroup)queryElement).getElements();
					for(Element e: elements){
						if(e.getClass() == ElementNamedGraph.class){
							this.allowed = false;
						}
					}
				}
			}
		}
		
		private void isQueryWellFormed() 
			throws MalformedQueryException{
			QueryParser parser = QueryParserUtil
				.createParser(QueryLanguage.SPARQL);

				parser.parseQuery(query, MARKETPLACE_URI);
		}
		
		public int getOutputFormat(){
			return this.outputFormat;
		}
		
		public String getQuery(){
			return this.query;
		}
		
		public boolean isAllowed() {
			return this.allowed;
		}
						
		public boolean isEmpty(){
			return this.empty;
		}
		
	}

}
