package eu.stratuslab.marketplace.server.utils;

import java.io.File;

public class MessageUtils {

    private static final String CONFIRM_MSG = "\n"
            + "A new metadata entry has been uploaded to the StratusLab\n"
            + "marketplace with this email address.\n\n"
            + "Please either confirm or abort this request by visiting\n"
            + "one of the following links:\n\n"
            + "Confirm:  %s\n\nAbort:  %s\n\n"
            + "If this matadata entry was not uploaded by you, please\n"
            + "indicate this by visiting the following address: \n\n"
            + "Abuse:  %s\n\n"
            + "We will then investigate how this entry was uploaded.\n";

    private MessageUtils() {

    }

    public static String createNotification(String baseUrl, File file) {
        String uuid = extractUUIDFromFile(file);

        String confirmUrl = baseUrl + "action/" + uuid + "/confirm/";
        String abortUrl = baseUrl + "action/" + uuid + "/abort/";
        String abuseUrl = baseUrl + "action/" + uuid + "/abuse/";
        return String.format(CONFIRM_MSG, confirmUrl, abortUrl, abuseUrl);
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
