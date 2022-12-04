import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.*;

public class LicenseManager {

    /**
     * License manager's main function receives the RSA encrypted data in order to
     * "sign" it.
     * 
     * @param hwEncrypted RSA encrypted data
     * @return the signature (back to the Client side)
     */
    public static byte[] main(byte[] hwEncrypted) {
        System.out.println("LicenseManager service started...");
        byte[] signature = signData(hash(decryptHwSpecificInfo(hwEncrypted)), getPrivateKey());
        System.out.println("Server -- Digital Signature: " + new String(signature, StandardCharsets.UTF_8));
        return signature;
    }

    /**
     * Decrypts the received info with javax.crypto class' RSA scheme.
     * 
     * @param hwEncrypted
     * @return the decrypted device specific info
     */
    public static String decryptHwSpecificInfo(byte[] hwEncrypted) {
        System.out.println("Server -- Server is being requested...");
        System.out.println("Server -- Incoming Encrypted Text: " + new String(hwEncrypted, StandardCharsets.UTF_8));
        // instantiating the private key from the given file
        PrivateKey privateKey = getPrivateKey();

        Cipher decryptCipher;
        try {
            decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

            String decrypted = new String(decryptCipher.doFinal(hwEncrypted));

            System.out.println("Server -- Decrypted Text: " + decrypted);
            return decrypted;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Signs the data using java security signature class ("SHA256WithRSA" scheme).
     * 
     * @param digest  the MD5 hashed data
     * @param privKey
     * @return signature of the hashed data
     */
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

    /**
     * Reads and instantiates the private key object from the given private key
     * file.
     * 
     * @return private key object
     */
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
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Hashes the given hardware specific data with MD5.
     * 
     * @param hwSpecificInfo hardware specific info
     * @return MD5 digest
     */
    public static byte[] hash(String hwSpecificInfo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashedBytes = digest.digest(hwSpecificInfo.getBytes(StandardCharsets.UTF_8));

            String[] hexadecimal = new String[hashedBytes.length];
            for (int i = 0; i < hashedBytes.length; i++) {
                hexadecimal[i] = String.format("%02x", hashedBytes[i]);
            }

            String hexString = "";
            for (String s : hexadecimal) {
                hexString += s;
            }

            System.out.println("Server -- MD5 Plain License Text: " + hexString);
            return hashedBytes;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;

    }
}