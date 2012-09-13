package eu.stratuslab.marketplace.server.query;

import java.util.Map;

public interface QueryBuilder {
	
	String buildLatestEntryQuery(String identifier, String email);
	
	String buildGetMetadataQuery(String deprecatedFlag, 
			Map<String, String> requestQueryValues, 
			Map<String, Object> requestAttributes);
	
	String buildGetMetadataCountQuery(String deprecatedFlag, 
			Map<String, String> requestQueryValues, 
			Map<String, Object> requestAttributes);
	
	String buildGetTotalRecordsQuery(String deprecatedFlag);
	
	String buildEndorserQuery(String email, String historyRange);
	
	String buildEndorsersQuery();
	
	String buildReminderQuery(String email);
	
	String buildExpiryQuery(String expiryDate);
}
