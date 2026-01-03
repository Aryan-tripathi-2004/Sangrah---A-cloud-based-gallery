package com.example.Auth.common.security;

import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for cryptographic operations.
 */
public final class CryptoUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private CryptoUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Generate a secure random string of specified length.
     *
     * @param length the length of the random string
     * @return a secure random string
     */
    public static String generateSecureRandomString(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate a secure random numeric code of specified length.
     *
     * @param length the length of the numeric code
     * @return a secure random numeric string
     */
    public static String generateSecureRandomNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Hash a string using SHA-256.
     *
     * @param input the input string
     * @return the SHA-256 hash in hexadecimal format
     */
    public static String sha256(String input) {
        return DigestUtils.sha256Hex(input);
    }

    /**
     * Hash a string using SHA-512.
     *
     * @param input the input string
     * @return the SHA-512 hash in hexadecimal format
     */
    public static String sha512(String input) {
        return DigestUtils.sha512Hex(input);
    }

    /**
     * Compute HMAC-SHA256 for a message with a secret key.
     *
     * @param message the message to hash
     * @param secret  the secret key
     * @return the HMAC-SHA256 hash in Base64 format
     * @throws RuntimeException if the algorithm is not available
     */
    public static String hmacSha256(String message, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    /**
     * Verify HMAC-SHA256 hash in constant time to prevent timing attacks.
     *
     * @param message      the original message
     * @param secret       the secret key
     * @param expectedHash the expected hash to verify against
     * @return true if the hash matches, false otherwise
     */
    public static boolean verifyHmacSha256(String message, String secret, String expectedHash) {
        String actualHash = hmacSha256(message, secret);
        return constantTimeEquals(actualHash, expectedHash);
    }

    /**
     * Compare two strings in constant time to prevent timing attacks.
     *
     * @param a the first string
     * @param b the second string
     * @return true if strings are equal, false otherwise
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(aBytes, bBytes);
    }

    /**
     * Generate a device fingerprint from user agent and IP address.
     *
     * @param userAgent the user agent string
     * @param ipAddress the IP address
     * @return a hashed device fingerprint
     */
    public static String generateDeviceFingerprint(String userAgent, String ipAddress) {
        String combined = (userAgent != null ? userAgent : "") + "|" + (ipAddress != null ? ipAddress : "");
        return sha256(combined);
    }

    /**
     * Mask a sensitive string, showing only first and last N characters.
     *
     * @param input     the input string
     * @param showFirst number of characters to show at the start
     * @param showLast  number of characters to show at the end
     * @return the masked string
     */
    public static String mask(String input, int showFirst, int showLast) {
        if (input == null || input.length() <= showFirst + showLast) {
            return input;
        }

        int maskLength = input.length() - showFirst - showLast;
        return input.substring(0, showFirst) +
                "*".repeat(maskLength) +
                input.substring(input.length() - showLast);
    }
}
