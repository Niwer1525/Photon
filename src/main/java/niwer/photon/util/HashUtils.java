package niwer.photon.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * @author Niwer
 */
public final class HashUtils {

    private static final int ITERATIONS = 3;
    private static final int MEMORY_KIB = 65536;
    private static final int PARALLELISM = 1;
    private static final String ARGON2_PREFIX = "$argon2";

    private HashUtils() {}

    /**
     * Generate a random salt of the specified length using a secure random number generator.
     * 
     * @param length The length of the salt in bytes
     * @return A byte array containing the generated salt
     */
    public static byte[] generateRandomSalt(int length) {
        byte[] salt = new byte[length];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * Generate a random salt of the specified length and encode it in Base64.
     * 
     * @param length The length of the salt in bytes
     * @return A Base64-encoded string containing the generated salt
     */
    public static String generateRandomSaltBase64(int length) {
        byte[] salt = generateRandomSalt(length);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash a password using Argon2id algorithm.
     * 
     * @param password The password to hash
     * @return The hashed password
     */
    public static String hashPassword(String password) {
        if (password == null || password.isBlank()) return null;

        final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        final char[] passwordChars = password.toCharArray();
        try {
            return argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, passwordChars);
        } finally {
            Arrays.fill(passwordChars, '\0');
            argon2.wipeArray(passwordChars);
        }
    }

    /**
     * Check if a raw password matches the stored password hash.
     * 
     * @param storedPassword The stored password hash
     * @param rawPassword The raw password to check
     * @return true if the passwords match, false otherwise
     */
    public static boolean passwordMatches(String storedPassword, String rawPassword) {
        if (storedPassword == null || storedPassword.isBlank() || rawPassword == null || rawPassword.isBlank()) return false;

        if (isArgon2Hash(storedPassword)) {
            final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
            final char[] passwordChars = rawPassword.toCharArray();
            try {
                return argon2.verify(storedPassword, passwordChars);
            } finally {
                Arrays.fill(passwordChars, '\0');
                argon2.wipeArray(passwordChars);
            }
        }

        return storedPassword.equals(rawPassword);
    }

    /**
     *  Check if a given string is an Argon2 hash by looking for the Argon2 prefix.
     * 
     * @param value The string to check
     * @return true if the string is an Argon2 hash, false otherwise
     */
    public static boolean isArgon2Hash(String value) {
        return value != null && value.startsWith(ARGON2_PREFIX);
    }

    /**
     * Calculate the SHA-256 hash of a file.
     * 
     * @param file The file to hash
     * @return The SHA-256 hash as a hexadecimal string
     * @throws IOException if an I/O error occurs reading the file
     */
    public static String hashFile(Path file) throws IOException {
        if(file == null || !Files.exists(file) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Invalid file path provided for hashing.");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream IS = Files.newInputStream(file); DigestInputStream DIS = new DigestInputStream(IS, digest)) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                while (DIS.read(buffer) != -1) {} // Reading the stream automatically updates the MessageDigest
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Hash a string using SHA-256 algorithm.
     * 
     * @param input The string to hash
     * @return The SHA-256 hash as a hexadecimal string
     */
    public static String hash(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
