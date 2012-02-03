package eu.stratuslab.marketplace.server.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import javax.security.auth.x500.X500Principal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.util.ResourceTestBase;

public class EndorserWhitelistTest extends ResourceTestBase {
	
	private String password = "XYZXYZ";
	
	private String classPrefix = "/eu/stratuslab/marketplace/server/resources/";
	
	@Test
	public void testCertVerificationWithUnverifiedCert() throws Exception {
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream(classPrefix + "valid-indate-signature.xml"));
				
		EndorserWhitelist whitelist = getWhitelistInstance("test-ca.jks",
				"test-crl-empty.pem", "test-whitelist");
						
		boolean verified = whitelist.isCertVerified(metadata);
		
		assertFalse(verified);
	}
	
	@Test
	public void testCertVerificationWithVerifiedCert() throws Exception {
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream(classPrefix + "valid-ca-signed.xml"));
				
		EndorserWhitelist whitelist = getWhitelistInstance("test-ca.jks",
				"test-crl-empty.pem", "test-whitelist");
						
		boolean verified = whitelist.isCertVerified(metadata);
		
		assertTrue(verified);
	}
	
	@Test
	public void testCertVerificationWithRevokedCert() throws Exception {
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream(classPrefix + "valid-ca-signed.xml"));
				
		EndorserWhitelist whitelist = getWhitelistInstance("test-ca.jks",
				"test-crl-revoked.pem", "test-whitelist");
						
		boolean verified = whitelist.isCertVerified(metadata);
		
		assertFalse(verified);
	}
	
	@Test
	public void testWhitelistWithValidUnlistedCert() throws Exception {
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream(classPrefix + "valid-ca-signed.xml"));
				
		EndorserWhitelist whitelist = getWhitelistInstance("test-ca.jks",
				"test-crl-empty.pem", "test-whitelist");
						
		boolean listed = whitelist.isEndorserWhitelisted(metadata);
		
		assertFalse(listed);
	}
	
	@Test
	public void testWhitelistWithValidListedCert() throws Exception {
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream(classPrefix + "valid-ca-signed.xml"));
				
		EndorserWhitelist whitelist = getWhitelistInstance("test-ca.jks",
				"test-crl-empty.pem", "test-whitelist-entry");
						
		boolean listed = whitelist.isEndorserWhitelisted(metadata);
		
		assertTrue(listed);
	}
	
	@Test
	public void testWhitelistWithInvalidListedCert() throws Exception {
		Document metadata = extractXmlDocument(this.getClass()
				.getResourceAsStream(classPrefix + "valid-ca-signed.xml"));
				
		EndorserWhitelist whitelist = getWhitelistInstance("test-ca.jks",
				"test-crl-revoked.pem", "test-whitelist-entry");
						
		boolean listed = whitelist.isEndorserWhitelisted(metadata);
		
		assertFalse(listed);
	}
			
	private EndorserWhitelist getWhitelistInstance(String truststore, 
			String crl, String whitelistFile) throws Exception {
		
		KeyStore anchors = createKeyStore(truststore, password);
		Collection<X509CRL> crls = loadCrls(crl);
		List<X500Principal> list = loadWhitelist(whitelistFile);
		
		EndorserWhitelist whitelist = new EndorserWhitelist(anchors, crls, list);
		
		return whitelist;
	}
	
	private KeyStore createKeyStore(String filename, String password) throws Exception {
		InputStream is = EndorserWhitelistTest.class
		.getResourceAsStream(classPrefix + filename);
		
		KeyStore anchors = KeyStore.getInstance(KeyStore.getDefaultType());
		try {
			anchors.load(is, password.toCharArray());
		} finally {
			if (is != null) {
				is.close();
			}
		}

		return anchors;
	}
	
	private Collection<X509CRL> loadCrls(String filename) throws Exception {
		InputStream is = EndorserWhitelistTest.class
		.getResourceAsStream(classPrefix + filename);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		X509CRL crl = null;

		try {
			crl = (X509CRL)cf.generateCRL(is);
		} finally {
			closeReliably(is);
		}
		Collection<X509CRL> crls = new ArrayList<X509CRL>();
		crls.add(crl);

		return crls;
	}
	
	private List<X500Principal> loadWhitelist(String filename) throws Exception {
		List<X500Principal> lines = new ArrayList<X500Principal>();

		InputStream is = EndorserWhitelistTest.class
		.getResourceAsStream(classPrefix + filename);
		
		InputStreamReader fileReader = new InputStreamReader(is);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			lines.add(new X500Principal(line));
		}

		closeReliably(bufferedReader);
		closeReliably(fileReader);
		closeReliably(is);
		
		return lines;
	}
	
}
