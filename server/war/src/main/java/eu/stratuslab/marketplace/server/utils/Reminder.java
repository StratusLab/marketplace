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
		StringBuilder query = new StringBuilder(SparqlUtils.SELECT_ALL);

		StringBuilder filter = new StringBuilder(
				" WHERE {"
				+ SparqlUtils.WHERE_BLOCK);

		filter.append(filterPredicate.toString());
		filter
		.append(SparqlUtils.getLatestFilter(getCurrentDate()));

		filter.append(" FILTER (!bound (?deprecated))");
		filter.append(" }");

		query.append(filter);

		return query.toString();
	}
		
}
