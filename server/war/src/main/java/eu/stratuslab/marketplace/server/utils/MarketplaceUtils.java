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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class MarketplaceUtils {
		
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	private MarketplaceUtils(){}
	
	public static String getCurrentDate() {
		return getFormattedDate(new Date());
	}

	public static Date getFormattedDate(String timestamp) throws ParseException{
		return getDateFormat().parse(timestamp);
	}
	
	public static String getFormattedDate(Date date){
		return getDateFormat().format(date);
	}
	
	private static DateFormat getDateFormat() {
		SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
		format.setLenient(false);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));

		return format;
	}

}
