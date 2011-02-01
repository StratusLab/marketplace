package eu.stratuslab.metadata;

import java.util.*;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dom.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.*;
import java.util.Collections;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

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
	public static void main(String[] args) throws Exception {

		// Create a DOM XMLSignatureFactory that will be used to generate the
		// enveloped signature
		if (args.length < 1) {
			System.out
					.println("Usage: java GenXmlSign [medatadalfile] [outputfile] [P12 GridCertificate] [P12 GridCertificate Passwd]"
							+ " \n SignatureMethod to be modified in the code : (RSA_SHA1 | DSA_SHA1 |...) ");
			System.exit(1);
		}

		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		// Create a Reference to the enveloped document (in this case we are
		// signing the whole document, so a URI of "" signifies that) and
		// also specify the SHA1 digest algorithm and the ENVELOPED Transform.
		Reference ref = fac.newReference("", fac.newDigestMethod(
				DigestMethod.SHA1, null), Collections.singletonList(fac
				.newTransform(Transform.ENVELOPED,
						(TransformParameterSpec) null)), null, null);

		// Create the SignedInfo
		SignedInfo si = fac
				.newSignedInfo(fac.newCanonicalizationMethod(
						CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null), fac
						.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
						Collections.singletonList(ref));

		// Create a KeyValue containing the PublicKey that was generated
		KeyInfoFactory kif = fac.getKeyInfoFactory();

		// Create a KeyPair
		KeyPair kp = null;
		KeyInfo ki = null;

		if (args.length == 4) {
			java.security.KeyStore ks = java.security.KeyStore
					.getInstance("PKCS12");
			java.io.FileInputStream fis = new java.io.FileInputStream(args[2]);

			ks.load(fis, args[3].toCharArray());

			// Retrieve the PrivateKey and its certificate chain using
			// the java.security.KeyStore API.
			Enumeration<String> aliases = ks.aliases();
			java.security.cert.X509Certificate cert = null;
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				// Get private key
				Key key = ks.getKey(alias, args[3].toCharArray());
				if (key instanceof PrivateKey) {
					// Get certificate of public key
					cert = (java.security.cert.X509Certificate) ks
							.getCertificate(alias);
					// Get public key
					PublicKey publicKey = cert.getPublicKey();
					// Return a key pair
					kp = new KeyPair(publicKey, (PrivateKey) key);

				}
			}
			List x509Content = new ArrayList();
			// x509Content.add(cert.getSubjectX500Principal().getName());
			x509Content.add(cert);
			X509Data xd = kif.newX509Data(x509Content);

			// Create a KeyInfo and add the KeyValue to it
			ki = kif.newKeyInfo(Collections.singletonList(xd));
		}

		if (args.length == 2) {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(1024);
			kp = kpg.generateKeyPair();
			KeyValue kv = kif.newKeyValue(kp.getPublic());
			ki = kif.newKeyInfo(Collections.singletonList(kv));
		}

		// Instantiate the document to be signed
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(
				new FileInputStream(args[0]));

		// Create a DOMSignContext and specify the RSA/DSA PrivateKey and
		// location of the resulting XMLSignature's parent element
		DOMSignContext dsc = new DOMSignContext(kp.getPrivate(),
				doc.getDocumentElement());

		// Create the XMLSignature (but don't sign it yet)
		XMLSignature signature = fac.newXMLSignature(si, ki);

		// Marshal, generate (and sign) the enveloped signature
		signature.sign(dsc);

		// output the resulting document
		OutputStream os = null;

		try {
			os = new FileOutputStream(args[1]);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer trans = tf.newTransformer();
			trans.transform(new DOMSource(doc), new StreamResult(os));
		} finally {
			if (os != null) {
				os.close();
			}
		}

	}

}
