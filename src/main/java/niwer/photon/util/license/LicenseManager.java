package niwer.photon.util.license;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;

import niwer.photon.objects.ObjectLicense;
import niwer.photon.sql.LicenseTable;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.OperatingSystem;
import niwer.photon.util.stripe.EntitlementManager;

/**
 * Utility class for validating license keys for the Photon Network Engine.
 * 
 * @author Niwer
 */
public final class LicenseManager {

	private static final int LICENSE_KEY_GROUPS = 4;
	private static final int LICENSE_KEY_GROUP_LENGTH = 5;
	private static final char[] LICENSE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray(); // Crockford/Base32 style alphabet (omits I, O to prevent confusion)

    private LicenseManager() {}

    public static String generateLicenseKey(String customerName, String customerEmail, String creatorUuid, String productId) {
        try {
            // Add entropy so keys can be regenerated if revoked
            byte[] nonce = new byte[8];
            new SecureRandom().nextBytes(nonce);

            // Cryptographic digest across inputs + nonce
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((customerName + customerEmail + creatorUuid + productId).getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(nonce);

            // Pick non-sequential pseudo-random characters using the hash bytes
            StringBuilder builder = new StringBuilder();
            int hashIndex = 0;

            for (int group = 0; group < LICENSE_KEY_GROUPS; group++) {
                if (group > 0) builder.append('-');
                for (int i = 0; i < LICENSE_KEY_GROUP_LENGTH; i++) {
                    int b = hash[hashIndex++ % hash.length] & 0xFF;
                    builder.append(LICENSE_ALPHABET[b % LICENSE_ALPHABET.length]);
                }
            }

            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate license key", e);
        }
    }

    /**
     * Issue a new license with the given information and store it in the database. The license key will be automatically generated and guaranteed to be unique.
     * 
     * @param productId The product id this license is valid for
     * @param customerName The name of the customer this license is issued to (optional)
     * @param customerEmail The email of the customer this license is issued to (optional)
     * @param orderId The associated order id for this license, if it was purchased through the official store (optional, but must be unique if provided)
     * @param expiresAtMillis The expiration date of the license in milliseconds since epoch, or null/0 for no expiration
     * @return the issued ObjectLicense containing all the license information, including the generated license key
     */
	public static ObjectLicense issueLicense(final String productId, final String customerName, final String customerEmail, final String creatorUuid, final Long expiresAtMillis) {
		final Date EXPIRES_AT = expiresAtMillis == null || expiresAtMillis <= 0L ? null : new Date(expiresAtMillis);
		final String LICENSE_KEY = generateLicenseKey(customerName, customerEmail, creatorUuid, productId);
		return LicenseTable.issueLicense(LICENSE_KEY, productId, customerName, customerEmail, creatorUuid, EXPIRES_AT);
	}

	/**
	 * Normalize a license key by trimming whitespace and converting to uppercase. This ensures consistent formatting for storage and comparison.
	 * 
	 * @param licenseKey The license key to normalize
	 * @return The normalized license key, or null if the input is null
	 */
	public static String normalizeKey(String licenseKey) { return licenseKey == null ? null : licenseKey.trim().toUpperCase(); }

	/**
     * Check if the given license key is valid for the expected product id, using the provided public key for signature verification.
     * 
     * @param licenseKey
     * @param publicKeyValue
     * @param productId
     * @return a LicenseValidationResult containing the validation result and claims if valid, or failure reason if invalid
     */
	public static LicenseValidationResult validateLicense(final String licenseKey, final String productId, final String hardwareId) {
		final String NORMALIZED_KEY = normalizeKey(licenseKey);
		final ObjectLicense LICENSE = LicenseTable.getByKey(NORMALIZED_KEY);
		if (LICENSE == null) return LicenseValidationResult.missing(); // License key not found in database

		if (LICENSE.productId() == null || !LICENSE.productId().equalsIgnoreCase(productId)) return LicenseValidationResult.invalid(LicenseFailureReason.PRODUCT_MISMATCH, "License is not valid for product '" + productId + "'");
		if (LicenseTable.LicenseStatus.REVOKED == LICENSE.status()) return LicenseValidationResult.invalid(LicenseFailureReason.UNEXPECTED_ERROR, "License key has been revoked");
		if (LICENSE.isExpired()) return LicenseValidationResult.invalid(LicenseFailureReason.EXPIRED, "License key has expired");

		/* Ensure creator's subscription is active; license becomes usable again if subscription restarts */
		final boolean HAS_ACCESS = PlayerAccountTable.isAdmin(LICENSE.creatorUuid()) || EntitlementManager.hasAccess(LICENSE.creatorUuid(), LICENSE.productId());
		if (!HAS_ACCESS) return LicenseValidationResult.invalid(LicenseFailureReason.SUBSCRIPTION_INACTIVE, "License creator has no active entitlement for this product");

		final String CURREND_HWID = (hardwareId != null && !hardwareId.isBlank()) ? hardwareId : OperatingSystem.getHWID();
		if (LICENSE.hwid() != null && !LICENSE.hwid().isBlank()) { // If the key is already bound to a hardware id, ensure it matches the current machine's hwid
			if (!LICENSE.hwid().equalsIgnoreCase(CURREND_HWID)) return LicenseValidationResult.invalid(LicenseFailureReason.HARDWARE_MISMATCH, "License key is bound to another machine");
		} else if (CURREND_HWID != null && !CURREND_HWID.isBlank()) LicenseTable.activate(NORMALIZED_KEY, CURREND_HWID); // If the key is not bound to a hardware id, bind it to the current machine's hwid

		return LicenseValidationResult.valid(new LicenseClaims(
			NORMALIZED_KEY,
			LICENSE.productId(),
			LICENSE.name(),
			LICENSE.customerEmail(),
			CURREND_HWID,
			LICENSE.createdAt() == null ? null : LICENSE.createdAt().getTime(),
			LICENSE.expiresAt() == null ? null : LICENSE.expiresAt().getTime()
		));
	}
}