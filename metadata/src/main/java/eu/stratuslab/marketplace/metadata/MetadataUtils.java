package eu.stratuslab.marketplace.metadata;

import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.DCTERMS_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.RDF_NS_URI;
import static eu.stratuslab.marketplace.metadata.MetadataNamespaceContext.SLREQ_NS_URI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.stratuslab.marketplace.X509Info;
import eu.stratuslab.marketplace.X509Utils;

public final class MetadataUtils {

    private static final int BUFFER_SIZE = 1024;

    private static final String[] ALGORITHMS = { "MD5", "SHA-1", "SHA-256",
            "SHA-512" };

    private static final String[] ENCODING = { "A", "B", "C", "D", "E", "F",
            "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
            "T", "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "-", "_" };

    private static final Map<String, BigInteger> DECODING = new HashMap<String, BigInteger>();
    static {
        for (int i = 0; i < ENCODING.length; i++) {
            DECODING.put(ENCODING[i], BigInteger.valueOf(i));
        }
    }

    public static final int SHA1_BITS = 160;

    private static final int FIELD_BITS = 6;

    private static final int NUM_ID_CHARS = SHA1_BITS / FIELD_BITS + 1;

    private static final BigInteger DIVISOR = BigInteger.valueOf(2L).pow(
            FIELD_BITS);

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private MetadataUtils() {

    }

    public static void signMetadataEntry(Document doc, X509Info x509Info,
            String email) {

        // Clean out any existing signatures.
        MetadataUtils.clearEndorsementElement(doc);
        MetadataUtils.stripSignatureElements(doc);

        // Fill in the endorsement element if it is empty.
        MetadataUtils.fillEndorsementElement(doc, x509Info, email);

        // Sign the document. The document is directly modified by method.
        X509Utils.signDocument(x509Info, doc);
    }

    public static Map<String, BigInteger> streamInfo(InputStream is) {

        BigInteger bytes = BigInteger.ZERO;

        Map<String, BigInteger> results = new HashMap<String, BigInteger>();
        results.put("BYTES", bytes);

        try {

            ArrayList<MessageDigest> mds = new ArrayList<MessageDigest>();
            for (String algorithm : ALGORITHMS) {
                try {
                    mds.add(MessageDigest.getInstance(algorithm));
                } catch (NoSuchAlgorithmException consumed) {
                    // Do nothing.
                }
            }

            byte[] buffer = new byte[BUFFER_SIZE];

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

        if (sha1.compareTo(BigInteger.ZERO) < 0 || sha1.bitLength() > SHA1_BITS) {
            throw new IllegalArgumentException("invalid SHA-1 checksum");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NUM_ID_CHARS; i++) {
            BigInteger[] values = sha1.divideAndRemainder(DIVISOR);
            sha1 = values[0];
            sb.append(ENCODING[values[1].intValue()]);
        }
        return sb.reverse().toString();
    }

    public static BigInteger identifierToSha1(String identifier) {

        if (identifier.length() != NUM_ID_CHARS) {
            throw new IllegalArgumentException("invalid identifier");
        }

        BigInteger sha1 = BigInteger.ZERO;

        for (int i = 0; i < identifier.length(); i++) {
            sha1 = sha1.shiftLeft(FIELD_BITS);
            BigInteger bits = DECODING.get(identifier.substring(i, i + 1));
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
            DOMValidateContext context = createContext(node);

            XMLSignature signature = extractXmlSignature(context);

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

    private static DOMValidateContext createContext(Node signatureXml) {
        DOMValidateContext context = new DOMValidateContext(
                new X509KeySelector(), signatureXml);

        return context;
    }

    private static XMLSignature extractXmlSignature(DOMValidateContext context)
            throws MarshalException {
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

        // This can throw a NPE when the signature element is empty or
        // malformed. Catch this explicitly.
        XMLSignature signature = null;
        try {
            signature = factory.unmarshalXMLSignature(context);
        } catch (NullPointerException e) {
            throw new MetadataException("invalid signature element");
        }

        return signature;
    }

    public static Object[] isCertificateVerified(Node signatureXml,
            KeyStore anchors, Collection<X509CRL> crls) {

        try {
            DOMValidateContext context = createContext(signatureXml);
            XMLSignature signature = extractXmlSignature(context);

            KeyInfo keyInfo = signature.getKeyInfo();

            PKIXBuilderParameters params = createPKIXBuilderParameters(anchors,
                    crls, keyInfo);

            boolean isRevoked = isCertRevoked(keyInfo, crls);
            if (isRevoked) {
                return new Object[] { Boolean.FALSE, "Certificate is revoked." };
            }

            /*
             * If build() returns successfully, the certificate is valid. More
             * details about the valid path can be obtained through the
             * PKIXBuilderResult. If no valid path can be found, a
             * CertPathBuilderException is thrown.
             */
            return buildCertPath(params);

        } catch (MarshalException e) {
            return new Object[] { Boolean.FALSE, e.getMessage() };
        }
    }

    private static boolean isCertRevoked(KeyInfo keyInfo,
            Collection<X509CRL> crls) {
        boolean isRevoked = false;

        X509Certificate cert = extractX509CertFromKeyInfo(keyInfo);

        if (cert != null) {
            for (X509CRL c : crls) {
                if (c.isRevoked(cert)) {
                    isRevoked = true;
                }
            }
        }

        return isRevoked;
    }

    public static X509Certificate extractX509CertFromKeyInfo(KeyInfo keyInfo) {

        List<X509Certificate> certs = extractX509CertChainFromKeyInfo(keyInfo);

        if (certs.size() > 0) {
            return certs.get(0);
        } else {
            return null;
        }
    }

    public static List<X509Certificate> extractX509CertChainFromKeyInfo(
            KeyInfo keyInfo) {
        List<X509Certificate> chain = new ArrayList<X509Certificate>();

        List<?> keyInfoContent = keyInfo.getContent();
        for (Object o : keyInfoContent) {
            if (o instanceof X509Data) {
                X509Data x509Data = (X509Data) o;
                List<?> x509DataContent = x509Data.getContent();
                for (Object obj2 : x509DataContent) {
                    if (obj2 instanceof X509Certificate) {
                        chain.add((X509Certificate) obj2);
                    }
                }
            }
        }

        return chain;
    }

    public static X509Certificate extractX509CertFromNode(Node signatureXml) {
        DOMValidateContext context = createContext(signatureXml);
        XMLSignature signature;
        try {
            signature = extractXmlSignature(context);
        } catch (MarshalException e) {
            throw new MetadataException(e.getMessage());
        }

        KeyInfo keyInfo = signature.getKeyInfo();

        X509Certificate cert = extractX509CertFromKeyInfo(keyInfo);

        return cert;
    }

    private static PKIXBuilderParameters createPKIXBuilderParameters(
            KeyStore anchors, Collection<X509CRL> crls, KeyInfo keyInfo) {
        List<X509Certificate> chain = extractX509CertChainFromKeyInfo(keyInfo);

        X509CertSelector target = new X509CertSelector();
        target.setCertificate(chain.get(0));

        try {

            PKIXBuilderParameters params = new PKIXBuilderParameters(anchors,
                    target);
            CertStoreParameters intermediates = new CollectionCertStoreParameters(
                    chain);
            params.addCertStore(CertStore.getInstance("Collection",
                    intermediates));

            if (crls.size() > 0) {
                CertStoreParameters revoked = new CollectionCertStoreParameters(
                        crls);
                params.addCertStore(CertStore
                        .getInstance("Collection", revoked));
            } else {
                // Disable CRL checks
                params.setRevocationEnabled(false);
            }

            return params;

        } catch (KeyStoreException e) {
            throw new MetadataException(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new MetadataException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new MetadataException(e.getMessage());
        }

    }

    private static Object[] buildCertPath(PKIXBuilderParameters params) {
        try {
            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");

            PKIXCertPathBuilderResult r = (PKIXCertPathBuilderResult) builder
                    .build(params);
            String builderResult = r.toString();

            return new Object[] { Boolean.TRUE, builderResult };

        } catch (NoSuchAlgorithmException e) {
            throw new MetadataException(e.getMessage());
        } catch (CertPathBuilderException e) {
            return new Object[] { Boolean.FALSE, e.getMessage() };
        } catch (InvalidAlgorithmParameterException e) {
            throw new MetadataException(e.getMessage());
        }
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

    public static void removeNodeFromParent(Node node) {
        Node parent = node.getParentNode();
        parent.removeChild(node);
    }

    public static void stripSignatureElements(Document doc) {

        NodeList nodes;

        // Careful, search needs to be done every time something's removed!
        for (//
        nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature"); //
        nodes.getLength() > 0; //
        nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature")) {

            removeNodeFromParent(nodes.item(0));
        }

        doc.normalizeDocument();
    }

    public static void writeStringToFile(String contents, File outputFile) {

        Writer os = null;
        try {

            FileOutputStream fos = new FileOutputStream(outputFile);
            os = new OutputStreamWriter(fos, "UTF-8");
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

    public static void clearEndorsementElement(Document doc) {

        NodeList nodes;

        // Careful, search needs to be done every time something's removed!
        // Remove all but last endorsement.
        for (//
        nodes = doc.getElementsByTagNameNS(SLREQ_NS_URI, "endorsement"); //
        nodes.getLength() > 1; //
        nodes = doc.getElementsByTagNameNS(SLREQ_NS_URI, "endorsement")) {

            removeNodeFromParent(nodes.item(0));
        }

        doc.normalizeDocument();

        nodes = doc.getElementsByTagNameNS(SLREQ_NS_URI, "endorsement");
        if (nodes.getLength() > 0) {
            Node endorsement = nodes.item(0);

            while (endorsement.hasChildNodes()) {
                Node child = endorsement.getLastChild();
                endorsement.removeChild(child);
            }
        }

        doc.normalizeDocument();
    }

    public static void fillEndorsementElement(Document doc, X509Info x509info,
            String defaultEmail) {

        String emailAddress = defaultEmail;

        NodeList nl = doc.getElementsByTagNameNS(SLREQ_NS_URI, "endorsement");
        if (nl.getLength() != 1) {
            throw new MetadataException(
                    "document must contain exactly 1 slreq:endorsement element");
        }

        if (x509info.email != null) {
            emailAddress = x509info.email;
        }

        if (emailAddress == null || "".equals(emailAddress)) {
            throw new MetadataException(
                    "cannot generate slreq:endorsement; email address not in cert or not provided");
        }

        if (!isValidEmailAddress(emailAddress)) {
            throw new MetadataException("invalid email address: "
                    + emailAddress);
        }

        Node endorsement = nl.item(0);

        if (!endorsement.hasChildNodes()) {

            endorsement.appendChild(createTimestampElement(doc));

            Element email = createSlreqElement(doc, "email");
            email.setTextContent(emailAddress);

            Element subject = createSlreqElement(doc, "subject");
            subject.setTextContent(x509info.subject);

            Element issuer = createSlreqElement(doc, "issuer");
            issuer.setTextContent(x509info.issuer);

            Element endorser = createSlreqElement(doc, "endorser");
            Attr attr = doc.createAttributeNS(RDF_NS_URI, "parseType");
            attr.setTextContent("Resource");
            endorser.setAttributeNodeNS(attr);
            endorser.appendChild(email);
            endorser.appendChild(subject);
            endorser.appendChild(issuer);

            endorsement.appendChild(endorser);
        }

        doc.normalizeDocument();
    }

    public static boolean isValidEmailAddress(String email) {

        try {
            new InternetAddress(email);
            String[] parts = email.split("@");
            return parts.length == 2 && !"".equals(parts[0])
                    && !"".equals(parts[1]);
        } catch (AddressException e) {
            return false;
        }
    }

    private static Element createSlreqElement(Document doc, String name) {
        return doc.createElementNS(SLREQ_NS_URI, name);

    }

    public static DateFormat getDateFormat() {

        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        return format;
    }

    private static Element createTimestampElement(Document doc) {
        Element e = doc.createElementNS(DCTERMS_NS_URI, "created");
        String datetime = getDateFormat().format(new Date());
        e.setTextContent(datetime);
        return e;
    }

}
