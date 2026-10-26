package com.campusclub.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Secure Password Utility implementing PBKDF2 with HMAC-SHA256 and salted hashing.
 * Provides backwards-compatibility for existing SHA-256 demo hashes while securing newly stored passwords.
 */
public final class PasswordUtil {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Utility class
    }

    /**
     * Hashes a raw password string using PBKDF2WithHmacSHA256 and a random cryptographic salt.
     * Output format: "PBKDF2$iterations$saltHex$hashHex"
     *
     * @param password raw plain text password
     * @return formatted salted hash string
     */
    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return String.format("PBKDF2$%d$%s$%s", ITERATIONS, bytesToHex(salt), bytesToHex(hash));
    }

    /**
     * Verifies a plain text password against a stored hash string.
     * Supports both PBKDF2 salted hashes and legacy demo SHA-256 hashes.
     *
     * @param plainPassword plain text password to check
     * @param storedHash    the stored hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.trim().isEmpty()) {
            return false;
        }

        // 1. Check if storedHash is in PBKDF2 format: PBKDF2$<iterations>$<salt>$<hash>
        if (storedHash.startsWith("PBKDF2$")) {
            String[] parts = storedHash.split("\\$");
            if (parts.length == 4) {
                try {
                    int iterations = Integer.parseInt(parts[1]);
                    byte[] salt = hexToBytes(parts[2]);
                    byte[] expectedHash = hexToBytes(parts[3]);

                    byte[] actualHash = pbkdf2(plainPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
                    return MessageDigest.isEqual(expectedHash, actualHash);
                } catch (Exception e) {
                    return false;
                }
            }
        }

        // 2. Legacy / Standard SHA-256 Verification (supports initial seed data)
        try {
            MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
            byte[] calculatedShaBytes = shaDigest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            byte[] expectedShaBytes = hexToBytes(storedHash);

            return MessageDigest.isEqual(expectedShaBytes, calculatedShaBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Computes raw SHA-256 hash in hex format.
     */
    public static String sha256Hex(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error calculating PBKDF2 hash", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
