/**
 * Created as part of the StratusLab project (http://stratuslab.eu),
 * co-funded by the European Commission under the Grant Agreement
 * INSFO-RI-261552.
 *
 * Copyright (c) 2011
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.stratuslab.marketplace.server.utils;

import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_ENABLED;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_LOCATION;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_TRUSTSTORE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_CRL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.emi.security.authn.x509.NamespaceCheckingMode;
import eu.emi.security.authn.x509.ValidationResult;
import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.OpensslCertChainValidator;
import eu.stratuslab.marketplace.metadata.MetadataException;
import eu.stratuslab.marketplace.metadata.MetadataUtils;
import eu.stratuslab.marketplace.server.cfg.Configuration;

public class EndorserWhitelist {
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private boolean enabled = false;
	
	private List<X500Principal> whitelist = new ArrayList<X500Principal>();
	private List<String> crls = new ArrayList<String>();
	private String crlFileExt;
	private String crlLocation;
	
    private X509CertChainValidator validator = null;
    
    public static final Long DEFAULT_UPDATE_INTERVAL = //
        10L * 60L * 1000L; // 10min
    
    public EndorserWhitelist(){
    	
    	enabled = Configuration.getParameterValueAsBoolean(WHITELIST_ENABLED);	
		
		String location = Configuration.getParameterValue(WHITELIST_LOCATION);
		String truststore = Configuration.getParameterValue(WHITELIST_TRUSTSTORE);
		String password = Configuration.getParameterValue(WHITELIST_PASSWORD);
		crlLocation = Configuration.getParameterValue(WHITELIST_CRL);
        
		if(Configuration.getParameterValueAsBoolean(WHITELIST_ENABLED)){
			
			loadWhitelist(location);
			
			if(new File(truststore).isFile()){
				
				loadCrls();
				
				if(password != null){
					validator = createKeystoreValidator(truststore, password.toCharArray());
				} else {
					LOGGER.severe("No password specified");
					enabled = false;
				}
				
			} else if (new File(truststore).isDirectory()) {
				validator = createOpensslValidator(truststore);
			} else {
				LOGGER.severe("Truststore (" + truststore + ")  not found");
				enabled = false;
			}
		}
		
		if(enabled){
			LOGGER.warning("Endorser whitelist enabled.");
		}
    }

	private X509CertChainValidator createOpensslValidator(String truststore) {
		
		OpensslCertChainValidator validator = new OpensslCertChainValidator(
				truststore, 
				NamespaceCheckingMode.EUGRIDPMA_AND_GLOBUS, 
				DEFAULT_UPDATE_INTERVAL);
			
		return validator;
	}

	private X509CertChainValidator createKeystoreValidator(String truststore, char[] password) {
		KeystoreCertChainValidator validator = null;
		
		try {
			String type = "JKS";
			if(truststore.endsWith(".jks")){
				type = "JKS";
			} else if(truststore.endsWith(".p12")){
				type = "PKCS12";
			}
			
			validator = new KeystoreCertChainValidator(
					truststore, 
					password,
					type, 
					DEFAULT_UPDATE_INTERVAL);
			
			validator.setCrls(crls);
			validator.setCRLUpdateInterval(DEFAULT_UPDATE_INTERVAL);
			
		} catch (KeyStoreException e) {
			LOGGER.severe("Error creating cert validator: " + e.getMessage());
			this.enabled = false;
		} catch (IOException e) {
			LOGGER.severe("Error creating cert validator: " + e.getMessage());
			this.enabled = false;
		}
		
		return validator;
	}

	public EndorserWhitelist(String truststore, String password, String crl,
			List<X500Principal> list) {
		
		whitelist = list;
		crls.add(crl);
		
		validator = createKeystoreValidator(truststore, password.toCharArray());
	}

	private void loadCrls() {
		
		List<File> crlFiles = getCrlFiles(crlLocation);			
		
		for ( File f : crlFiles ){
			crls.add(f.getAbsolutePath());
		}	
		
	}

	private List<File> getCrlFiles(String crlLocation) {
		
		List<File> crlFiles = new ArrayList<File>();
		
		File crls = new File(crlLocation);
				
		if(crls.isDirectory()){
			crlFiles = Arrays.asList(crls.listFiles());
		} else if (crls.isFile()) {
	        crlFiles.add(crls);
		} else {
			crlFiles = getFilesFromPattern(crls);			
		}
		
		return crlFiles;
	}

	private List<File> getFilesFromPattern(File crls) {
		
		List<File> crlFiles = new ArrayList<File>(); 
		
		String[] filePattern = crls.getName().split("\\*");
		
		if(filePattern.length == 2){
			crlFileExt = filePattern[1];
			
			File crlsDir = crls.getParentFile();
			
			if(crlsDir != null){
				crlFiles = Arrays.asList(crlsDir.listFiles(new CrlFileFilter()));
			}
		}
		
		return crlFiles;
	}

	private void loadWhitelist(String location) {
		
		List<X500Principal> lines = new ArrayList<X500Principal>();

		try {
			Reader fileReader = new InputStreamReader(
					new FileInputStream(location), "UTF-8");
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				lines.add(new X500Principal(line));
			}

			MetadataFileUtils.closeReliably(bufferedReader);
			MetadataFileUtils.closeReliably(fileReader);

		}catch(FileNotFoundException e){
			LOGGER.severe("Unable to find whitelist file: " + location);
			this.enabled = false;
		} catch (IOException e) {
			LOGGER.severe("Error reading whitelist file. " + e.getMessage());
			this.enabled = false;
		}

		this.whitelist = lines;
	}

	public boolean isEnabled(){
		return enabled;
	}
	
	public boolean isCertVerified(Document doc){
		
		boolean verified = false;
		try {
			Node signature = extractSignature(doc);
			KeyInfo keyInfo = MetadataUtils.extractKeyInfoFromNode(signature);
			X509Certificate[] chain = extractX509CertChainFromKeyInfo(keyInfo);        
            
			ValidationResult result = validator.validate(chain);
			verified = result.isValid();
						
		} catch(MetadataException e){
			verified = false;
		}
		
		return verified;
	}
	
	public boolean isEndorserWhitelisted(Document doc){
		
		String endorser = getCertSubject(doc);
		
		if(!isCertVerified(doc)){
			return false;
		}
		
		boolean isListed = isEndorserListed(endorser);
		if(isListed){
			LOGGER.info("Endorser " + endorser + " found in whitelist.");
		}
		
		return isListed;
	}
	
	private String getCertSubject(Document doc){
		
		String endorser = "";
		
		NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
        "Signature");
    	
		if (nl.getLength() > 0) {
    		Node node = nl.item(0);
    	
    		X509Certificate cert = MetadataUtils.extractX509CertFromNode(node);
    		Map<String, String> endorsement = MetadataUtils.extractEndorserInfoFromCert(cert);
    	
    		endorser = endorsement.get("subject");
    	}
		
		return endorser;	
	}
	
	private boolean isEndorserListed(String endorser){
		
		for(X500Principal s : this.whitelist){
			if(s.getName().equals(endorser))
				return true;
		}
		
		return false;
	}
	
	public class CrlFileFilter implements FileFilter {

		public boolean accept(File arg0) {

			if (arg0.getName().endsWith(crlFileExt)){
				return true;
			}

			return false;
		}

	}
	
	private static Node extractSignature(Document doc){
    	NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
        "Signature");
    	Node node = null;
    	
    	if (nl.getLength() > 0) {
    		node = nl.item(0);
    	} else {
			throw new MetadataException("no signature");
		}
    	
    	return node;
    }
    
   public static X509Certificate[] extractX509CertChainFromKeyInfo(
            KeyInfo keyInfo) {
        List<X509Certificate> chain = MetadataUtils.extractX509CertChainFromKeyInfo(keyInfo);
        
        X509Certificate[] certs = new X509Certificate[chain.size()];
        for(int i = 0; i < chain.size(); i++){
        	certs[i] = chain.get(i);
        }
        
        return certs;
    }
    	
}
