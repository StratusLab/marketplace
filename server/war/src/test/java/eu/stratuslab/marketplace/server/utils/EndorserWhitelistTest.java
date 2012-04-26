package eu.stratuslab.marketplace.server.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.security.auth.x500.X500Principal;

import java.util.ArrayList;
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
		
		List<X500Principal> list = loadWhitelist(whitelistFile);
		
		URL store = EndorserWhitelistTest.class
		.getResource(classPrefix + truststore);
		URL crlFile = EndorserWhitelistTest.class
		.getResource(classPrefix + crl);
		EndorserWhitelist whitelist = new EndorserWhitelist(store.getPath(), 
				password, crlFile.getPath(), list);
		
		return whitelist;
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
