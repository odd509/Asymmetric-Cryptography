import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.*;

public class LicenseManager {

    public static byte[] main(byte[] hwEncrypted) {
        byte[] signature = signData(hash(decryptHwSpecificInfo(hwEncrypted)), getPrivateKey());
        return signature;
    }

    public static String decryptHwSpecificInfo(byte[] hwEncrypted) {
        PrivateKey privateKey = getPrivateKey();

        Cipher decryptCipher;
        try {
            decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

            String decrypted = new String(decryptCipher.doFinal(hwEncrypted));

            return decrypted;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

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

    public static PrivateKey getPrivateKey() {
        File publicKeyFile = new File("private.key");

        try {

            FileInputStream publicKeyIStream = new FileInputStream(publicKeyFile);
            byte[] keyBytes = new byte[(int) publicKeyFile.length()];
            publicKeyIStream.read(keyBytes);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            publicKeyIStream.close();
            return privateKey;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] hash(String hwSpecificInfo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashedBytes = digest.digest(hwSpecificInfo.getBytes(StandardCharsets.UTF_8));
            return hashedBytes;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;

    }
}