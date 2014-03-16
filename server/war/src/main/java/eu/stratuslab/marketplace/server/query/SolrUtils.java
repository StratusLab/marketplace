package eu.stratuslab.marketplace.server.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SolrUtils {
	
	private static final String[] aColumns = { "", "os_ssi", "osversion_ssi", "arch_ssi", "email_ssi", "kind_ssi", 
		"created_dtsi", "identifier_ssi", "location_ssim", "description_tesi", "title_tesi"};
	
	public static final int SEARCHABLE_COLUMNS = 5;
	
	public static final int DEFAULT_SORT_COL = 6;
	
	private static Map<String,String> solrToColumn = new HashMap<String, String>();
	static {
		Map<String, String> solrMap = new HashMap<String, String>();
		solrMap.put("os_ssi", "os");
		solrMap.put("osversion_ssi", "osversion");
		solrMap.put("arch_ssi", "arch");
		solrMap.put("email_ssi", "email");
		solrMap.put("kind_ssi", "kind");
		solrMap.put("created_dtsi", "created");
		solrMap.put("identifier_ssi", "identifier");
		solrMap.put("location_ssim", "location");
		solrMap.put("description_tesi", "description");
		solrMap.put("title_tesi", "title");
		
		solrToColumn = Collections.unmodifiableMap(solrMap);
	}
	
	public static String getResultColumn(String solrName){
		return solrToColumn.get(solrName);
	}
	
	public static int getColumnCount() {
		return aColumns.length;
	}
	
	public static String getColumn(int i){
		return aColumns[i];
	}
		
}
