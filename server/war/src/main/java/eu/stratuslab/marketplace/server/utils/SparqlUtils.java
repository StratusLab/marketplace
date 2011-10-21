package eu.stratuslab.marketplace.server.utils;


public class SparqlUtils {
	private static final String[] aColumns = { "", "os", "osversion", "arch", "email", "created",
			"identifier", "location", "description"};
	
	private static final String FILTER_TEMPLATE = " FILTER (?%s = \"%s\") . ";
	private static final String LIMIT_TEMPLATE = " ORDER BY %s(?%s)" +
    		         " LIMIT %s" +
    		         " OFFSET %s";
	private static final String REGEX_TEMPLATE = "regex(?%s, \"%s\", \"i\") ";
	
	private static final String LATEST_FILTER_TEMPLATE = " OPTIONAL { "
    + " ?lx <http://purl.org/dc/terms/identifier>  ?lidentifier; "
    + " <http://mp.stratuslab.eu/slreq#endorsement> ?lendorsement ."
    + " ?lendorsement <http://mp.stratuslab.eu/slreq#endorser> ?lendorser;"
    + " <http://purl.org/dc/terms/created> ?latestcreated ."
    + " ?lendorser <http://mp.stratuslab.eu/slreq#email> ?lemail ."
    + " FILTER (?lidentifier = ?identifier) ."
    + " FILTER (?lemail = ?email) ."
    + " FILTER (?latestcreated > ?created) . } FILTER (!bound (?lendorsement)) ."
    + " FILTER (?valid > \"%s\") .";
	
	public static final String WHERE_BLOCK =
		" ?x <http://purl.org/dc/terms/identifier>  ?identifier ."
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os> ?os . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-version> ?osversion . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#os-arch> ?arch . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#location> ?location . }"
        + " OPTIONAL { ?x <http://purl.org/dc/terms/description> ?description . }"
        + " OPTIONAL { ?x <http://mp.stratuslab.eu/slterms#deprecated> ?deprecated . }"
        + " ?x <http://purl.org/dc/terms/valid> ?valid;"
        + " <http://mp.stratuslab.eu/slreq#endorsement> ?endorsement ."
        + " ?endorsement <http://mp.stratuslab.eu/slreq#endorser> ?endorser;"
        + " <http://purl.org/dc/terms/created> ?created ."
        + " ?endorser <http://mp.stratuslab.eu/slreq#email> ?email .";
	
    public static final String SELECT_COUNT = "SELECT DISTINCT(COUNT(*) AS ?count)";
        
    public static final String SELECT_ALL = "SELECT ?identifier ?email ?created"
		+ " ?os ?osversion ?arch ?location ?description";
    
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
	
	public static String getLatestFilter(String date){
		return String.format(LATEST_FILTER_TEMPLATE, date);
	}
	
	public static int getColumnCount() {
		return aColumns.length;
	}
		
}
