package niwer.photon.objects;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import niwer.photon.sql.LicenseTable.LicenseStatus;
import niwer.queryon.SQLSerializable;
import niwer.queryon.tables.api.IColumnField;
import niwer.queryon.tables.api.IDefaultValue;

/**
 * @author Niwer 
 */
public class ObjectLicense extends SQLSerializable<ObjectLicense> implements IPayloadProvider {
	@IColumnField(name = "license_key", primaryKey = true, notNull = true)
	private String licenseKey;

	@IColumnField(name = "product_id", notNull = true)
	private String productId;

	@IColumnField(name = "name")
	private String name;

	@IColumnField(name = "customer_email")
	private String customerEmail;

	@IColumnField(name = "creator_uuid")
	private String creatorUuid;

	@IColumnField(name = "hwid")
	private String hwid;

	@IColumnField(name = "status", notNull = true, defaultValue = @IDefaultValue(value = "ISSUED"))
	private LicenseStatus status;

	@IColumnField(name = "created_at", defaultValue = @IDefaultValue(value = "CURRENT_TIMESTAMP"))
	private String createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

	@IColumnField(name = "activated_at")
	private Date activatedAt;

	@IColumnField(name = "expires_at")
	private Date expiresAt;

	public ObjectLicense() {}

	public boolean isExpired() { return this.expiresAt != null && this.expiresAt.before(new Date()); }

	public String licenseKey() { return this.licenseKey; }

	public String productId() { return this.productId; }

	public String name() { return this.name; }

	public String customerEmail() { return this.customerEmail; }

	public String creatorUuid() { return this.creatorUuid; }

	public String hwid() { return this.hwid; }

	public LicenseStatus status() { return this.status; }

	public Date createdAt() { return parseDate(this.createdAt); }

	public Date activatedAt() { return this.activatedAt; }

	public Date expiresAt() { return this.expiresAt; }

	@Deprecated
	private static Date parseDate(String value) {
		if (value == null || value.isBlank()) return null;
		if ("CURRENT_TIMESTAMP".equalsIgnoreCase(value)) return null;

		for (DateTimeFormatter formatter : new DateTimeFormatter[] {
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
		}) {
			try {
				return Date.from(LocalDateTime.parse(value, formatter).atZone(ZoneId.systemDefault()).toInstant());
			} catch (DateTimeParseException ignored) {
				// try the next format
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return "ObjectLicense{" +
			"licenseKey='" + licenseKey + '\'' +
			", status=" + status +
			", expiresAt=" + expiresAt +
			'}';
	}

	@Override
	public Map<String, Object> payload() { //TODO maybe use the common method
        final Map<String, Object> payload = new LinkedHashMap<>();
        final Long createdAt = this.createdAt() == null ? null : this.createdAt().getTime();
        final Long activatedAt = this.activatedAt() == null ? null : this.activatedAt().getTime();
        final Long expiresAt = this.expiresAt() == null ? null : this.expiresAt().getTime();

        payload.put("licenseKey", this.licenseKey());
        payload.put("license_key", this.licenseKey());
        payload.put("productId", this.productId());
        payload.put("product_id", this.productId());
        payload.put("name", this.name());
        payload.put("customerEmail", this.customerEmail());
        payload.put("customer_email", this.customerEmail());
        payload.put("creatorUuid", this.creatorUuid());
        payload.put("creator_uuid", this.creatorUuid());
        payload.put("hwid", this.hwid());
        payload.put("status", this.status());
        payload.put("createdAt", createdAt);
        payload.put("created_at", createdAt);
        payload.put("activatedAt", activatedAt);
        payload.put("activated_at", activatedAt);
        payload.put("expiresAt", expiresAt);
        payload.put("expires_at", expiresAt);
        return payload;
    }
}