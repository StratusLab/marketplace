package eu.stratuslab.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.XMLUtils;

@SuppressWarnings("restriction")
public class GenXmlSign {

    // Synopsis: java GenXmlSign [medatadalfile] [outputfile] [P12
    // GridCertificate] [P12 GridCertificate Password]
    //
    // where "medatadalfile" is the name of a file containing the XML document
    // to be signed, "outputfile" is the name of the file to store the
    // signed document.
    // SignatureMethod : RSA_SHA1, DSA_SHA1, ...
    // DigestMethod : SHA1, ...
    // P12 GridCertificate : means we are using PKCS12 keystore.
    // Gridcertificate Password.
    //

    public static final Pattern cnExtractionPattern = Pattern
            .compile(".*CN=([^,]*?),.*");

    public static void main(String[] args) throws Exception {

        if (args.length != 2 && args.length != 4) {
            System.out
                    .println("Usage: java GenXmlSign [medatadalfile] [outputfile] [P12 GridCertificate] [P12 GridCertificate Passwd]"
                            + " \n SignatureMethod to be modified in the code : (RSA_SHA1 | DSA_SHA1 |...) ");
            System.exit(1);
        }

        File metadataFile = new File(args[0]);
        File outputFile = new File(args[1]);

        XMLSignatureFactory factory = newXMLSignatureFactory();

        SignedInfo si = newSignedInfo(factory);

        // Create a KeyValue containing the PublicKey that was generated
        KeyInfoFactory kif = factory.getKeyInfoFactory();

        // Create a KeyPair
        KeyPair kp = null;
        KeyInfo ki = null;

        Object[] result = null;

        if (args.length == 4) {
            result = extractPKCS12Key(args[2], args[3], kif);
        }

        if (args.length == 2) {
            result = rsaInfo(kif);
        }

        kp = (KeyPair) result[0];
        ki = kif.newKeyInfo(Collections.singletonList(result[1]));

        // Instantiate the document to be signed
        DocumentBuilder db = XMLUtils.newDocumentBuilder(false);
        Document doc = db.parse(metadataFile);

        // Create a DOMSignContext and specify the RSA/DSA PrivateKey and
        // location of the resulting XMLSignature's parent element
        DOMSignContext dsc = new DOMSignContext(kp.getPrivate(), doc
                .getDocumentElement());

        // Create the XMLSignature (but don't sign it yet)
        XMLSignature signature = factory.newXMLSignature(si, ki);

        // Marshal, generate (and sign) the enveloped signature
        signature.sign(dsc);

        String signedContents = XMLUtils.documentToString(doc);

        writeStringToFile(signedContents, outputFile);

    }

    private static XMLSignatureFactory newXMLSignatureFactory() {
        return XMLSignatureFactory.getInstance("DOM");
    }

    private static SignedInfo newSignedInfo(XMLSignatureFactory factory)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        DigestMethod method = factory.newDigestMethod(DigestMethod.SHA1, null);
        Transform transform = factory.newTransform(Transform.ENVELOPED,
                (TransformParameterSpec) null);
        List<Transform> transforms = Collections.singletonList(transform);

        // Create a Reference to the enveloped document; in this case we are
        // signing the whole document, so a URI of "" signifies that.
        Reference ref = factory
                .newReference("", method, transforms, null, null);
        List<Reference> refs = Collections.singletonList(ref);

        CanonicalizationMethod cmethod = factory.newCanonicalizationMethod(
                CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                (C14NMethodParameterSpec) null);

        SignatureMethod smethod = factory.newSignatureMethod(
                SignatureMethod.RSA_SHA1, null);

        return factory.newSignedInfo(cmethod, smethod, refs);
    }

    private static void writeStringToFile(String contents, File outputFile) {

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

    private static KeyPair generateRSAKeyPair() {

        try {

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            return kpg.generateKeyPair();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] rsaInfo(KeyInfoFactory kif) throws KeyException {

        KeyPair kp = generateRSAKeyPair();

        KeyValue kv = kif.newKeyValue(kp.getPublic());

        return new Object[] { kp, kv };
    }

    private static Object[] readPKCS12KeyPair(String fname, String password) {

        InputStream fis = null;

        try {

            fis = new FileInputStream(fname);

            char[] pwchars = password.toCharArray();

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, pwchars);

            X509Certificate cert = null;
            KeyPair kp = null;

            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                Key key = ks.getKey(alias, pwchars);

                if (key instanceof PrivateKey) {
                    cert = (X509Certificate) ks.getCertificate(alias);
                    PublicKey publicKey = cert.getPublicKey();
                    kp = new KeyPair(publicKey, (PrivateKey) key);
                }
            }

            return new Object[] { kp, cert };

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException consumed) {
                }
            }
        }
    }

    private static void printX509Info(X509Certificate cert) {

        X500Principal principal = cert.getSubjectX500Principal();

        System.out.println(principal);
        System.out.println(cert.getIssuerX500Principal());
        System.out.println(extractCN(principal));

        System.out.println(extractEmailAddress(cert));

    }

    private static String extractCN(X500Principal principal) {

        String dn = principal.getName();
        Matcher m = cnExtractionPattern.matcher(dn);

        return (m.matches()) ? m.group(1) : "";
    }

    private static String extractEmailAddress(X509Certificate cert) {

        try {

            String email = null;

            for (List<?> entry : cert.getSubjectAlternativeNames()) {

                int type = ((Integer) entry.get(0)).intValue();
                if (type == 1) {
                    email = (String) entry.get(1);
                    break;
                }
            }

            return email;

        } catch (CertificateParsingException e) {

            // Unable to parse information, so must be no email address.
            return null;
        }
    }

    private static Object[] extractPKCS12Key(String fname, String password,
            KeyInfoFactory kif) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException {

        Object[] results = readPKCS12KeyPair(fname, password);
        KeyPair kp = (KeyPair) results[0];
        X509Certificate cert = (X509Certificate) results[1];

        // DEBUGGING
        printX509Info(cert);

        List<X509Certificate> x509Content = new ArrayList<X509Certificate>();
        x509Content.add(cert);

        X509Data xd = kif.newX509Data(x509Content);

        return new Object[] { kp, xd };
    }
}
