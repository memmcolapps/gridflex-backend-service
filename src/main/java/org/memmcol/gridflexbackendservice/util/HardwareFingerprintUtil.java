package org.memmcol.gridflexbackendservice.util;

import org.memmcol.gridflexbackendservice.model.licence.HardwareFingerprint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

public class HardwareFingerprintUtil {

    private static final String SALT = "GridFlex@Hardware2024!#$";

    private HardwareFingerprintUtil() {
    }

    public static HardwareFingerprint generateFingerprint() {
        String biosSerial = executeCommand("wmic bios get serialnumber");
        String motherboardSerial = executeCommand("wmic baseboard get serialnumber");
        String diskSerial = executeCommand("wmic diskdrive get serialnumber");
        String osSerial = executeCommand("wmic os get serialnumber");

        String concatenated = biosSerial + motherboardSerial + diskSerial + osSerial;
        String saltedInput = SALT + concatenated + SALT;
        String hash = sha256Hash(saltedInput);

        return HardwareFingerprint.builder()
                .biosSerial(biosSerial)
                .motherboardSerial(motherboardSerial)
                .diskSerial(diskSerial)
                .osSerial(osSerial)
                .hash(hash)
                .build();
    }

    public static boolean verifyFingerprint(String storedHash) {
        HardwareFingerprint current = generateFingerprint();
        return current.getHash().equals(storedHash);
    }

    private static String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().collect(Collectors.joining());
                String[] lines = output.split("\\r?\\n");
                if (lines.length > 1) {
                    return lines[1].trim();
                }
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
