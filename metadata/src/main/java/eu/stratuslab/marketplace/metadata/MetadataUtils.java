package eu.stratuslab.marketplace.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

import org.w3c.dom.Node;

public class MetadataUtils {

    final static private String[] algorithms = { "MD5", "SHA-1", "SHA-256",
            "SHA-512" };

    final static private String[] encoding = { "A", "B", "C", "D", "E", "F",
            "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
            "T", "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "-", "_" };

    final static private Map<String, BigInteger> decoding = new HashMap<String, BigInteger>();
    static {
        for (int i = 0; i < encoding.length; i++) {
            decoding.put(encoding[i], BigInteger.valueOf(i));
        }
    }

    final static private int sha1Bits = 160;

    final static private int fieldBits = 6;

    final static private int identifierChars = sha1Bits / fieldBits + 1;

    final static private BigInteger divisor = BigInteger.valueOf(2L).pow(
            fieldBits);

    private MetadataUtils() {

    }

    public static Map<String, BigInteger> streamInfo(InputStream is) {

        BigInteger bytes = BigInteger.ZERO;

        Map<String, BigInteger> results = new HashMap<String, BigInteger>();
        results.put("BYTES", bytes);

        try {

            ArrayList<MessageDigest> mds = new ArrayList<MessageDigest>();
            for (String algorithm : algorithms) {
                try {
                    mds.add(MessageDigest.getInstance(algorithm));
                } catch (NoSuchAlgorithmException consumed) {
                    // Do nothing.
                }
            }

            byte[] buffer = new byte[1024];

            for (int length = is.read(buffer); length > 0; length = is
                    .read(buffer)) {

                bytes = bytes.add(BigInteger.valueOf(length));
                for (MessageDigest md : mds) {
                    md.update(buffer, 0, length);
                }
            }

            results.put("BYTES", bytes);

            for (MessageDigest md : mds) {
                results.put(md.getAlgorithm(), new BigInteger(1, md.digest()));
            }

        } catch (IOException consumed) {
            // Do nothing.
        }

        return results;
    }

    public static String sha1ToIdentifier(BigInteger sha1) {

        if (sha1.compareTo(BigInteger.ZERO) < 0 || sha1.bitLength() > sha1Bits) {
            throw new IllegalArgumentException("invalid SHA-1 checksum");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < identifierChars; i++) {
            BigInteger[] values = sha1.divideAndRemainder(divisor);
            sha1 = values[0];
            sb.append(encoding[values[1].intValue()]);
        }
        return sb.reverse().toString();
    }

    public static BigInteger identifierToSha1(String identifier) {

        if (identifier.length() != identifierChars) {
            throw new IllegalArgumentException("invalid identifier");
        }

        BigInteger sha1 = BigInteger.ZERO;

        for (int i = 0; i < identifier.length(); i++) {
            sha1 = sha1.shiftLeft(fieldBits);
            BigInteger bits = decoding.get(identifier.substring(i, i + 1));
            if (bits == null) {
                throw new IllegalArgumentException("invalid identifier");
            }
            sha1 = sha1.or(bits);
        }
        return sha1;
    }

    public static boolean isSignatureOK(Node node) {

        try {

            DOMValidateContext context = new DOMValidateContext(
                    new X509KeySelector(), node);

            XMLSignatureFactory factory = XMLSignatureFactory
                    .getInstance("DOM");
            XMLSignature signature = factory.unmarshalXMLSignature(context);

            return signature.validate(context);

        } catch (MarshalException consumed) {
            return false;

        } catch (XMLSignatureException consumed) {
            return false;
        }
    }

}
