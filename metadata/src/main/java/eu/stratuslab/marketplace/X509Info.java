package eu.stratuslab.marketplace;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;

@SuppressWarnings("restriction")
public class X509Info {

    public static final Pattern COMMON_NAME = Pattern
            .compile(".*CN=(.*?),[A-Z]{1,2}=.*");

    public static final int RFC822_NAME = 1;

    public final X509Certificate cert;

    public final PrivateKey privateKey;

    public final String subject;

    public final String issuer;

    public final String email;

    public final String commonName;

    public X509Info(X509Certificate cert) {
        this(cert, null);
    }

    public X509Info(X509Certificate cert, PrivateKey privateKey) {
        this.cert = cert;
        this.privateKey = privateKey;

        X500Principal subject = cert.getSubjectX500Principal();
        this.subject = subject.getName();

        X500Principal issuer = cert.getIssuerX500Principal();
        this.issuer = issuer.getName();

        email = extractEmailAddress(cert);

        commonName = extractCommonName(this.subject);

    }

    public KeyPair getKeyPair() {
        return new KeyPair(cert.getPublicKey(), privateKey);
    }

    public X509Data getX509Data(KeyInfoFactory factory) {
        List<X509Certificate> x509Content = Collections.singletonList(cert);
        return factory.newX509Data(x509Content);
    }

    public KeyInfo getKeyInfo(KeyInfoFactory factory) {
        List<X509Data> data = Collections.singletonList(getX509Data(factory));
        return factory.newKeyInfo(data);
    }

    public static String extractCommonName(String dn) {
        Matcher m = COMMON_NAME.matcher(dn);
        return (m.matches()) ? m.group(1) : null;
    }

    public static String extractEmailAddress(X509Certificate cert) {
        return extractEmailAddress(cert, null);
    }

    public static String extractEmailAddress(X509Certificate cert,
            String defaultEmail) {

        String email = defaultEmail;

        try {

            Collection<List<?>> entries = cert.getSubjectAlternativeNames();

            // Unfortunately, if there are no entries then null is returned
            // rather than an empty collection.
            if (entries != null) {
                for (List<?> entry : entries) {
                    int type = ((Integer) entry.get(0)).intValue();
                    if (type == RFC822_NAME) {
                        email = (String) entry.get(1);
                        break;
                    }
                }
            }

        } catch (CertificateParsingException consumed) {
            // do nothing; return the default value that's already set
        }

        return email;
    }
}
