package eu.stratuslab.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.junit.Test;

public class X509InfoTest {

    @Test
    public void extractNullEmail() throws Exception {
        X509Certificate[] certs = X509UtilsTest.getCertificateChain(null);
        X509Certificate userCert = certs[1];
        String result = X509Info.extractEmailAddress(userCert);
        assertNull(result);
    }

    @Test
    public void extractNonNullEmail() throws Exception {
        String email = "example@example.org";
        X509Certificate[] certs = X509UtilsTest.getCertificateChain(email);
        X509Certificate userCert = certs[1];
        String result = X509Info.extractEmailAddress(userCert);
        assertEquals(email, result);
    }

    @Test
    public void expectNullCN() {
        assertNull(X509Info.extractCommonName(""));
    }

    @Test
    public void extractCNFromFullDN() {
        String expected = "Joe SMITH";
        String cn = X509Info.extractCommonName("CN=" + expected + ",O=MyInc");
        assertEquals(expected, cn);
    }

    @Test
    public void extractLonelyCN() {
        String expected = "Joe SMITH";
        String cn = X509Info.extractCommonName("CN=" + expected);
        assertEquals(expected, cn);
    }

    @Test
    public void checkX509Information() throws Exception {

        String email = "example@example.org";
        X509Certificate[] certs = X509UtilsTest.getCertificateChain(email);

        X509Certificate userCert = certs[1];

        X509Info x509Info = new X509Info(userCert);

        assertEquals(x509Info.subject, X509UtilsTest.USER_DN);
        assertEquals(x509Info.issuer, X509UtilsTest.ISSUER_DN);
        assertEquals(x509Info.email, email);
        assertEquals(x509Info.commonName, X509UtilsTest.USER_CN);
        assertEquals(x509Info.cert, userCert);
        assertNull(x509Info.privateKey);

    }

    @Test
    public void checkX509InformationWithFakePrivateKey() throws Exception {

        KeyPair pair = X509UtilsTest.createKeyPair();

        String email = "example@example.org";
        X509Certificate[] certs = X509UtilsTest.getCertificateChain(email);

        X509Certificate userCert = certs[1];

        X509Info x509Info = new X509Info(userCert, pair.getPrivate());

        assertEquals(x509Info.subject, X509UtilsTest.USER_DN);
        assertEquals(x509Info.issuer, X509UtilsTest.ISSUER_DN);
        assertEquals(x509Info.email, email);
        assertEquals(x509Info.commonName, X509UtilsTest.USER_CN);
        assertEquals(x509Info.cert, userCert);
        assertEquals(x509Info.privateKey, pair.getPrivate());

        KeyPair embeddedKeyPair = x509Info.getKeyPair();
        assertEquals(userCert.getPublicKey(), embeddedKeyPair.getPublic());
        assertEquals(pair.getPrivate(), embeddedKeyPair.getPrivate());

    }

}
