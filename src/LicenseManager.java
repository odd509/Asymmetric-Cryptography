import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

public class LicenseManager {

    public static byte[] signData(byte[] digest, PrivateKey privKey) {
        byte[] signature = null;
        try {
            Signature rsa = Signature.getInstance("SHA256WithRSA");
            rsa.initSign(privKey);
            rsa.update(digest);
            signature = rsa.sign();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return signature;
    }
}