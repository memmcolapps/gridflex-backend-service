package org.memmcol.gridflexbackendservice.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class LicenceSignerUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private LicenceSignerUtil() {
    }

    public static String sign(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    public static boolean verify(String data, String signature, String secretKey) {
        try {
            String expectedSignature = sign(data, secretKey);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
