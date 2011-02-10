package eu.stratuslab.marketplace.metadata;

import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;

@SuppressWarnings("restriction")
public class X509KeySelector extends KeySelector {

    public KeySelectorResult select(KeyInfo keyInfo,
            KeySelector.Purpose purpose, AlgorithmMethod method,
            XMLCryptoContext context) throws KeySelectorException {

        PublicKey key = null;

        for (Object oinfo : keyInfo.getContent()) {

            key = extractX509Key(oinfo);

            if (key == null) {
                key = extractKeyValue(oinfo);
            }

            if (key != null && compatibleAlgorithms(method, key)) {
                return new SimpleKeySelectorResult(key);
            }
        }

        throw new KeySelectorException("No key found!");
    }

    private static PublicKey extractKeyValue(Object oinfo) {

        PublicKey key = null;

        if (oinfo instanceof KeyValue) {
            try {
                KeyValue kv = (KeyValue) oinfo;
                key = kv.getPublicKey();
            } catch (KeyException consumed) {
                key = null;
            }
        }

        return key;
    }

    private static PublicKey extractX509Key(Object info) {

        PublicKey key = null;

        if (info instanceof X509Data) {

            X509Data x509Data = (X509Data) info;

            for (Object o : x509Data.getContent()) {
                if (o instanceof X509Certificate) {
                    key = ((X509Certificate) o).getPublicKey();
                }
            }

        }
        return key;
    }

    private static boolean compatibleAlgorithms(AlgorithmMethod method,
            PublicKey key) {

        String algURI = method.getAlgorithm();
        String algName = key.getAlgorithm();

        boolean bothDSA = (algName.equalsIgnoreCase("DSA") && algURI
                .equalsIgnoreCase(SignatureMethod.DSA_SHA1));

        boolean bothRSA = (algName.equalsIgnoreCase("RSA") && algURI
                .equalsIgnoreCase(SignatureMethod.RSA_SHA1));

        return (bothDSA || bothRSA);
    }

    private static class SimpleKeySelectorResult implements KeySelectorResult {

        private PublicKey pk;

        SimpleKeySelectorResult(PublicKey pk) {
            this.pk = pk;
        }

        public Key getKey() {
            return pk;
        }
    }

}
