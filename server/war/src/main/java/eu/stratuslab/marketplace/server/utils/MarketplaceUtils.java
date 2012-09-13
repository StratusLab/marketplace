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
