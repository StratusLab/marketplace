package eu.stratuslab.marketplace;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
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

    final public X509Certificate cert;

    final public PrivateKey privateKey;

    final public String subject;

    final public String issuer;

    final public String email;

    final public String commonName;

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

}
