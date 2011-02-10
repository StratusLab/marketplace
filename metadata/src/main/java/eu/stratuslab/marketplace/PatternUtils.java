package eu.stratuslab.marketplace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtils {

    final public static Pattern IDENTIFIER = Pattern
            .compile("^[A-Za-z0-9_-]{27}$");

    final public static Pattern EMAIL = Pattern
            .compile("[A-Za-z0-9\\._%+-]+@[A-Za-z0-9\\.-]+");

    final public static Pattern DATE = Pattern
            .compile("\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

    private PatternUtils() {

    }

    public static boolean isIdentifier(CharSequence identifier) {
        Matcher m = IDENTIFIER.matcher(identifier);
        return m.matches();
    }

    public static boolean isEmail(CharSequence email) {
        Matcher m = EMAIL.matcher(email);
        return m.matches();
    }

    public static boolean isDate(CharSequence date) {
        Matcher m = DATE.matcher(date);
        return m.matches();
    }

}
