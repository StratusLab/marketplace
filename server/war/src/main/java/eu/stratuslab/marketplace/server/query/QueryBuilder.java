package eu.stratuslab.marketplace.server.query;

import java.util.Map;

public interface QueryBuilder {
	
	String buildLatestEntryQuery(String identifier, String email);
	
	String buildGetMetadataQuery(String status, String access,
			Map<String, String> requestQueryValues, 
			Map<String, Object> requestAttributes);
	
	String buildGetMetadataCountQuery(String status, String access,
			Map<String, String> requestQueryValues, 
			Map<String, Object> requestAttributes);
	
	String buildGetTotalRecordsQuery(String status, String access);
	
	String buildEndorserQuery(String email, String historyRange);
	
	String buildEndorsersQuery();
	
	String buildExpiryQuery(String expiryDate);

	String buildReminderQuery(String email);
	
	String buildTagQuery(String email, String tag);
	
}
