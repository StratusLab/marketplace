package eu.stratuslab.marketplace.server.utils;

import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_ENABLED;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_LOCATION;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_TRUSTSTORE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.WHITELIST_CRL;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.dsig.XMLSignature;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.stratuslab.marketplace.metadata.MetadataException;
import eu.stratuslab.marketplace.metadata.MetadataUtils;
import eu.stratuslab.marketplace.metadata.ValidateXMLSignature;
import eu.stratuslab.marketplace.server.cfg.Configuration;

public class EndorserWhitelist {
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private boolean enabled = false;
	private List<X500Principal> whitelist = new ArrayList<X500Principal>();
	private KeyStore truststore;

	private Collection<X509CRL> crls = new ArrayList<X509CRL>();
	private String crlFileExt;
	private String crlLocation;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> reloadCrls = null;
	
    public EndorserWhitelist(){
    	this.enabled = Configuration.getParameterValueAsBoolean(WHITELIST_ENABLED);	
		
		String location = Configuration.getParameterValue(WHITELIST_LOCATION);
		String truststore = Configuration.getParameterValue(WHITELIST_TRUSTSTORE);
		String password = Configuration.getParameterValue(WHITELIST_PASSWORD);
		crlLocation = Configuration.getParameterValue(WHITELIST_CRL);
        
		if(Configuration.getParameterValueAsBoolean(WHITELIST_ENABLED)){
			loadTrustStore(truststore, password);	
			loadWhitelist(location);
			loadCrls();
			
			reloadCrls = 
      		  scheduler.scheduleWithFixedDelay(new ReloadCRLs(), 12, 12, TimeUnit.HOURS);
		}
		
		if(this.enabled){
			LOGGER.warning("Endorser whitelist enabled.");
		}
    }

	public EndorserWhitelist(KeyStore anchors, Collection<X509CRL> crls,
			List<X500Principal> list) {
		this.enabled = true;
		this.truststore = anchors;
		this.crls = crls;
		this.whitelist = list;
	}
    
	private void loadCrls() {
		try {
			List<File> crlFiles = getCrlFiles(crlLocation);			
						
			for ( File f : crlFiles ){
				InputStream inStream = new FileInputStream(f);
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				X509CRL crl = (X509CRL)cf.generateCRL(inStream);
			
				this.crls.add(crl);
				
				closeReliably(inStream);
			}	

		} catch (FileNotFoundException e) {
			LOGGER.severe("Unable to load crl: " + e.getMessage());
			this.enabled = false;
		} catch (CertificateException e) {
			LOGGER.severe("Unable to load crl: " + e.getMessage());
			this.enabled = false;
		} catch (CRLException e) {
			LOGGER.severe("Unable to load crl: " + e.getMessage());
			this.enabled = false;
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

	private void loadTrustStore(String truststore, String password) {
		try {
			InputStream trustStoreInput = new FileInputStream(truststore);

			KeyStore anchors = KeyStore.getInstance(KeyStore.getDefaultType());
			anchors.load(trustStoreInput, password.toCharArray());

			this.truststore = anchors;

			closeReliably(trustStoreInput);
			
		} catch(IOException e){
			LOGGER.severe("Unable to load truststore: " + e.getMessage());
			this.enabled = false;
		} catch (KeyStoreException e) {
			LOGGER.severe("Unable to load truststore: " + e.getMessage());
			this.enabled = false;
		} catch (NoSuchAlgorithmException e) {
			LOGGER.severe("Unable to load truststore: " + e.getMessage());
			this.enabled = false;
		} catch (CertificateException e) {
			LOGGER.severe("Invalid certificate in truststore: " + e.getMessage());
			this.enabled = false;
		}
	}

	private void loadWhitelist(String location) {
		List<X500Principal> lines = new ArrayList<X500Principal>();

		try {
			FileReader fileReader = new FileReader(location);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				lines.add(new X500Principal(line));
			}

			closeReliably(bufferedReader);
			closeReliably(fileReader);

		}catch(FileNotFoundException e){
			LOGGER.severe("Unable to find whitelist file: " + location);
			this.enabled = false;
		} catch (IOException e) {
			LOGGER.severe("Error reading whitelist file. " + e.getMessage());
			this.enabled = false;
		}

		this.whitelist = lines;
	}

	public void stop(){
		if(reloadCrls != null){
			reloadCrls.cancel(true);
		}
	}
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public boolean isCertVerified(Document doc){
		boolean verified = false;
		try {
			ValidateXMLSignature.validateCertificate(doc, this.truststore, this.crls);
			verified = true;
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
	
	public static void closeReliably(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException consumed) {
			}
		}
	}
	
	public class CrlFileFilter implements FileFilter {

		public boolean accept(File arg0) {

			if (arg0.getName().endsWith(crlFileExt)){
				return true;
			}

			return false;
		}

	}
	
	 /**
     * A simple utility class that implements the timer that updates the CRLs.
     */
    class ReloadCRLs implements Runnable {
        /**
         * The actual method run by the timer.
         */
        public void run() {
           loadCrls();
        }
    }
	
}
