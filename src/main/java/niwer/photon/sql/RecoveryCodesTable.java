package niwer.photon.sql;

import java.util.List;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectRecoveryCode;
import niwer.photon.util.HashUtils;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.TotpManager;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.DeletionManager;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.tables.Table;

/**
 * Recovery code management with SQLite database.
 * 
 * @author Niwer
 */
public class RecoveryCodesTable extends Table {

    private static final int RECOVERY_CODE_COUNT = 8;

    public RecoveryCodesTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectRecoveryCode.class).execute();
    }

    @Override public String name() { return "RecoveryCodes"; }

    /**
     * Generates new recovery codes for a user, deletes any old ones, and saves the new hashes.
     * 
     * @param uuid The user UUID
     * @return A list of plaintext codes to show the user *once*
     */
    public static List<String> generateAndSaveCodes(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return List.of();
        deleteAllCodesForUser(uuid); // Clear any pre-existing recovery codes for this user (e.g., if regenerating)

        final List<String> PLAIN_CODES = TotpManager.generateRecoveryCodes(uuid, RECOVERY_CODE_COUNT);
        for (final String CODE : PLAIN_CODES) {
            final String HASHED_CODE = HashUtils.hashPassword(CODE);
            if (HASHED_CODE == null) continue;

            InsertionManager.insert(PhotonEngine.DATA_BASE, RecoveryCodesTable.class, "codeHash", "uuid")
                .row(HASHED_CODE, uuid)
                .execute();
        }

        Console.log("Generated %d new recovery codes for user %s", RECOVERY_CODE_COUNT, uuid).type(PhotonLogTypes.SQL).container(PhotonEngine.LOGGER).send();
        return PLAIN_CODES;
    }

    /**
     * Verifies if a given plaintext recovery code matches an active code for the user.
     * If valid, consumes (deletes or marks used) the code so it cannot be reused.
     * 
     * @param uuid The user UUID
     * @param plainCode The plaintext recovery code provided by the user
     * @return true if valid and consumed successfully, false otherwise
     */
    public static boolean verifyAndConsumeCode(String uuid, String plainCode) {
        if (uuid == null || uuid.trim().isEmpty() || plainCode == null || plainCode.trim().isEmpty()) return false;

        /* Fetch all unused codes for this user */
        final List<ObjectRecoveryCode> ACTIVE_CODES = SelectionManager.select(PhotonEngine.DATA_BASE, RecoveryCodesTable.class)
            .where(Expressions.isEqualTo("uuid", uuid))
            .executeList(ObjectRecoveryCode.class);
            
        if (ACTIVE_CODES == null) return false;
        for (final ObjectRecoveryCode RC : ACTIVE_CODES) {
            /* Check if the code hash is valid */
            final String CODE_HASH = RC.getCodeHash();
            if (CODE_HASH == null || CODE_HASH.trim().isEmpty()) continue;

            if (HashUtils.passwordMatches(CODE_HASH, plainCode.trim())) {
                DeletionManager.delete(PhotonEngine.DATA_BASE, RecoveryCodesTable.class).where(Expressions.isEqualTo("codeHash", CODE_HASH)).execute(); // Delete the code once used
                Console.log("Recovery code consumed for user %s", uuid).type(PhotonLogTypes.SQL).container(PhotonEngine.LOGGER).send();
                return true;
            }
        }

        return false;
    }

    /**
     * Deletes all recovery codes associated with a user (e.g., when 2FA is disabled).
     * 
     * @param uuid The user UUID
     */
    public static void deleteAllCodesForUser(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return;
        DeletionManager.delete(PhotonEngine.DATA_BASE, RecoveryCodesTable.class).where(Expressions.isEqualTo("uuid", uuid)).execute();
    }
}