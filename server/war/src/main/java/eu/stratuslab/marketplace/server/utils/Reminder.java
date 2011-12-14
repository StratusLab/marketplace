package eu.stratuslab.marketplace.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.restlet.Application;

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.resources.BaseResource;

public class Reminder extends BaseResource {

	public Reminder(Application app){
		setApplication(app);
	}
	
	public void remind() {
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		
		try {
        	results = query(SparqlUtils.EMAIL_QUERY);
        } catch(MarketplaceException e){
        	LOGGER.severe(e.getMessage());
        }
		for(int i = 0; i < results.size(); i++){
        	Map<String, String> resultRow = results.get(i);
        	
        	String subject = resultRow.get("subject");
        	String name = subject.substring(subject.indexOf("CN=")+3, 
        			subject.indexOf(",", subject.indexOf("CN=")+3));
        	String email = resultRow.get("email");
        	
        	StringBuilder mailContents = new StringBuilder("Dear " + name + ",\n\n" +
        			"You are currently endorsing the following images in the StratusLab Marketplace.\n" +
                    "If you no longer wish to endorse these images please " +
                    "deprecate the corresponding Marketplace entries.\n\n" +
                    "You can deprecate an entry using the following command:\n\t" +
                    "stratus-deprecate-metadata --email=" + email + " <image id>\n\n");

            List<Map<String, String>> entries = new ArrayList<Map<String, String>>();
            
            try {
				entries = query(buildQuery(email));
			} catch (MarketplaceException e) {
				LOGGER.warning(e.getMessage());
			}

			if(entries.size() > 0){
				for(int j = 0; j < entries.size(); j++){
					Map<String, String> entryRow = (Map<String, String>)entries.get(j);

					String identifier = entryRow.get("identifier");
					String created = entryRow.get("created");
					String os = entryRow.get("os");
					String osversion = entryRow.get("osversion");
					String location = entryRow.get("location");
					String description = entryRow.get("description");

					mailContents.append(identifier 
							+ "\n\tcreated:\t" + created
							+ "\n\tos:     \t" + os + " " + osversion + "\n");

					if(description != "null" && description.length() > 0){
						mailContents.append("\tdescription:\t" + description + "\n");
					}
					if(location != "null" && location.length() > 0){
						mailContents.append("\tlocation:\t" + location + "\n");
					}

					mailContents.append("\n");
				}

				try {
					Notifier.sendNotification(email, mailContents.toString());
				} catch (Exception e) {
					LOGGER.warning(e.getMessage());
				}
			}
		}
	}
	
	private String buildQuery(String email){
		StringBuilder filterPredicate = new StringBuilder();

		filterPredicate.append(
				SparqlUtils.buildFilterEq("email", email));
		//Build the full SPARQL query
		StringBuilder queryString = new StringBuilder(SparqlUtils.SELECT_ALL);

		StringBuilder filterString = new StringBuilder(
				" WHERE {"
				+ SparqlUtils.WHERE_BLOCK);

		filterString.append(filterPredicate.toString());
		filterString
		.append(SparqlUtils.getLatestFilter(getCurrentDate()));

		filterString.append(" FILTER (!bound (?deprecated))");
		filterString.append(" }");

		queryString.append(filterString);

		return queryString.toString();
	}
		
}
