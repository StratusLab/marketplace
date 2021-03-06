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
package eu.stratuslab.marketplace.server.query;


public final class SparqlUtils {
	private static final String[] aColumns = { "", "os", "osversion", "arch", "email", "kind", 
		"created", "identifier", "location", "description", "title"};
	
	public static final int SEARCHABLE_COLUMNS = 5;
	
	public static final int DEFAULT_SORT_COL = 6;
	
	public static final String EMAIL_QUERY = //
        "SELECT DISTINCT ?email ?subject ?issuer "
                + " WHERE {"
                + " ?x <http://purl.org/dc/terms/identifier>  ?identifier . "
                + " ?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . "
                + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser . "
                + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email . "
                + " ?endorser <http://mp.stratuslab.eu/slreq#subject> ?subject . "
                + " ?endorser <http://mp.stratuslab.eu/slreq#issuer> ?issuer . }";
	
	public static final String ENDORSER_HISTORY_QUERY_TEMPLATE = //
    	"SELECT ?identifier ?created ?description ?location ?deprecated ?email " +
    	"WHERE { ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
    	"OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#location> ?location . } " +
    	"OPTIONAL { ?x <http://purl.org/dc/terms/description> ?description . } " +
    	"OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated . } " +
    	"?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
    	"?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser; " +
    	"<http://purl.org/dc/terms/created> ?created . " +
    	"?endorser <http://mp.stratuslab.eu/slreq#email> ?email . " +
    	"FILTER (?email = \"%s\") . FILTER(?created > \"%s\") }";
	
	public static final String LATEST_ENTRY_QUERY_TEMPLATE = //
		"SELECT (MAX(?created) AS ?latest)" +
		"WHERE { ?x <http://purl.org/dc/terms/identifier>  ?identifier . " +
		"?x <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
    	"?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser; " +
    	"<http://purl.org/dc/terms/created> ?created . " +
    	"?endorser <http://mp.stratuslab.eu/slreq#email> ?email . " +
    	"FILTER (?identifier = \"%s\") . " +
    	"FILTER (?email = \"%s\") }";
    	
	private static final String FILTER_TEMPLATE = " FILTER (?%s = \"%s\") . ";
	
	private static final String LIMIT_TEMPLATE = " ORDER BY %s(?%s)" +
    		         " LIMIT %s" +
    		         " OFFSET %s";
	
	private static final String REGEX_TEMPLATE = "regex(?%s, \"%s\", \"i\") ";
	
	private static final String LATEST_FILTER_TEMPLATE = " FILTER (?tag = \"latest\") .";
	
	private static final String VALID_ENTRIES_FILTER = "FILTER (?valid > \"%s\") .";
	
	private static final String EXPIRED_ENTRIES_FILTER = "FILTER (?valid < \"%s\") .";
	
	public static final String COUNT_WHERE_BLOCK =
			" ?x <http://purl.org/dc/terms/identifier>  ?identifier;"
		    + " <http://mp.stratuslab.eu/slterms#tag> ?tag;"
			+ " <http://purl.org/dc/terms/valid> ?valid;"
	        + " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ."
	        + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;"
	        + " <http://purl.org/dc/terms/created> ?created ."
	        + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .";
	
	public static final String WHERE_BLOCK =
		" ?x <http://purl.org/dc/terms/identifier>  ?identifier ."
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os> ?os . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-version> ?osversion . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-arch> ?arch . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#location> ?location . }"
        + " OPTIONAL { ?x <http://purl.org/dc/terms/description> ?description . }"
        + " OPTIONAL { ?x <http://purl.org/dc/terms/title> ?title . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#kind> ?kind . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#tag> ?tag . }"
        + " ?x <http://purl.org/dc/terms/valid> ?valid;"
        + " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ."
        + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;"
        + " <http://purl.org/dc/terms/created> ?created ."
        + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .";
	
    public static final String SELECT_COUNT = "SELECT DISTINCT(COUNT(*) AS ?count)";
        
    public static final String SELECT_ALL = "SELECT ?identifier ?email ?created"
		+ " (sample(?os) as ?os) (sample(?osversion) as ?osversion) (sample(?arch) as ?arch) " +
		"(sample(?location) as ?location) (sample(?description) as ?description) " +
		"(sample(?title) as ?title) (sample(?kind) as ?kind)";
    
    public static final String DEPRECATED_OFF = "FILTER (NOT EXISTS " +
    		"{?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated}) ";
    
    public static final String DEPRECATED_ON = "FILTER (EXISTS " +
    		"{?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated}) ";
    
    public static final String ACCESS_PUBLIC_FILTER = "FILTER (!BOUND(?location) || regex(?location, \"^(https?|ftp)://.*$\")) ";
    public static final String ACCESS_PRIVATE_FILTER = "FILTER regex(?location, \"^pdisk.*\") ";
   
    public static final String TAG_QUERY_TEMPLATE = 
    	"SELECT ?identifier ?created where {"
    	+ "{"
    	+ "SELECT ?identifier (MAX(?created) as ?created) (sample(?deprecated) as ?dep) "
    	+ "WHERE { ?x <http://purl.org/dc/terms/identifier>  ?identifier; " +
    	"<http://purl.org/dc/terms/alternative> \"%s\"; " +
        "<http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . " +
    	"?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser; " +
    	"<http://purl.org/dc/terms/created> ?created . " +
    	"?endorser <http://mp.stratuslab.eu/slreq#email> \"%s\"  " +
    	". OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated .} " +
    	"} GROUP BY ?identifier " +
    	"}  . FILTER (!bound(?dep)) " +
    	"} ORDER BY desc(?created) LIMIT 1";
    
    public static final String ENDORSER_TAG_QUERY_TEMPLATE = "SELECT DISTINCT ?tag "
    		+ "WHERE { ?x <http://purl.org/dc/terms/alternative> ?tag; "
    		+  "<http://mp.stratuslab.eu/slreq#endorsement> ?endorsement . "
    		+ "?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser; "
            + "<http://purl.org/dc/terms/created> ?created . "
    		+ "?endorser <http://mp.stratuslab.eu/slreq#email> \"%s\""
            + "}";
    
    public static final String GROUP_BY = " GROUP BY ?identifier ?email ?created";
    
    private SparqlUtils(){}
    
	public static String buildFilterEq(String arg, String value){
		return String.format(FILTER_TEMPLATE, arg, value);
	}
	
	public static String buildLimit(String col, String limit, String offset, String direction){
		return String.format(LIMIT_TEMPLATE, direction, col, limit, offset);
	}
	
	public static String buildRegex(String col, String pattern){
		return String.format(REGEX_TEMPLATE, col, pattern);
	}
	
	public static String getColumn(int i){
		return aColumns[i];
	}
	
	public static String getLatestFilter(){
		return String.format(LATEST_FILTER_TEMPLATE);
	}
	
	public static String getValidFilter(String date){
		return String.format(VALID_ENTRIES_FILTER, date);
	}
	
	public static String getExpiredFilter(String date){
		return String.format(EXPIRED_ENTRIES_FILTER, date);
	}
	
	public static int getColumnCount() {
		return aColumns.length;
	}
		
}
