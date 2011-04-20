package eu.stratuslab.marketplace;

import static org.bouncycastle.asn1.x509.X509Extensions.AuthorityKeyIdentifier;
import static org.bouncycastle.asn1.x509.X509Extensions.SubjectAlternativeName;
import static org.bouncycastle.asn1.x509.X509Extensions.SubjectKeyIdentifier;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class X509UtilsTest {

    public static final long ONE_MINUTE_MILLIS = 60L * 1000;

    public static final long ONE_MONTH_MILLIS = 30L * 24L * 60L * 60L * 1000;

    public static final String KEYGEN_ALGORITHM = "RSA";

    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    // @Test
    // public void checkCertificateGenWithoutEmail() throws Exception {
    // X509Certificate userCert = getSignedCert(null);
    // assertNotNull(userCert);
    //
    // Collection<List<?>> altNames = userCert.getSubjectAlternativeNames();
    // assertNull(altNames);
    // }
    //
    // @Test
    // public void checkCertificateGenWithEmail() throws Exception {
    // X509Certificate userCert = getSignedCert("example@example.org");
    // assertNotNull(userCert);
    //
    // Collection<List<?>> altNames = userCert.getSubjectAlternativeNames();
    // assertNotNull(altNames);
    // }

    public static X509Certificate getSignedCert(String email) throws Exception {

        KeyPair caKeyPair = createKeyPair();
        KeyPair userKeyPair = createKeyPair();

        X509Certificate caCert = generateV1Certificate("CN=TEST_CA", caKeyPair);
        return generateV3Certificate("CN=TEST_USER", caCert, caKeyPair
                .getPrivate(), userKeyPair, email);
    }

    public static KeyPair createKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator
                .getInstance(KEYGEN_ALGORITHM);
        return generator.generateKeyPair();
    }

    public static Date[] getDates(long duration) {

        long begin = System.currentTimeMillis() - ONE_MINUTE_MILLIS;
        long end = begin + duration;

        return new Date[] { new Date(begin), new Date(end) };
    }

    public static X509Certificate generateV1Certificate(String dn,
            KeyPair keyPair) throws Exception {

        Date[] dates = getDates(2L * ONE_MONTH_MILLIS);

        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal dnName = new X500Principal(dn);

        certGen.setSerialNumber(BigInteger.ONE);
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(dates[0]);
        certGen.setNotAfter(dates[1]);
        certGen.setSubjectDN(dnName);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(SIGNATURE_ALGORITHM);

        // X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");
        return certGen.generate(keyPair.getPrivate());
    }

    public static X509Certificate generateV3Certificate(String dn,
            X509Certificate caCert, PrivateKey caKey, KeyPair keyPair,
            String email) throws Exception {

        Date[] dates = getDates(ONE_MONTH_MILLIS);

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal subjectName = new X500Principal(dn);

        certGen.setSerialNumber(BigInteger.ONE);
        certGen.setIssuerDN(caCert.getSubjectX500Principal());
        certGen.setNotBefore(dates[0]);
        certGen.setNotAfter(dates[1]);
        certGen.setSubjectDN(subjectName);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(SIGNATURE_ALGORITHM);

        certGen.addExtension(AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(keyPair.getPublic()));

        addSubjectAltName(certGen, email);

        // return certGen.generate(caKey, "BC"); // note: private key of CA
        return certGen.generate(caKey); // note: private key of CA
    }

    public static void addSubjectAltName(X509V3CertificateGenerator certGen,
            String email) {

        if (email != null) {
            GeneralName rfc822 = new GeneralName(GeneralName.rfc822Name, email);
            GeneralNames subjectAltName = new GeneralNames(rfc822);

            X509Extension extension = new X509Extension(false,
                    new DEROctetString(subjectAltName));

            certGen.addExtension(SubjectAlternativeName, false, extension
                    .getValue());
        }
    }

}
