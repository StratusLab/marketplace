package eu.stratuslab.marketplace.server.query;

import java.util.Map;

import eu.stratuslab.marketplace.PatternUtils;
import eu.stratuslab.marketplace.server.utils.MarketplaceUtils;

public class SparqlBuilder implements QueryBuilder {

	protected static final int ARG_EMAIL = 1;
	protected static final int ARG_DATE = 2;
	protected static final int ARG_OTHER = 3;
	
	private static final String CREATED = "created";
	private static final String IDENTIFIER = "identifier";
	private static final String EMAIL = "email";
	
	private static final String WHERE = " WHERE {";
	
	public String buildLatestEntryQuery(String identifier, String endorser) {
		String query = String.format(SparqlUtils.LATEST_ENTRY_QUERY_TEMPLATE, 
        		identifier, endorser);
		
		return query;
	}

	public String buildGetMetadataQuery(String deprecatedFlag,
			Map<String, String> requestQueryValues,
			Map<String, Object> requestAttributes) {
				
		StringBuilder dataQuery = new StringBuilder(SparqlUtils.SELECT_ALL);
		buildBaseQuery(dataQuery, deprecatedFlag,
				requestQueryValues, requestAttributes);
		
		//Build the paging query
		String paging = buildPagingFilter(requestQueryValues);
    	dataQuery.append(paging);
        
        return dataQuery.toString();
	}

	public String buildGetMetadataCountQuery(String deprecatedFlag,
			Map<String, String> requestQueryValues,
			Map<String, Object> requestAttributes) {
		
		StringBuilder countQuery = new StringBuilder(SparqlUtils.SELECT_COUNT);
        buildBaseQuery(countQuery, deprecatedFlag,
				requestQueryValues, requestAttributes);

        return countQuery.toString();        
    }
	
	private void buildBaseQuery(StringBuilder query, String deprecatedFlag,
			Map<String, String> requestQueryValues,
			Map<String, Object> requestAttributes){
		
		boolean hasDate = false;
	    
		addQueryParametersToRequest(requestAttributes, requestQueryValues);
		String filter = buildFilterFromUrlQuery(requestAttributes);
    	    	
    	if(filter.length() > 0){
    		if(filter.contains(CREATED)){
    			hasDate = true;
    		}
    	}
    	    	
    	//Build the paging query
    	String searching = buildSearchingFilter(requestQueryValues);
    	   	         
        String where = WHERE
            + SparqlUtils.WHERE_BLOCK;
        
        StringBuilder wherePredicate = new StringBuilder(
                    where);

        wherePredicate.append(filter);

        if (!hasDate) {
                wherePredicate
                        .append(SparqlUtils.getLatestFilter(MarketplaceUtils.getCurrentDate()));
        }
                     
        appendDeprecated(deprecatedFlag, wherePredicate);
                        
        wherePredicate.append(searching);
        wherePredicate.append(" }");

        query.append(wherePredicate);
    }
	
	private void addQueryParametersToRequest(Map<String, Object> attributes, Map<String, String> formValues) {
    	
		//Create filter from request parameters.    	    	 	
    	if(formValues.containsKey(IDENTIFIER)){
    		attributes.put(IDENTIFIER, formValues.get(IDENTIFIER));
    	}
    	if(formValues.containsKey(EMAIL)){
    		attributes.put(EMAIL, formValues.get(EMAIL));
    	}
    	if(formValues.containsKey(CREATED)){
    		attributes.put(CREATED, formValues.get(CREATED));
    	}   
    }
	
	private String buildFilterFromUrlQuery(Map<String, Object> attributes){
    	StringBuilder filter = new StringBuilder();
    	
    	for (Map.Entry<String, Object> arg : attributes.entrySet()) {
    		String key = arg.getKey();
    		if (!key.startsWith("org.restlet")) {
    			switch (classifyArg((String) arg.getValue())) {
    			case ARG_EMAIL:
    				filter.append(
    						SparqlUtils.buildFilterEq(EMAIL, (String)arg.getValue()));
    				break;
    			case ARG_DATE:
    				filter.append(
    						SparqlUtils.buildFilterEq(CREATED, (String)arg.getValue()));
    				break;
    			case ARG_OTHER:
    				filter.append(
    						SparqlUtils.buildFilterEq(IDENTIFIER, (String)arg.getValue()));
    				break;
    			default:
    				break;
    			}
    		}
    	}
    	
    	return filter.toString();
    }
    
    private int classifyArg(String arg) {
        if (arg == null || arg.equals("null") || arg.equals("")) {
            return -1;
        } else if (PatternUtils.isEmail(arg)) {
            return ARG_EMAIL;
        } else if (PatternUtils.isDate(arg)) {
            return ARG_DATE;
        } else {
            return ARG_OTHER;
        }
    }
	
    /*
     * Build a paging filter (ORDER BY ... LIMIT ...)
     * 
     * @return SPARQL LIMIT clause
     */
    private String buildPagingFilter(Map<String, String> requestQueryValues){
    	
    	String iDisplayStart = requestQueryValues.get("iDisplayStart");
		String iDisplayLength = requestQueryValues.get("iDisplayLength");
		String iSortCol = requestQueryValues.get("iSortCol_0");
		String sSortDir = requestQueryValues.get("sSortDir_0");   	
    	String paging = "";
    	
    	if(iDisplayStart != null && iDisplayLength != null){
    		int sort = (iSortCol != null) ? 
    				Integer.parseInt(iSortCol) : SparqlUtils.DEFAULT_SORT_COL;
    		String sortCol = SparqlUtils.getColumn(sort);
    		      		
    		paging = SparqlUtils.buildLimit(sortCol, iDisplayLength, iDisplayStart, sSortDir);
    	}
    	
    	return paging;
    }
             
    private String buildSearchingFilter(Map<String, String> requestQueryValues){
    	String sSearch = requestQueryValues.get("sSearch");
    	
    	String[] sSearchCols = new String[SparqlUtils.getColumnCount()];
    	
        for(int i = 0; i < SparqlUtils.getColumnCount();i++){
                sSearchCols[i] = requestQueryValues.get("sSearch_" + i);
        }
        
        String searching = buildSearchAllFilter(sSearch)
        					+ buildSearchColsFilter(sSearchCols);      
        
        return searching;
    }
           
    private String buildSearchAllFilter(String sSearch){
    	String searching = "";
    	
    	if(sSearch != null && sSearch.length() > 0){
            String[] searchTerms = sSearch.split(" ");
            StringBuilder searchAllFilter = new StringBuilder(" FILTER (");
            for(int i = 0; i < searchTerms.length; i++){

                    searchAllFilter.append("(");
                    for(int j = 1; j < SparqlUtils.getColumnCount(); j++){
                            searchAllFilter.append(SparqlUtils.buildRegex(
                                            SparqlUtils.getColumn(j), searchTerms[i]));

                            if(j < SparqlUtils.getColumnCount() - 1){
                                    searchAllFilter.append(" || ");
                            }
                    }
                    searchAllFilter.append(")");

                    if(i < searchTerms.length -1){
                                    searchAllFilter.append(" && ");
                    }
            }
            searchAllFilter.append(") . ");
            searching = searchAllFilter.toString();
    	}
    	
    	return searching;    	
    }
    
    private String buildSearchColsFilter(String[] sSearchCols) {
    	StringBuilder searchColumnsPredicate = new StringBuilder();
        
        for(int i = 1; i <= SparqlUtils.SEARCHABLE_COLUMNS; i++){
                if ( sSearchCols[i] != null && sSearchCols[i].length() > 0 )
                {
                        if ( searchColumnsPredicate.length() == 0 )
                        {
                                searchColumnsPredicate.append(" FILTER (");
                        }
                        else
                        {
                                searchColumnsPredicate.append(" && ");
                        }
                        String word = sSearchCols[i];
                        searchColumnsPredicate.append(SparqlUtils.buildRegex(
                                        SparqlUtils.getColumn(i), word));
                }
        }
        
        if(searchColumnsPredicate.length() != 0){
                searchColumnsPredicate.append(" ) . ");
        }
        
        return searchColumnsPredicate.toString();
    }
    
	public String buildGetTotalRecordsQuery(String deprecatedFlag) {
		String q = SparqlUtils.SELECT_COUNT
        + WHERE
        + SparqlUtils.WHERE_BLOCK
        + SparqlUtils.getLatestFilter(MarketplaceUtils.getCurrentDate());
    	
    	StringBuilder query = new StringBuilder(q);
    	
    	appendDeprecated(deprecatedFlag, query);
    	
    	query.append(" }");
    	
    	return query.toString();
	}
	
	private void appendDeprecated(String deprecated, StringBuilder filter) {
		if (deprecated.equals("off")){
        	filter.append(SparqlUtils.DEPRECATED_OFF);
        } else if (deprecated.equals("only")){
        	filter.append(SparqlUtils.DEPRECATED_ON);
        }
	}

	public String buildEndorserQuery(String email, String historyRange) {
		return String.format(SparqlUtils.ENDORSER_HISTORY_QUERY_TEMPLATE, email, historyRange);
	}
	
	public String buildEndorsersQuery(){
		return SparqlUtils.EMAIL_QUERY;
	}
	
	public String buildReminderQuery(String email){
		StringBuilder filterPredicate = new StringBuilder();

		filterPredicate.append(
				SparqlUtils.buildFilterEq("email", email));
		//Build the full SPARQL query
		StringBuilder query = new StringBuilder(SparqlUtils.SELECT_ALL);

		String where = WHERE + SparqlUtils.WHERE_BLOCK;
		
		StringBuilder filter = new StringBuilder(where);

		filter.append(filterPredicate.toString());
		filter
		.append(SparqlUtils.getLatestFilter(MarketplaceUtils.getCurrentDate()));

		filter.append(" " + SparqlUtils.DEPRECATED_OFF);
		filter.append(" }");

		query.append(filter);

		return query.toString();
	}
	
	public String buildExpiryQuery(String expiryDate){
		String select = "SELECT " +
		"?identifier ?email ?created ?valid";
		String where = WHERE + SparqlUtils.WHERE_BLOCK;
		
		//Build the full SPARQL query
		StringBuilder query = new StringBuilder(select);

		StringBuilder filter = new StringBuilder(
				where);

		filter
		.append(SparqlUtils.getLatestFilter(MarketplaceUtils.getCurrentDate()));
			
		filter.append(" FILTER (?valid < \"" + expiryDate + "\") .");
				
		filter.append(" " + SparqlUtils.DEPRECATED_OFF);
		filter.append(" }");

		query.append(filter);

		return query.toString();
	}
	
}
