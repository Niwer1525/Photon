package niwer.photon.util;

import java.util.List;
import java.util.concurrent.TimeUnit;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.sql.PlayerAccountTable.AccountDeletionStatus;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.SelectionManager;

/**
 * This class is responsible for purging user accounts that have been marked for deletion and have exceeded the retention period defined in the configuration.
 * It runs as a background thread and periodically checks for expired accounts to delete them from the database.
 */
public class AccountRetentionPurger implements Runnable {

    public static void createPurgerScheduler() {
        Thread retentionThread = new Thread(new AccountRetentionPurger(), "AccountRetentionPurger");
        retentionThread.setDaemon(true);
        retentionThread.start();
    }

    private AccountRetentionPurger() {}

    @Override
    public void run() {
        long cutoffTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Directories.config().account_retention_days);

        // Fetch accounts where deletedAt <= (NOW - RETENTION_DAYS)
        List<ObjectUserAccount> expiredAccounts = SelectionManager.select(PhotonEngine.DATA_BASE, PlayerAccountTable.class)
            .where(
                Expressions.and(
                    Expressions.isEqualTo("deletionStatus", AccountDeletionStatus.PENDING_DELETION.name()),
                    Expressions.and(
                        Expressions.isNotNull("deletedAt"),
                        Expressions.isLessThanOrEqualTo("deletedAt", cutoffTimestamp)
                    )
                )
            )
            .executeList(ObjectUserAccount.class);

        if (expiredAccounts == null || expiredAccounts.isEmpty()) return;

        for (ObjectUserAccount acc : expiredAccounts) {
            try {
                String uuid = acc.getUuid();

                // Anonymize PII for auditing/legal requirements before deleting
                String anonymizedEmail = "deleted_user_" + uuid + "@anonymized.local";
                String anonymizedUsername = "deleted_user_" + uuid.substring(0, 8);

                {
                    /* Annonymize the user's email and username */
                    PlayerAccountTable.setEmail(uuid, anonymizedEmail);
                    PlayerAccountTable.setUsername(uuid, anonymizedUsername);
                }

                // Hard delete the player account record
                PlayerAccountTable.deleteAccount(uuid);

                Console.log("Purged expired account: " + uuid).send();
            } catch (Exception e) {
                Console.log("Failed to purge account " + acc.getUuid() + ": " + e.getMessage()).error().send();
            }
        }
    }
}