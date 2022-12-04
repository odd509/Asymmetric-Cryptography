import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.*;

public class Client {
    public static void main(String[] args) throws Exception {

        System.out.println("Client started...");
        System.out.println("My MAC: " + getMac());
        System.out.println("My Disk ID: " + getDriverSerialNumber());
        System.out.println("My Motherboard ID: " + getSystemMotherBoard_SerialNumber());
        File licenseFile = new File("license.txt");
        byte[] signature;

        // Checking whether the license file exists.
        if (licenseFile.exists()) {
            FileInputStream licenseIStream = new FileInputStream(licenseFile);
            signature = new byte[(int) licenseFile.length()];
            licenseIStream.read(signature);
            licenseIStream.close();

            // Checking whether the license file is genuine or corrupted
            if (verifySig(hash(getHwSpecificInfo()), signature, getPublicKey())) {
                System.out.println("Succeed. The license is correct.");
            } else {
                System.out.println("The license file has been broken!!");
                // if the license file is corrupt, deleting it and calling the main function
                // in order to create a new license
                licenseFile.delete();
                Client.main(args);
            }

        } else { // if the license file does not exist
            System.out.println("Client -- License file is not found.");
            System.out.println("Client -- Raw License Text: " + getHwSpecificInfo());
            // encrypting and sending the data to the License manager
            byte[] encryptedData = encryptHwSpecificInfo();
            String encryptedString = new String(encryptedData, StandardCharsets.UTF_8);
            System.out
                    .println("Client -- Encrypted License Text: " + encryptedString);

            byte[] hash = hash(getHwSpecificInfo());
            String[] hexadecimal = new String[hash.length];
            for (int i = 0; i < hash.length; i++) {
                hexadecimal[i] = String.format("%02x", hash[i]);
            }

            String hexString = "";
            for (String s : hexadecimal) {
                hexString += s;
            }

            System.out.println(
                    "Client -- MD5 License Text: " + hexString);
            signature = LicenseManager.main(encryptedData);

            // verifying the signature
            if (verifySig(hash(getHwSpecificInfo()), signature, getPublicKey())) {
                // creating the license text file
                try (FileOutputStream fos = new FileOutputStream(licenseFile)) {
                    fos.write(signature);
                    System.out.println(
                            "Client -- Succeed. The license file content is secured and signed by the server.");
                }
            }

        }
    }

    /**
     * Reads and instantiates the public key object from the public key file.
     * 
     * @return public key object
     */
    public static PublicKey getPublicKey() {
        File publicKeyFile = new File("public.key");

        try {

            FileInputStream publicKeyIStream = new FileInputStream(publicKeyFile);
            byte[] keyBytes = new byte[(int) publicKeyFile.length()];
            publicKeyIStream.read(keyBytes);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            publicKeyIStream.close();
            return publicKey;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Encrypts the hardware data with javax.crypto cipher class'
     * "RSA/ECB/PKCS1Padding" scheme.
     * 
     * @return encrypted data
     */
    public static byte[] encryptHwSpecificInfo() {
        PublicKey publicKey = getPublicKey();
        String hwSpecificInfo = getHwSpecificInfo();

        Cipher encryptCipher;
        try {
            encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] hwBytes = hwSpecificInfo.getBytes(StandardCharsets.UTF_8);
            byte[] hwEncrypted = encryptCipher.doFinal(hwBytes);

            return hwEncrypted;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static String getHwSpecificInfo() {
        String username = "abt";
        String serialNumber = "1234-5678-9012";
        String hwSpecificInfo = username + "$" + serialNumber;

        hwSpecificInfo += "$" + getMac() + "$" + getDriverSerialNumber() + "$" + getSystemMotherBoard_SerialNumber();

        return hwSpecificInfo;
    }

    public static String getMac() {

        InetAddress localHost;
        String macAddress = "";

        try {
            localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            byte[] hardwareAddress = ni.getHardwareAddress();

            String[] hexadecimal = new String[hardwareAddress.length];
            for (int i = 0; i < hardwareAddress.length; i++) {
                hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
            }
            macAddress = String.join(":", hexadecimal);

        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }

        return macAddress;
    }

    public static String getDriverSerialNumber() {
        String result = "";
        try {
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs = "Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n"
                    + "Set colDrives = objFSO.Drives\n"
                    + "Set objDrive = colDrives.item(\"" + "C" + "\")\n"
                    + "Wscript.Echo objDrive.SerialNumber"; // see note
            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.trim();
    }

    /**
     * Returns motherboard serial number
     * 
     * @return motherboard serial number
     */
    public static String getSystemMotherBoard_SerialNumber() {

        String result = "";
        try {
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs = "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
                    + "Set colItems = objWMIService.ExecQuery _ \n"
                    + "   (\"Select * from Win32_BaseBoard\") \n"
                    + "For Each objItem in colItems \n"
                    + "    Wscript.Echo objItem.SerialNumber \n"
                    + "    exit for  ' do the first cpu only! \n"
                    + "Next \n";

            fw.write(vbs);
            fw.close();

            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
        } catch (Exception E) {
            System.err.println("Windows MotherBoard Exp : " + E.getMessage());
        }

        // hard coded this part because my custom laptops motherboard id returned as
        // "Not Applicable"
        if (result.trim().equals("Not Applicable")) {
            result = "201075710502043";
        }

        return result.trim();

    }

    /**
     * Method for get Linux Machine MotherBoard Serial Number
     * 
     * @return
     */
    private static String GetLinuxMotherBoard_serialNumber() {
        String command = "dmidecode -s baseboard-serial-number";
        String sNum = null;
        try {
            Process SerNumProcess = Runtime.getRuntime().exec(command);
            BufferedReader sNumReader = new BufferedReader(new InputStreamReader(SerNumProcess.getInputStream()));
            sNum = sNumReader.readLine().trim();
            SerNumProcess.waitFor();
            sNumReader.close();
        } catch (Exception ex) {
            System.err.println("Linux Motherboard Exp : " + ex.getMessage());
            sNum = null;
        }
        return sNum;
    }

    /**
     * Verifies the given signature by creating a new signature instance from the
     * original data.
     * 
     * @param digestToVerify hash of the original hardware data
     * @param sigToVerify    signature to verify
     * @param pubKey         public key instance
     * @return boolean
     */
    public static boolean verifySig(byte[] digestToVerify, byte[] sigToVerify, PublicKey pubKey) {
        boolean verifies = false;
        try {
            Signature sig = Signature.getInstance("SHA256WithRSA");
            sig.initVerify(pubKey);
            sig.update(digestToVerify);
            verifies = sig.verify(sigToVerify);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return false;
        }
        return verifies;
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
            return hashedBytes;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}
