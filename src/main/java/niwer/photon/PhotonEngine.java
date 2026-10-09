package niwer.photon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import niwer.lumen.Console;
import niwer.lumen.LumenEngine;
import niwer.lumen.container.Container;
import niwer.photon.discord.BotEngine;
import niwer.photon.sql.AnticheatTable;
import niwer.photon.sql.CrashReportTable;
import niwer.photon.sql.DiscordLogTable;
import niwer.photon.sql.HWIDTable;
import niwer.photon.sql.LicenseTable;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.sql.PurchaseTable;
import niwer.photon.sql.ServerTable;
import niwer.photon.sql.SubscriptionTable;
import niwer.photon.util.AccountRetentionPurger;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.TranslationManager;
import niwer.photon.util.stripe.StripeStartupSync;
import niwer.photon.web.WebServerEngine;
import niwer.queryon.DataBase;
import niwer.queryon.SchemaPrunePolicy;
import niwer.queryon.backup.DatabaseBackupManager;

/**
 * Main entry point. This class is responsible for initializing stuff, loading features, registering database tables, starting the Discord bot, and launching the web API and server.
 * 
 * @author Niwer
 */
public class PhotonEngine {

    private static volatile String currentIP = null;

    public static final DataBase DATA_BASE = new DataBase(Directories.DATA_BASE_FILE);
    public static final Container LOGGER = LumenEngine.registerContainer("PhotonEngine").addProcessor((data, time, formattedMessage) -> {
        if(BotEngine.isBotInitialized()) BotEngine.log(data); // Print the log to the discord channel if the bot is initialized
    }).setSaveFolder(Directories.LOGS_DIR, "network");

    /**
	 * Get the current IP of the user using the Amazon AWS service
	 * @return the current IP of the user
	 */
    public static synchronized String getCurrentIP() {
        if (currentIP != null) return currentIP;

        try {
            final URL whatismyip = new URI("http://checkip.amazonaws.com").toURL();
            final BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            currentIP = in.readLine();
            in.close();
            return currentIP;
        } catch (IOException | URISyntaxException e) {}
        return "UNKNOWN";
    }

	/**
	 * Check if the current IP is equals to the given IP
     *
	 * @param ip the IP to check
	 * @return true if the current IP is equals to the given IP
	 */
    public static boolean isIPEquals(String ip) { return ip.equalsIgnoreCase(getCurrentIP()); }

	/**
	 * Check if the given IP is online (ping)
     *
	 * @param op the IP to check
	 * @return true if the given IP is online
	 * @throws UnknownHostException : if the IP is not valid
	 * @throws IOException : if the IP is not reachable
	 */
	public static boolean isOnline(String op) throws UnknownHostException, IOException { return InetAddress.getByName(op).isReachable(100); }

    /**
     * Get the current date in the official format
     *
     * @param showTime if true, the time will be added to the date
     * @param date the date to format, if null, the current date will be used
     * @return The formatted current date as a String
     */
    public static String getDate(boolean showTime) { return getDate(showTime, new Date()); }

    /**
     * Get the date in the official format
     *
     * @param showTime if true, the time will be added to the date
     * @param date the date to format, if null, the current date will be used
     * @return The formatted date as a String
     */
    public static String getDate(boolean showTime, Date date) {
        return new SimpleDateFormat("dd-mm-yyyy"+(showTime?"_HH-mm-ss":"")).format(date);
    }

    public static void main(final String[] args) {
        /* Add logs cleaner (We'll clean up logs after 30 days) */
        LumenEngine.registerLogsCleanerFor(LOGGER, TimeUnit.DAYS, 30);

        /* Load features */
        Directories.load();

        /* Load translations */
        TranslationManager.loadAllLanguages("lang");

        /* Register tables to the Data Base */
        DATA_BASE
            /* Security */
            .registerTable(HWIDTable.class)
            .registerTable(LicenseTable.class)
            .registerTable(PurchaseTable.class)
            .registerTable(SubscriptionTable.class)
            .registerTable(AnticheatTable.class)
            .registerTable(CrashReportTable.class)
            .registerTable(DiscordLogTable.class)

            /* User Accounts */
            .registerTable(PlayerAccountTable.class)
            .registerTable(ServerTable.class)
        ;
        DATA_BASE.setPrunePolicy(SchemaPrunePolicy.PRUNE_ALL);

        /* Run the database backup system */
        if(Directories.getConfig().database_backup_enabled) {
            final DatabaseBackupManager BACKUP_MANAGER = DatabaseBackupManager.createWithScheduler(DATA_BASE, Directories.BACKUPS_DIR, 
                Directories.getConfig().dbBackupFilePrefix(),
                (int)Directories.getConfig().database_backup_retention_days,
                Directories.getConfig().database_backup_interval_minutes
            );
            if(Directories.getConfig().database_backup_on_startup) BACKUP_MANAGER.createBackup("startup-backup");
        }

        /* Run the account retention purger */
        AccountRetentionPurger.createPurgerScheduler();

        DATA_BASE.syncSchema(); // We're syncing the schema after the backup to ensure that the backup to avoid losing data.

        /* Repopulate Stripe data on startup */
        StripeStartupSync.load();

        /* Starting the discord bot if token available */
        BotEngine.load(Arrays.asList(args).contains("--restart"));

        /* Starts the web API and Server */
        WebServerEngine.load();

        /* Log that the network server is running */
        Console.log("Network Server is now running and waiting for connections...").type(PhotonLogTypes.NETWORK).sendToProcessor().container(PhotonEngine.LOGGER).send();
    }
}
