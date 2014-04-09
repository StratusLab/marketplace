package eu.stratuslab.marketplace.server.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.params.GroupParams;

import eu.stratuslab.marketplace.PatternUtils;

public class SolrBuilder implements QueryBuilder {

	protected static final int ARG_EMAIL = 1;
	protected static final int ARG_DATE = 2;
	protected static final int ARG_OTHER = 3;
	
	private static final String CREATED = "created";
	private static final String IDENTIFIER = "identifier";
	private static final String EMAIL = "email";
	
	@Override
	public String buildLatestEntryQuery(String identifier, String email) {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.addFilterQuery("identifier_ssi:" + identifier, "email_ssi:" + email, "tag_ssi:latest");
		
		return query.toString();
	}

	@Override
	public String buildGetMetadataQuery(String status, String access,
			Map<String, String> requestQueryValues,
			Map<String, Object> requestAttributes) {
		
		SolrQuery dataQuery = new SolrQuery();
		dataQuery.setRequestHandler("select");
		buildBaseQuery(dataQuery, status, access,
				requestQueryValues, requestAttributes);
		
		//Build the paging query
		buildPagingFilter(dataQuery, requestQueryValues);
    	
        return dataQuery.toString();
	}

	@Override
	public String buildGetMetadataCountQuery(String status, String access,
			Map<String, String> requestQueryValues,
			Map<String, Object> requestAttributes) {
		
		SolrQuery dataQuery = new SolrQuery();
		dataQuery.setRequestHandler("select");
		dataQuery.setRows(0);
		buildBaseQuery(dataQuery, status, access,
				requestQueryValues, requestAttributes);
		
        return dataQuery.toString();
	}

	@Override
	public String buildGetTotalRecordsQuery(String status, String access) {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.setRows(0);
		query.setRequestHandler("select");
		
		String[] filters = new String[2];
		filters[0] = "tag_ssi:latest";
		
		Map<String, String> statusFilter = new HashMap<String, String>();
		setStatus(statusFilter, status);
		
		for (Entry<String, String> entry : statusFilter.entrySet()){
			filters[1] = entry.getKey() + ":" + entry.getValue();
		}
		
		query.addFilterQuery(filters);   
		
		return query.toString();
	}

	@Override
	public String buildEndorserQuery(String email, String historyRange) {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.addFilterQuery("email_ssi:" + email, "created_dtsi:[" + historyRange + " TO *]");
		
		return query.toString();
	}

	@Override
	public String buildEndorsersQuery() {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.set(GroupParams.GROUP, true);
		query.set(GroupParams.GROUP_FIELD, "email_ssi");
		
		return query.toString();
	}

	@Override
	public String buildExpiryQuery(String expiryDate) {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.addFilterQuery("tag_ssi:latest", 
				"-deprecated_tesi:*", "valid_dtsi:[ NOW TO " + expiryDate + "]");
		
		return query.toString();
	}

	@Override
	public String buildReminderQuery(String email) {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.addFilterQuery("email_ssi:" + email, "tag_ssi:latest", 
				"-deprecated_tesi:*", "valid_dtsi:[ NOW TO *]");
		
		return query.toString();
	}

	@Override
	public String buildTagQuery(String tag, String email) {
		SolrQuery tagQuery = new SolrQuery();
		tagQuery.setQuery("*:*");
		tagQuery.addFilterQuery("email_ssi:" + email, "alternative_ssi:" + tag);
		tagQuery.setSort("created_dtsi", ORDER.desc);
		tagQuery.setRows(1);
		
		return tagQuery.toString();
	}

	@Override
	public String buildEndorserTagsQuery(String email) {
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.addFilterQuery("email_ssi:" + email);
		query.set(GroupParams.GROUP, true);
		query.set(GroupParams.GROUP_FIELD, "alternative_ssi");
		
		return query.toString();
	}
	
	private void buildBaseQuery(SolrQuery query, String status, String access,
			Map<String, String> requestQueryValues,
			Map<String, Object> requestAttributes){
		
		boolean hasDate = false;
	    
		addQueryParametersToRequest(requestAttributes, requestQueryValues);
		Map<String, String> filters = buildFilterFromUrlQuery(requestAttributes);
    	    	
    	if(filters.size() > 0){
    		if(filters.containsKey("created_dtsi")){
    			hasDate = true;
    		}
    	}
    	    	
    	//Build the paging query
    	String search = requestQueryValues.get("sSearch");
    	if (search.equals("")){
    		search = "*:*";
    	}
    	
    	query.setQuery(search);
    	
    	buildSearchColsFilter(filters, requestQueryValues);	

        if (!hasDate) {
        	filters.put("tag_ssi", "latest");
        }
                     
        setStatus(filters, status);
        setAccess(filters, access);
            
        String[] filterStrings = new String[filters.size()];
        int i = 0;
        //somehow turn filters to string list and set in query
        for (Entry<String, String> entry : filters.entrySet()){
        	filterStrings[i] = entry.getKey() + ":" + entry.getValue();
        	i++;
        }
        
        query.addFilterQuery(filterStrings);
    }
	
	private void setStatus(Map<String, String> filters, String status){
		if(status.equals("expired")){ //implies validity date in the past and not deprecated
        	filters.put("valid_dtsi", "[ * TO NOW ]");
        	filters.put("-deprecated_tesi","*");
        } else if(status.equals("valid")){ //implies in date and not deprecated
        	  filters.put("valid_dtsi", "[ NOW TO *]");
        	  filters.put("-deprecated_tesi","*");
        } else if(status.equals("deprecated")){ //implies contains a deprecated field
        	filters.put("deprecated_tesi","*");
        }
	}
	
	private void setAccess(Map<String, String> filters, String access){
		if(access.equals("pdisk")){ 
        	filters.put("location_ssim", "pdisk");
        } else if(access.equals("web")){ 
        	  filters.put("location_ssim", "http OR ftp OR https");
        }
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
	
	private Map<String, String> buildFilterFromUrlQuery(Map<String, Object> attributes){
    	Map<String, String> filters = new HashMap<String, String>();
    	
    	for (Map.Entry<String, Object> arg : attributes.entrySet()) {
    		String key = arg.getKey();
    		if (!key.startsWith("org.restlet")) {
    			switch (classifyArg((String) arg.getValue())) {
    			case ARG_EMAIL:
    				filters.put("email_ssi", (String)arg.getValue());
    				break;
    			case ARG_DATE:
    				filters.put("created_dtsi", (String)arg.getValue());
    				break;
    			case ARG_OTHER:
    				filters.put("identifier_ssi", (String)arg.getValue());
    				break;
    			default:
    				break;
    			}
    		}
    	}
    	
    	return filters;
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
     * Build a paging filter
     * 
     */
    private void buildPagingFilter(SolrQuery query, Map<String, String> requestQueryValues){
    	
    	String iDisplayStart = requestQueryValues.get("iDisplayStart");
		String iDisplayLength = requestQueryValues.get("iDisplayLength");
		String iSortCol = requestQueryValues.get("iSortCol_0");
		String sSortDir = requestQueryValues.get("sSortDir_0");   	
    	
    	if(iDisplayStart != null && iDisplayLength != null){
    		int sort = (iSortCol != null) ? 
    				Integer.parseInt(iSortCol) : SolrUtils.DEFAULT_SORT_COL;
    		String sortCol = SolrUtils.getColumn(sort);
    		
    		if (sSortDir.equals("asc")){
    			query.setSort(sortCol, ORDER.asc);
    		} else {
    			query.setSort(sortCol, ORDER.desc);
    		}
    		
    		query.setStart(Integer.parseInt(iDisplayStart));
    		query.setRows(Integer.parseInt(iDisplayLength));
    	}
    	
    }
              
    private void buildSearchColsFilter(Map<String, String> filters, Map<String, String> requestQueryValues) {
    	String[] sSearchCols = new String[SolrUtils.getColumnCount()];
    	
        for(int i = 0; i < SolrUtils.getColumnCount();i++){
                sSearchCols[i] = requestQueryValues.get("sSearch_" + i);
        }
    	
    	for(int i = 1; i <= SolrUtils.SEARCHABLE_COLUMNS; i++){
                if ( sSearchCols[i] != null && sSearchCols[i].length() > 0 )
                {
                	String word = sSearchCols[i];
                    filters.put(SolrUtils.getColumn(i), word);
                }
        }
          
    }
    
    @Override
	public String getLatestField() {
		return "created";
	}

}
