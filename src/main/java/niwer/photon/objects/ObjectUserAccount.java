package niwer.photon.objects;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import niwer.photon.util.TranslationManager.Language;
import niwer.queryon.SQLSerializable;
import niwer.queryon.tables.api.IColumnField;
import niwer.queryon.tables.api.IDefaultValue;

/**
 * @author Niwer 
 */
public class ObjectUserAccount extends SQLSerializable<ObjectUserAccount> implements IPayloadProvider {

    @IColumnField(name = "username", notNull = true)
    private String username;

    @IColumnField(name = "email", notNull = true)
    private String email;

    @IColumnField(name = "password", notNull = true)
    private transient String password;

    @IColumnField(name = "uuid", primaryKey = true, notNull = true)
    private String uuid;

    @IColumnField(name = "discordID", charLimit = 1024)
    private String discordID;

    @IColumnField(name = "discordAuthCode", notNull = true, charLimit = 255)
    private String discordAuthCode = generateAuthCode();

    @IColumnField(name = "administrator", defaultValue = @IDefaultValue(value = "false"))
    private boolean administrator;

    @IColumnField(name = "language", defaultValue = @IDefaultValue(value = "ENGLISH"))
    private String language = Language.ENGLISH.name();

    @IColumnField(name = "totpEnabled", defaultValue = @IDefaultValue(value = "false"))
    private boolean totpEnabled = false; // Toptal One-Time Password (TOTP) for 2FA

    @IColumnField(name = "totpSecret")
    private String totpSecret; // Toptal One-Time Password (TOTP) for 2FA

    public static String generateAuthCode() { return new BigInteger(40, new SecureRandom()).toString(32); }

    public boolean hasDiscordLinked() { return this.discordID != null && !this.discordID.isEmpty(); }

    @Override
    public String toString() {
        return String.format("User Account{username='%s', email='%s', uuid='%s', discordID='%s', administrator=%s}", this.username, this.email, this.uuid, this.discordID, isAdministrator());
    }

    public String getUsername() { return this.username; }

    public String getEmail() { return this.email; }

    public String password() { return this.password; }

    public String getUuid() { return this.uuid; }

    public String getDiscordID() { return this.discordID; }

    public String getDiscordAuthCode() { return this.discordAuthCode; }

    public boolean isAdministrator() { return this.administrator; }
    public boolean getAdministrator() { return this.administrator; }

    public Language getLanguage() { return Language.fromString(this.language); }

    public boolean isTotpEnabled() { return this.totpEnabled; }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }

    public String getTotpSecret() { return this.totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    @Override 
    public Map<String, Object> payload() {
        final Map<String, Object> response = new HashMap<>();
        response.put("username", this.username);
        response.put("email", this.email);
        response.put("uuid", this.uuid);
        response.put("discordID", this.discordID);
        response.put("discordAuthCode", this.discordAuthCode);
        response.put("administrator", isAdministrator());
        response.put("language", getLanguage().name());
        response.put("totpEnabled", this.totpEnabled);
        return response;
    }
}