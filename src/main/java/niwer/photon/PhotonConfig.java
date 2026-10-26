package niwer.photon;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;

import niwer.photon.objects.ObjectProduct;
import niwer.photon.objects.ObjectPurchase;
import niwer.photon.util.HashUtils;

public class PhotonConfig {

    protected static final PhotonConfig DEFAULT = new PhotonConfig();
		
    @SerializedName("isDevEnv") private boolean isDevEnv = false;

    /* Bot infos */
    @SerializedName("bot_activity") public String bot_activity = "/";
    @SerializedName("discord_bot_token") public String discord_bot_token = "";
    @SerializedName("discord_bot_id") public String discord_bot_id = "";
    @SerializedName("official_discord_server_id") public String official_discord_server_id = "";
    @SerializedName("network_console_channel_id") public String network_console_channel_id = "";
    
    /* Web Server */
    @SerializedName("webserver_host") public String webserver_host = null; // null = all interfaces,
    @SerializedName("webserver_port") public int webserver_port = 7070;
    @SerializedName("webserver_api_key") public String webserver_api_key = UUID.randomUUID().toString(); // We're generating a random signature on the first launch, and then saving it in the config file. This signature is used to secure the endpoints, and should be kept secret.

    /* Database backups */
    @SerializedName("database_backup_enabled") public boolean database_backup_enabled = Boolean.TRUE;
    @SerializedName("database_backup_on_startup") public boolean database_backup_on_startup = Boolean.TRUE;
    @SerializedName("database_backup_interval_minutes") public long database_backup_interval_minutes = 1440L; // 1440 minutes (24 hours) default interval between automatic database backups
    @SerializedName("database_backup_file_prefix") public String database_backup_file_prefix = "db_backup";
    @SerializedName("database_backup_retention_days") public long database_backup_retention_days = 15L; // Keep backups for 15 days by default

    /* Licensing */
    @SerializedName("products") public List<ObjectProduct> products;

    /* Github */
    @SerializedName("github_pat") public String github_pat = ""; // Personal Access Token for GitHub API authentication
    @SerializedName("github_username") public String github_username = ""; // GitHub username for API requests

    /* Mail */
    @SerializedName("mail_sender_email") public String mail_sender_email = "sender@example.org";
    @SerializedName("mail_smtp_host") public String mail_smtp_host = "";
    @SerializedName("mail_smtp_port") public int mail_smtp_port = 587;
    @SerializedName("mail_username") public String mail_username = "";
    @SerializedName("mail_password") public String mail_password = "";

    /* Wakatime */
    @SerializedName("wakatime_api_key") public String wakatime_api_key = "";

    /* Stripe */
    @SerializedName("stripe_api_key") public String stripe_api_key = "";
    @SerializedName("stripe_webhook_secret") public String stripe_webhook_signature = "";

    /* Accounts */
    @SerializedName("account_retention_days") public int account_retention_days = 14; // Default retention period for deleted accounts (in days)
    @SerializedName("totp_issuer") private String totp_issuer = "Photon";
    @SerializedName("totp_secret") private String totp_secret = HashUtils.generateRandomSaltBase64(32); // Default TOTP secret (hashed for security)

    public boolean isEmpty() {
        return this.equals(PhotonConfig.DEFAULT);
    }

    public String dbBackupFilePrefix() {
        return database_backup_file_prefix != null && !database_backup_file_prefix.isBlank() ? database_backup_file_prefix : "db_backup";
    }

    public boolean hasEmailConfig() {
        return mail_sender_email != null && !mail_sender_email.isBlank() &&
            mail_smtp_host != null && !mail_smtp_host.isBlank() &&
            mail_username != null && !mail_username.isBlank() &&
            mail_password != null && !mail_password.isBlank();
    }

    public boolean hasBotToken() {
        return discord_bot_token != null && !discord_bot_token.isBlank();
    }

    public List<ObjectProduct> getProducts() {
        return products == null ? List.of() : products;
    }

    /**
     * Check if a product is a one-time purchase (not a subscription) based on its product ID.
     * 
     * @param purchase The ObjectPurchase to check.
     * @return True if the product is a one-time purchase, false if it is a subscription or not found.
     */
    public boolean isOneTimeProduct(ObjectPurchase purchase) {
        if(purchase == null) return false;
        if(purchase.productId() == null || purchase.productId().isBlank()) return false;
        return getProducts().stream().anyMatch(product -> !product.isSubscription() && purchase.productId().equals(product.id()));
    }

    /**
     * Resolve a product by its Stripe price ID.
     * 
     * @param stripePriceId The Stripe price ID to look for.
     * @return The product corresponding to the Stripe price ID, or null if it cannot be found.
     */
    public ObjectProduct productByStripePriceId(String stripePriceId) {
        if (stripePriceId == null || stripePriceId.isBlank()) return null;
        return getProducts().stream()
            .filter(product -> product.stripePriceIds() != null && product.stripePriceIds().contains(stripePriceId))
            .findFirst()
            .orElse(null);
    }

    public boolean isDevEnvironment() {
        return isDevEnv;
    }

    public String getTotpIssuer() {
        return totp_issuer != null && !totp_issuer.isBlank() ? totp_issuer : "Photon";
    }

    public byte[] getTotpSecretBytes() {
        return totp_secret != null && !totp_secret.isBlank() ? Base64.getDecoder().decode(totp_secret) : new byte[0];
    }
}
