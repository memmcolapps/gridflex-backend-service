package org.memmcol.gridflexbackendservice.util;

public final class LicenceSecurityConstants {

    private LicenceSecurityConstants() {}

    public static String getHmacKey() {
        return "GridFlex@HMAC@2024!@#$";
    }

    public static String getEncryptionKey() {
        return "GridFlex@2024SecretKey";
    }
}
