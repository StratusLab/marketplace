package eu.stratuslab.marketplace.metadata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.stratuslab.marketplace.X509Info;

@SuppressWarnings("restriction")
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

    public static Object[] isSignatureOK(Map<String, String> docEndorserInfo,
            Node node) {

        try {

            DOMValidateContext context = new DOMValidateContext(
                    new X509KeySelector(), node);

            XMLSignatureFactory factory = XMLSignatureFactory
                    .getInstance("DOM");
            XMLSignature signature = factory.unmarshalXMLSignature(context);

            boolean coreValidation = signature.validate(context);

            if (coreValidation) {

                KeyInfo keyInfo = signature.getKeyInfo();
                X509Certificate cert = extractX509CertFromKeyInfo(keyInfo);
                Map<String, String> certEndorserInfo = extractEndorserInfoFromCert(cert);

                String errorString = isEndorserInfoConsistent(certEndorserInfo,
                        docEndorserInfo);
                if (errorString == null) {
                    return new Object[] { Boolean.TRUE, cert.toString() };
                } else {
                    return new Object[] { Boolean.FALSE, errorString };
                }

            } else {

                StringBuilder sb = new StringBuilder();

                boolean sv = signature.getSignatureValue().validate(context);
                sb.append("signature validation: " + sv);

                List<?> refs = signature.getSignedInfo().getReferences();
                for (Object oref : refs) {
                    Reference ref = (Reference) oref;
                    boolean refValid = ref.validate(context);
                    sb.append("content (ref='" + ref.getURI() + "') validity: "
                            + refValid);
                }

                return new Object[] { Boolean.FALSE, sb.toString() };

            }

        } catch (MarshalException e) {
            return new Object[] { Boolean.FALSE, e.getMessage() };
        } catch (XMLSignatureException e) {
            return new Object[] { Boolean.FALSE, e.getMessage() };
        }
    }

    public static X509Certificate extractX509CertFromKeyInfo(KeyInfo keyInfo) {

        List<?> keyInfoContent = keyInfo.getContent();
        for (Object o : keyInfoContent) {
            if (o instanceof X509Data) {
                X509Data x509Data = (X509Data) o;
                List<?> x509DataContent = x509Data.getContent();
                for (Object obj2 : x509DataContent) {
                    if (obj2 instanceof X509Certificate) {
                        return (X509Certificate) obj2;
                    }
                }
            }
        }

        return null;
    }

    /*
     * All of the keys extracted from the certificate must be match the values
     * in the metadata description, if present. Returns an error string; null
     * means everything's OK.
     */
    private static String isEndorserInfoConsistent(
            Map<String, String> certEndorserInfo,
            Map<String, String> docEndorserInfo) {

        for (Map.Entry<String, String> certEntry : certEndorserInfo.entrySet()) {
            String certKey = certEntry.getKey();
            String certValue = certEntry.getValue();
            String docValue = docEndorserInfo.get(certKey);
            if (!certValue.equals(docValue)) {
                if (docValue == null) {
                    docValue = "null";
                }
                return "endorser inconsistency (" + certKey + "): " + certValue
                        + " != " + docValue;
            }
        }

        return null;
    }

    public static Map<String, String> extractEndorserInfoFromCert(
            X509Certificate cert) {

        String subject = cert.getSubjectX500Principal().getName();
        String issuer = cert.getIssuerX500Principal().getName();
        String email = X509Info.extractEmailAddress(cert);

        Map<String, String> info = new HashMap<String, String>();
        info.put("subject", subject);
        info.put("issuer", issuer);
        if (email != null) {
            info.put("email", email);
        }

        return info;
    }

    public static void stripSignatureElements(Document doc) {

        NodeList nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
                "Signature");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            Node parent = node.getParentNode();
            parent.removeChild(node);
            doc.normalizeDocument();
        }
    }

    public static void writeStringToFile(String contents, File outputFile) {

        FileWriter os = null;
        try {

            os = new FileWriter(outputFile);
            os.write(contents);

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

    }

}
