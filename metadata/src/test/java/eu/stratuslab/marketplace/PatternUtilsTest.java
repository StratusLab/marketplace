package eu.stratuslab.marketplace;

import static eu.stratuslab.marketplace.PatternUtils.isDate;
import static eu.stratuslab.marketplace.PatternUtils.isEmail;
import static eu.stratuslab.marketplace.PatternUtils.isIdentifier;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PatternUtilsTest {

    final static private String[] validLetters = { "A", "B", "C", "D", "E",
            "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e",
            "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
            "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4",
            "5", "6", "7", "8", "9", "-", "_" };

    @Test
    public void testValidIdentifiers() {

        for (String s : validLetters) {
            String identifier = repeat(s, 27);
            if (!isIdentifier(identifier)) {
                fail("identifier (" + identifier
                        + ") is invalid but should be OK");
            }
        }

    }

    @Test
    public void testInvalidShortIdentifiers() {

        for (String s : validLetters) {
            String identifier = repeat(s, 26);
            if (isIdentifier(identifier)) {
                fail("identifier (" + identifier
                        + ") is valid but should be INVALID");
            }
        }

    }

    @Test
    public void testInvalidLongIdentifiers() {

        for (String s : validLetters) {
            String identifier = repeat(s, 28);
            if (isIdentifier(identifier)) {
                fail("identifier (" + identifier
                        + ") is valid but should be INVALID");
            }
        }

    }

    @Test
    public void testInvalidLetterIdentifiers() {

        String prefix = repeat("A", 26);

        // Common Base64 variant characters.
        String[] invalidLetters = { "!", "/", ".", ":", "+" };

        for (String s : invalidLetters) {
            if (isIdentifier(prefix + s)) {
                fail("identifier (" + prefix + s
                        + ") is valid but should be INVALID");
            }
        }

    }

    @Test
    public void validShortEmailPatterns() {

        String[] valid = { "A@A", "a@a", "0@0", ".@.", "-@-", "_@A", "%@A",
                "+@A" };

        for (String s : valid) {
            if (!isEmail(s)) {
                fail("email (" + s + ") is invalid but should be OK");
            }
        }

    }

    @Test
    public void invalidShortEmailPatterns() {

        String[] invalid = { "@", "@A", "A@", "A@_", "A@%", "A@+" };

        for (String s : invalid) {
            if (isEmail(s)) {
                fail("email (" + s + ") is valid but should be INVALID");
            }
        }

    }

    @Test
    public void validDatePattern() {
        String date = "9999-99-99T99:99:99Z";
        if (!isDate(date)) {
            fail("date (" + date + ") is invalid but should be OK");
        }
    }

    @Test
    public void invalidDatePatterns() {

        String[] invalid = { "9999-99-99t99:99:99Z", "9999-99-99T99:99:99z",
                "9999-99-99T99:99:99+0100" };

        for (String s : invalid) {
            if (isDate(s)) {
                fail("date (" + s + ") is valid but should be INVALID");
            }
        }

    }

    private static String repeat(String letter, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(letter);
        }
        return sb.toString();
    }
}
