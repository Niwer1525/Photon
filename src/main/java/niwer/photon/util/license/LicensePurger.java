package niwer.photon.util.license;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import niwer.photon.sql.LicenseTable;

/**
 * This class is responsible for purging expired licenses from the database.
 * It runs as a background thread and periodically checks for licenses that have passed their expiration date, marking them as "EXPIRED" in the database.
 */
public class LicensePurger implements Runnable {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread purgerThread = new Thread(r, "LicensePurger");
        purgerThread.setDaemon(true);
        purgerThread.setPriority(Thread.MIN_PRIORITY);
        return purgerThread;
    });

    public static void createPurgerScheduler() {
        SCHEDULER.scheduleAtFixedRate(new LicensePurger(), 0, 24, TimeUnit.HOURS);
    }

    @Override
    public void run() {
        LicenseTable.checkExpired();
    }
}
