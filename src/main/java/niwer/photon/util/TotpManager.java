package niwer.photon.util;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import niwer.photon.Directories;

/**
 * This class manages TOTP (Time-based One-Time Password) operations, including generating secrets, creating QR codes for authenticator apps, verifying codes, and encrypting/decrypting secrets.
 *
 * @author Niwer
 */
public class TotpManager {
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH = 12;

    // In production, load this from System.getenv("TOTP_ENCRYPTION_KEY") (must be 32 bytes)
    private static final byte[] AES_KEY_BYTES = "12345678901234567890123456789012".getBytes();
    private static final SecretKey SECRET_KEY = new SecretKeySpec(AES_KEY_BYTES, "AES");

    private static final DefaultCodeVerifier VERIFIER = new DefaultCodeVerifier(
        new DefaultCodeGenerator(HashingAlgorithm.SHA1),
        new SystemTimeProvider()
    );

    public static String generateRawSecret() {
        return new DefaultSecretGenerator(32).generate();
    }

    /**
     * Generate a QR code data URI for the given email and raw secret, which can be scanned by authenticator apps to set up TOTP.
     * 
     * @param email The email address to associate with the TOTP account (used as the label in the QR code)
     * @param rawSecret The raw secret key to encode in the QR code
     * @return A data URI containing the QR code image in PNG format
     * @throws Exception If an error occurs during QR code generation
     */
    public static String generateQrDataUri(String email, String rawSecret) throws Exception {
        QrData data = new QrData.Builder()
            .label(email)
            .secret(rawSecret)
            .issuer(Directories.getConfig().getTotpIssuer())
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

        ZxingPngQrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData = generator.generate(data);
        return Utils.getDataUriForImage(imageData, generator.getImageMimeType());
    }

    /**
     * Verify a TOTP code against an encrypted secret. The secret is decrypted before verification.
     * 
     * @param encryptedSecret The encrypted secret key to verify against
     * @param code The TOTP code to verify
     * @return true if the code is valid, false otherwise
     */
    public static boolean verifyCode(String encryptedSecret, String code) {
        if (encryptedSecret == null || code == null) return false;
        try {
            String plainSecret = decrypt(encryptedSecret);
            return VERIFIER.isValidCode(plainSecret, code.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Encrypt a plaintext string using AES-GCM with a random IV. The IV is prepended to the ciphertext for later decryption.
     * 
     * @param plainText The plaintext string to encrypt
     * @return The encrypted string
     * @throws Exception If an error occurs during encryption
     */
    public static String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes());

        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt an encrypted string that was produced by the encrypt method. The IV is extracted from the beginning of the input.
     * 
     * @param base64Combined The base64-encoded combined IV and ciphertext
     * @return The decrypted string
     * @throws Exception If an error occurs during decryption
     */
    public static String decrypt(String base64Combined) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Combined);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        int cipherTextLen = combined.length - IV_LENGTH;
        byte[] cipherText = new byte[cipherTextLen];
        System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherTextLen);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        return new String(cipher.doFinal(cipherText));
    }
}