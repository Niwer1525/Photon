package niwer.photon.sql;

import java.util.Date;
import java.util.List;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectLicense;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.license.LicenseManager;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonException;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.UpdateManager;
import niwer.queryon.tables.Table;

public class LicenseTable extends Table {
	public enum LicenseStatus {
		ISSUED,
		ACTIVE,
		REVOKED;
	}

	public LicenseTable(DataBase db) {
		super(db);
		this.addColumnsFromClass(ObjectLicense.class).execute();
	}

	@Override public String name() { return "License"; }

	public static ObjectLicense issueLicense(String licenseKey, String productId, String name, String customerEmail, String creatorUuid, Date expiresAt) {
		InsertionManager.insert(PhotonEngine.DATA_BASE, LicenseTable.class, "license_key", "product_id", "name", "customer_email", "creator_uuid", "status", "created_at", "expires_at")
			.row(LicenseManager.normalizeKey(licenseKey), productId, name, customerEmail, creatorUuid, LicenseStatus.ISSUED, new Date(), expiresAt)
			.execute();

		return getByKey(licenseKey);
	}

	/**
	 * Get a license by its license key. The license key will be normalized (trimmed and uppercased) before searching.
	 * 
	 * @param licenseKey The license key to search for
	 * @return The ObjectLicense if found, or null if not found or if the license key is null/blank
	 */
	public static ObjectLicense getByKey(String licenseKey) {
		if (licenseKey == null || licenseKey.isBlank()) return null;
		return SelectionManager.select(PhotonEngine.DATA_BASE, LicenseTable.class)
			.where(Expressions.isEqualTo("license_key", LicenseManager.normalizeKey(licenseKey)))
			.limit(1)
			.executeSerializable(ObjectLicense.class);
	}

	/**
	 * Get all licenses issued by a specific creator UUID. This can be used to retrieve all licenses associated with a particular user or account.
	 * 
	 * @param creatorUuid The UUID of the creator whose licenses are to be retrieved
	 * @return A list of ObjectLicense instances issued by the specified creator, or an empty list if none are found or if the creator UUID is null/blank
	 */
	public static List<ObjectLicense> getByCreatorUuid(String creatorUuid) {
		if (creatorUuid == null || creatorUuid.isBlank()) return List.of();
		return SelectionManager.select(PhotonEngine.DATA_BASE, LicenseTable.class)
			.where(Expressions.isEqualTo("creator_uuid", creatorUuid))
			.executeList(ObjectLicense.class);
	}

	public static boolean activate(String licenseKey, String hwid) {
		if (licenseKey == null || licenseKey.isBlank() || hwid == null || hwid.isBlank()) return false;
		try {
            UpdateManager.update(PhotonEngine.DATA_BASE, LicenseTable.class)
                .set("hwid", hwid)
                .set("status", LicenseStatus.ACTIVE)
                .set("activated_at", new Date())
                .where(Expressions.isEqualTo("license_key", LicenseManager.normalizeKey(licenseKey)))
                .execute();
            return true;
        } catch(QueryonException e) {
            Console.log("Failed to activate license key '" + licenseKey + "': " + e.getMessage()).type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return false;
        }
	}

	public static boolean revoke(String licenseKey) {
		if (licenseKey == null || licenseKey.isBlank()) return false;
        try {
            UpdateManager.update(PhotonEngine.DATA_BASE, LicenseTable.class)
                .set("status", LicenseStatus.REVOKED)
                .where(Expressions.isEqualTo("license_key", LicenseManager.normalizeKey(licenseKey)))
                .execute();
            return true;
        } catch(QueryonException e) {
            Console.log("Failed to revoke license key '" + licenseKey + "': " + e.getMessage()).type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return false;
        }
	}
}