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

import static eu.stratuslab.marketplace.server.cfg.Parameter.MARKETPLACE_ENDPOINT;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.Application;

import eu.stratuslab.marketplace.server.MarketplaceException;
import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.resources.BaseResource;

public class Reminder extends BaseResource {

	private String marketplaceEndpoint;
		
	private static final int MAX_EXPIRY_RANGE = 3;
	
	public Reminder(Application app){
		setApplication(app);
		
		String endpoint = Configuration.getParameterValue(MARKETPLACE_ENDPOINT);
		if (endpoint.endsWith("/")) {
			    marketplaceEndpoint = endpoint.substring(0, endpoint.length() - 1);
		} else {
			marketplaceEndpoint = endpoint;
		}
		
	}
	
	public void remind() {
		String cn = "CN=";
		
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		
		try {
        	results = query(getQueryBuilder().buildEndorsersQuery());
        } catch(MarketplaceException e){
        	LOGGER.severe(e.getMessage());
        }
		for(int i = 0; i < results.size(); i++){
        	Map<String, String> resultRow = results.get(i);
        	
        	String subject = resultRow.get("subject");
        	String name = subject.substring(subject.indexOf(cn)+cn.length(), 
        			subject.indexOf(",", subject.indexOf(cn)+cn.length()));
        	String email = resultRow.get("email");
        	
        	StringBuilder mailContents = new StringBuilder("Dear " + name + ",\n\n" +
        			"You are currently endorsing the following images in the StratusLab Marketplace.\n" +
                    "If you no longer wish to endorse these images please " +
                    "deprecate the corresponding Marketplace entries.\n\n" +
                    "You can deprecate an entry using the following command:\n\t" +
                    "stratus-deprecate-metadata --email=" + email + " <image id>\n\n");

            List<Map<String, String>> entries = new ArrayList<Map<String, String>>();
            
            try {
				entries = query(getQueryBuilder().buildReminderQuery(email));
			} catch (MarketplaceException e) {
				LOGGER.warning(e.getMessage());
			}

			buildAndSendReminderEmail(email, entries, mailContents);
		}
	}
		
	public void expiry() {
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		
		try {
			String expiringEntries = getQueryBuilder().buildExpiryQuery(getExpiryDate());
			
			results = query(expiringEntries);
        } catch(MarketplaceException e){
        	LOGGER.severe(e.getMessage());
        }
        
        HashMap<String, List<Map<String, String>>> entries = 
        	new HashMap<String, List<Map<String, String>>>();
        
		for(int i = 0; i < results.size(); i++){
			Map<String, String> resultRow = results.get(i);
        	
        	String email = resultRow.get("email");
        	
        	List<Map<String, String>> store = new ArrayList<Map<String, String>>();
        	
        	if(entries.containsKey(email)){
        		store = entries.get(email);
        	}
        	
        	store.add(resultRow);
        	entries.put(email, store);
		}
		
		buildAndSendExpiryEmail(entries);
	}
		
	private void buildAndSendReminderEmail(String email, 
			List<Map<String, String>> entries, StringBuilder mailContents){
		if(entries.size() > 0){
			for(int j = 0; j < entries.size(); j++){
				Map<String, String> entryRow = (Map<String, String>)entries.get(j);

				String identifier = entryRow.get("identifier");
				String created = entryRow.get("created");
				String os = entryRow.get("os");
				String osversion = entryRow.get("osversion");
				String location = entryRow.get("location");
				
				mailContents.append(identifier 
						+ "\n\tcreated:\t" + created
						+ "\n\tos:     \t" + os + " " + osversion + "\n");

				if(!location.equals("null") && location.length() > 0){
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
	
	private void buildAndSendExpiryEmail(Map<String, List<Map<String, String>>> entries){
			
		for(Map.Entry<String, List<Map<String, String>>> entry : entries.entrySet()){
			String endorser = entry.getKey();
			List<Map<String, String>> expiring = entry.getValue();
						
			StringBuilder mailContents = new StringBuilder("Dear Marketplace user,\n\n" +
        			"The following images in the StratusLab Marketplace ("
					+ marketplaceEndpoint + ")"    			
        			+ " are due to expire shortly. " 
        			+ "Once the validity date has passed the entry will no longer be visible. "
                    + "If you wish to continue endorsing these images please "
                    + "re-sign the metadata with an extended validity period and upload to the Marketplace.\n\n");
			
			for(Map<String, String> e: expiring){
				String identifier = e.get("identifier");
				String created = e.get("created");
				String valid = e.get("valid");
				
				mailContents.append("\n" + identifier + "\texpires: " + valid);
				mailContents.append("\nRetrieve entry: " + marketplaceEndpoint
				+ "/metadata/" + identifier + "/" + endorser + "/" + created + "?media=xml\n"); 
			}
			
			try {
				Notifier.sendNotification(endorser, mailContents.toString());
			} catch (Exception e) {
				LOGGER.warning(e.getMessage());
			}			
		}
	}
	
	private String getExpiryDate(){
		Date today = new Date();
		Calendar cal = Calendar.getInstance();  
		cal.setTime(today);    
		cal.add(Calendar.DATE, MAX_EXPIRY_RANGE);
		Date expiration = cal.getTime();
				
		return MarketplaceUtils.getFormattedDate(expiration);
	}
		
}
