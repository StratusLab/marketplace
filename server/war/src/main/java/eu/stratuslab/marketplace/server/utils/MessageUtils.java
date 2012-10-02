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

import java.io.File;

public final class MessageUtils {

    private static final String CONFIRM_MSG = "%n"
            + "A new metadata entry %s image ID %s,%n"
            + "endorsed at %s with this email address,%n"
            + "has been uploaded to the StratusLab marketplace.%n%n "
            + "Please either confirm or abort this request by visiting%n"
            + "one of the following links:%n%n"
            + "Confirm:  %s%n%nAbort:  %s%n%n"
            + "If this metadata entry was not uploaded by you, please%n"
            + "indicate this by visiting the following address: %n%n"
            + "Abuse:  %s%n%n"
            + "We will then investigate how this entry was uploaded.%n";

    private static final String ABUSE_MSG = "%n"
    	+ "Abuse has been reported for the following entry: %s%n";
    
    private MessageUtils() {

    }

    public static String createNotification(String baseUrl, File file, 
    		String[] coords, String deprecated) {
        String identifier = coords[0];
        String created = coords[2];
    	String tag = (deprecated != "") ? "deprecating" : "for";
        
    	String uuid = extractUUIDFromFile(file);
        
        String confirmUrl = baseUrl + "/action/" + uuid + "/confirm/";
        String abortUrl = baseUrl + "/action/" + uuid + "/abort/";
        String abuseUrl = baseUrl + "/action/" + uuid + "/abuse/";
        return String.format(CONFIRM_MSG, tag, identifier, created, 
        		confirmUrl, abortUrl, abuseUrl);
    }

    public static String createAbuseNotification(File file){
    	String uuid = extractUUIDFromFile(file);
    	
    	return String.format(ABUSE_MSG, uuid);
    }
    
    private static String extractUUIDFromFile(File file) {
        String name = file.getName();
        int index = name.indexOf('.');
        if (index < 0) {
            index = name.length();
        }
        return name.substring(0, index);
    }
}
