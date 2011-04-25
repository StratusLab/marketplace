package eu.stratuslab.marketplace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.w3c.dom.Document;

public final class X509Utils {

    private X509Utils() {

    }

    public static KeyStore pkcs12ToKeyStore(File file, String password)
            throws FileNotFoundException {

        return pkcs12ToKeyStore(new FileInputStream(file), password);

    }

    public static KeyStore pkcs12ToKeyStore(InputStream is, String password) {

        try {

            char[] pwchars = password.toCharArray();

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, pwchars);

            return ks;

        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException consumed) {
                }
            }
        }
    }

    public static X509Info x509FromKeyStore(KeyStore keyStore, String password) {

        try {

            char[] pwchars = password.toCharArray();

            X509Certificate cert = null;
            Key key = null;

            Enumeration<String> aliases;
            aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                key = keyStore.getKey(alias, pwchars);

                if (key instanceof PrivateKey) {
                    cert = (X509Certificate) keyStore.getCertificate(alias);
                }
            }

            return new X509Info(cert, (PrivateKey) key);

        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    public static void signDocument(X509Info x509Info, Document doc) {

        try {

            // Fully normalize the document before trying to sign it.
            doc.normalizeDocument();

            XMLSignatureFactory factory = newXMLSignatureFactory();

            SignedInfo si = newSignedInfo(factory);

            KeyInfoFactory kif = factory.getKeyInfoFactory();

            KeyPair kp = x509Info.getKeyPair();
            KeyInfo ki = x509Info.getKeyInfo(kif);

            // Create a DOMSignContext and specify the RSA/DSA PrivateKey and
            // location of the resulting XMLSignature's parent element
            DOMSignContext dsc = new DOMSignContext(kp.getPrivate(), doc
                    .getDocumentElement());

            // Create the XMLSignature (but don't sign it yet)
            XMLSignature signature = factory.newXMLSignature(si, ki);

            // Marshal, generate (and sign) the enveloped signature
            signature.sign(dsc);

        } catch (MarshalException e) {
            throw new RuntimeException(e);
        } catch (XMLSignatureException e) {
            throw new RuntimeException(e);
        }

    }

    private static XMLSignatureFactory newXMLSignatureFactory() {
        return XMLSignatureFactory.getInstance("DOM");
    }

    private static SignedInfo newSignedInfo(XMLSignatureFactory factory) {

        try {

            DigestMethod method;
            method = factory.newDigestMethod(DigestMethod.SHA1, null);
            Transform transform = factory.newTransform(Transform.ENVELOPED,
                    (TransformParameterSpec) null);
            List<Transform> transforms = Collections.singletonList(transform);

            // Create a Reference to the enveloped document; in this case we are
            // signing the whole document, so a URI of "" signifies that.
            Reference ref = factory.newReference("", method, transforms, null,
                    null);
            List<Reference> refs = Collections.singletonList(ref);

            CanonicalizationMethod cmethod = factory.newCanonicalizationMethod(
                    CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                    (C14NMethodParameterSpec) null);

            SignatureMethod smethod = factory.newSignatureMethod(
                    SignatureMethod.RSA_SHA1, null);

            return factory.newSignedInfo(cmethod, smethod, refs);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

    }

}
