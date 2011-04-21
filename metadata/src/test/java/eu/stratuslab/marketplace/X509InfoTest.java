package eu.stratuslab.marketplace;

import java.security.cert.X509Certificate;

import static org.junit.Assert.*;

import org.junit.Test;

public class X509InfoTest {

    @Test
    public void extractNullEmail() throws Exception {
        X509Certificate[] certs = X509UtilsTest.getSignedCert(null);
        X509Certificate userCert = certs[1];
        String result = X509Info.extractEmailAddress(userCert);
        assertNull(result);
    }

    @Test
    public void extractNonNullEmail() throws Exception {
        String email = "example@example.org";
        X509Certificate[] certs = X509UtilsTest.getSignedCert(email);
        X509Certificate userCert = certs[1];
        String result = X509Info.extractEmailAddress(userCert);
        assertEquals(email, result);
    }

}
