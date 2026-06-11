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

        String biosSerial;
        String motherboardSerial;
        String machineId;

        if (isWindows()) {
            biosSerial = executeCommand("wmic bios get serialnumber");
            motherboardSerial = executeCommand("wmic baseboard get serialnumber");
            machineId = executeCommand("wmic os get serialnumber");
        } else if (isLinux()) {
            biosSerial = executeCommand("cat /sys/class/dmi/id/product_serial");
            motherboardSerial = executeCommand("cat /sys/class/dmi/id/board_serial");
            machineId = executeCommand("cat /etc/machine-id");
        } else {
            biosSerial = "";
            motherboardSerial = "";
            machineId = "";
        }

        String macAddress = getMacAddress();

        String fingerprintSource =
                biosSerial +
                        motherboardSerial +
                        machineId +
                        macAddress;

        String hash = sha256Hash(SALT + fingerprintSource + SALT);

        return HardwareFingerprint.builder()
                .biosSerial(biosSerial)
                .motherboardSerial(motherboardSerial)
                .osSerial(machineId)
                .macAddress(macAddress)
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

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(process.getInputStream()))) {

                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .skip(1) // Skip WMIC header
                        .findFirst()
                        .orElse("");
            }

        } catch (Exception e) {
            return "";
        }
    }

//    private static String executeCommand(String command) {
//        try {
//            Process process = Runtime.getRuntime().exec(command);
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                String output = reader.lines().collect(Collectors.joining());
//                String[] lines = output.split("\\r?\\n");
//                if (lines.length > 1) {
//                    return lines[1].trim();
//                }
//                return "";
//            }
//        } catch (Exception e) {
//            return "";
//        }
//    }

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


    private static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase()
                .contains("win");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name")
                .toLowerCase()
                .contains("linux");
    }

    private static String getMacAddress() {
        try {
            for (java.net.NetworkInterface ni :
                    java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {

                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                    continue;
                }

                byte[] mac = ni.getHardwareAddress();

                if (mac == null || mac.length == 0) {
                    continue;
                }

                StringBuilder sb = new StringBuilder();

                for (byte b : mac) {
                    sb.append(String.format("%02X", b));
                }

                return sb.toString();
            }
        } catch (Exception ignored) {
        }

        return "";
    }
}
