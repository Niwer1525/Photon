package niwer.photon.sql;

import java.util.UUID;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.util.HashUtils;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.TranslationManager.Language;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.DeletionManager;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.UpdateManager;
import niwer.queryon.tables.Table;

/**
 * Player account management with SQLite database.
 * Handles account creation, retrieval, validation and deletion.
 * 
 * @author Niwer
 */
public class PlayerAccountTable extends Table {

    public enum AccountDeletionStatus {
        PENDING_DELETION,
        DELETED,
        NONE; // Account is not marked for deletion
	}

    public PlayerAccountTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectUserAccount.class).execute();
    }

    @Override public String name() { return "Account"; }

    /**
     * Create a new player account.
     * Validates input and checks for existing email/username before creation.
     * 
     * @param username The desired username (must be unique)
     * @param email The email address (must be unique)
     * @param password The password (already hashed)
     * @return ObjectPlayerAccount if successful, null otherwise
     */
    public static ObjectUserAccount createAccount(String username, String email, String password) {
        if (username == null || email == null || password == null) {
            Console.log("Cannot create account with null parameters").type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        
        if (username.trim().isEmpty() || email.trim().isEmpty() || password.isEmpty()) {
            Console.log("Cannot create account with empty parameters").type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        
        if (emailExists(email)) {
            Console.log("Email already exists: " + email).type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        
        if (usernameExists(username)) {
            Console.log("Username already exists: " + username).type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return null;
        }

        final String UniqueUserID = UUID.randomUUID().toString();
        final String hashedPassword = HashUtils.hashPassword(password);
        if (hashedPassword == null) {
            Console.log("Cannot hash password for account creation").type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        
        InsertionManager.insert(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "uuid", "username", "email", "password", "discordAuthCode")
            .row(UniqueUserID, username.trim(), email.trim().toLowerCase(), hashedPassword, ObjectUserAccount.generateAuthCode())
        .execute();

        return getAccountByUUID(UniqueUserID);
    }

    /**
     * Flags an account as pending deletion with the current epoch timestamp.
     * 
     * @param uuid The unique identifier of the user
     * @return true if updated successfully
     */
    public static boolean markPendingDeletion(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot flag deletion for null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return false;
        }

        try {
            UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
                .set("deletedAt", System.currentTimeMillis())
                .set("deletionStatus", AccountDeletionStatus.PENDING_DELETION)
                .where(Expressions.isEqualTo("uuid", uuid))
                .execute();
            return true;
        } catch (Exception e) {
            Console.log("Failed to mark account for deletion: " + e.getMessage()).error().container(PhotonEngine.LOGGER).send();
            return false;
        }
    }

    /**
     * Checks if the account is currently marked for deletion.
     * 
     * @param uuid The account UUID
     * @return true if deletedAt is present and non-zero
     */
    public static boolean isPendingDeletion(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return false;

        try {
            final Long deletedAt = SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "deletedAt")
                .where(Expressions.isEqualTo("uuid", uuid))
                .executePrimitive(Long.class);
            return deletedAt != null && deletedAt > 0;
        } catch(Exception e) {
            Console.log("Failed to check deletion status for account " + uuid + ": " + e.getMessage()).error().container(PhotonEngine.LOGGER).send();
        }
        return false;
    }

    /**
     * Check if a user is an administrator based on their UUID.
     * 
     * @param uuid The unique identifier of the user
     * @return true if the user is an administrator, false otherwise
     */
    public static boolean isAdmin(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return false;
        final Boolean IS_ADMIN = SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "administrator")
            .where(Expressions.isEqualTo("uuid", uuid))
            .executePrimitive(Boolean.class);
        return IS_ADMIN != null && IS_ADMIN;
    }

    public static void setAdmin(String uuid, boolean isAdmin) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot update admin status for null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("administrator", isAdmin)
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    public static void setDiscordID(String uuid, String discordID) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot update Discord ID for null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("discordID", discordID)
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    /**
     * Check if a Discord user is already registered.
     * 
     * @param discordID The Discord ID to check
     * @return true if the user is already registered, false otherwise
     */
    public static boolean discordExists(long discordId) {
        final String DISCORD_ID_STR = String.valueOf(discordId);
        final Integer COUNT = SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "COUNT(*) as count")
            .where(Expressions.isEqualTo("LOWER(discordID)", DISCORD_ID_STR))
            .executePrimitive(Integer.class);
        return COUNT != null && COUNT > 0;
    }

    /**
     * Update language preferences for a user by UUID.
     * 
     * @param uuid The unique identifier of the user
     * @param language The new language preference to set
     */
    public static void setLanguageFromUUID(String uuid, Language language) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot update language for null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("language", language.name())
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    /**
     * Update language preferences for a user.
     * 
     * @param discordUserID The discord id of the user
     * @param newUserLanguage List of Languages to set
     */
    public static void setLanguageFromDiscordID(String discordUserID, Language newUserLanguage) {
        if (discordUserID == null || discordUserID.trim().isEmpty()) {
            Console.log("Cannot update language for null/empty Discord ID").error().container(PhotonEngine.LOGGER).send();
            return;
        }

        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("language", newUserLanguage.name())
            .where(Expressions.isEqualTo("discord_user_id", discordUserID))
            .execute();
    }

    /**
     * Retrieve language preferences for a user.
     * 
     * @param discordIdOrAccountUUID The discord id of the user or the account UUID
     * @return List of Languages or null if user has no preferences
     */
    public static Language getLanguage(String discordIdOrAccountUUID) {
        final var QUERY = SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "language").where(
            Expressions.or(
                Expressions.isEqualTo("discord_user_id", discordIdOrAccountUUID),
                Expressions.isEqualTo("uuid", discordIdOrAccountUUID)
            ));
        
        if(!QUERY.executeHasResult()) return null; // No preferences found for the user

        final String USERR_LANG = QUERY.executePrimitive(String.class);
        return Language.fromString(USERR_LANG);
    }

    public static boolean existByUUID(String uuid) { return getAccountByUUID(uuid) != null; }

    /**
     * Retrieve account by UUID.
     * 
     * @param uuid The unique identifier
     * @return ObjectPlayerAccount if found, null otherwise
     */
    public static ObjectUserAccount getAccountByUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot get account with null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        return SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .where(Expressions.isEqualTo("uuid", uuid))
            .executeSerializable(ObjectUserAccount.class);
    }

    /**
     * Retrieve account by email address.
     * Email comparison is case-insensitive.
     * 
     * @param email The email address
     * @return ObjectPlayerAccount if found, null otherwise
     */
    public static ObjectUserAccount getAccountByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            Console.log("Cannot get account with null/empty email").error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        final String NORMALIZED_EMAIL = email.trim().toLowerCase();
        return SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .where(Expressions.isEqualTo("email", NORMALIZED_EMAIL))
            .executeSerializable(ObjectUserAccount.class);
    }

    /**
     * Retrieve account by username.
     * Username comparison is case-insensitive.
     * 
     * @param username The username
     * @return ObjectPlayerAccount if found, null otherwise
     */
    public static ObjectUserAccount getAccountByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            Console.log("Cannot get account with null/empty username").error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        final String NORMALIZED_USERNAME = username.trim().toLowerCase();
        return SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .where(Expressions.isEqualTo("username", NORMALIZED_USERNAME))
            .executeSerializable(ObjectUserAccount.class);
    }

    /**
     * Retrieve account by Discord ID.
     * 
     * @param discordID The Discord user ID
     * @return ObjectPlayerAccount if found, null otherwise
     */
    public static ObjectUserAccount getAccountByDiscordID(String discordID) {
        if (discordID == null || discordID.trim().isEmpty()) {
            Console.log("Cannot get account with null/empty Discord ID").error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        return SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .where(Expressions.isEqualTo("discordID", discordID))
            .executeSerializable(ObjectUserAccount.class);
    }

    /**
     * Check if an email is already registered.
     * Email comparison is case-insensitive.
     * 
     * @param email The email to check
     * @return true if email exists, false otherwise
     */
    public static boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            Console.log("Cannot check existence of null/empty email").error().container(PhotonEngine.LOGGER).send();
            return false;
        }
        final String NORMALIZED_EMAIL = email.trim().toLowerCase();
        final Integer count = SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "COUNT(*) as count")
            .where(Expressions.isEqualTo("email", NORMALIZED_EMAIL))
            .executePrimitive(Integer.class);
        return count != null && count > 0;
    }

    /**
     * Check if a username is already taken.
     * Username comparison is case-insensitive.
     * 
     * @param username The username to check
     * @return true if username exists, false otherwise
     */
    public static boolean usernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            Console.log("Cannot check existence of null/empty username").error().container(PhotonEngine.LOGGER).send();
            return false;
        }
        final String NORMALIZED_USERNAME = username.trim().toLowerCase();
        final Integer count = SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "COUNT(*) as count")
            .where(Expressions.isEqualTo("username", NORMALIZED_USERNAME))
            .executePrimitive(Integer.class);
        return count != null && count > 0;
    }

    /**
     * Validate authentication code for a given UUID.
     * 
     * @param givenUUID The player UUID
     * @param givenAuthCode The authentication code to validate
     * @return true if valid, false otherwise
     */
    public static boolean isAuthCodeValid(String givenUUID, String givenAuthCode) {
        if(givenUUID == null || givenUUID.trim().isEmpty() || givenAuthCode == null || givenAuthCode.trim().isEmpty()) {
            Console.log("Cannot validate auth code with null/empty parameters").error().container(PhotonEngine.LOGGER).send();
            return false;
        }
        return SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class, "COUNT(*) as count")
            .where(Expressions.and(
                Expressions.isEqualTo("uuid", givenUUID),
                Expressions.isEqualTo("discordAuthCode", givenAuthCode)
            ))
            .executeHasResult();
    }

    public static void setUsername(String uuid, String username) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot update username with null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        if (username == null || username.isBlank()) {
            Console.log("Cannot update username with null/empty value").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("username", username.trim())
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    public static void setEmail(String uuid, String email) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot update email with null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        if (email == null || email.isBlank()) {
            Console.log("Cannot update email with null/empty value").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("email", email.trim().toLowerCase())
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    public static void setPassword(String uuid, String password) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot update password with null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        if (password == null || password.isBlank()) {
            Console.log("Cannot update password with null/empty value").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        final String hashedPassword = HashUtils.hashPassword(password);
        if (hashedPassword == null) {
            Console.log("Cannot hash password for update").type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return;
        }

        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("password", hashedPassword)
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    public static void setTotpSecret(String uuid, String encryptedSecret) {
        if (uuid == null || uuid.trim().isEmpty()) return;
        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("totpSecret", encryptedSecret)
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    public static void setTotpEnabled(String uuid, boolean enabled) {
        if (uuid == null || uuid.trim().isEmpty()) return;
        UpdateManager.update(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .set("totpEnabled", enabled)
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }

    /**
     * Delete an account by UUID.
     * 
     * @param uuid The account UUID to delete
     */
    public static void deleteAccount(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            Console.log("Cannot delete account with null/empty UUID").error().container(PhotonEngine.LOGGER).send();
            return;
        }
        DeletionManager.delete(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .where(Expressions.isEqualTo("uuid", uuid))
            .execute();
    }
}