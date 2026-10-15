package niwer.photon.util.license;

import niwer.photon.sql.LicenseTable;

/**
 * This class is responsible for purging expired licenses from the database.
 * It runs as a background thread and periodically checks for licenses that have passed their expiration date, marking them as "EXPIRED" in the database.
 */
public class LicensePurger implements Runnable {

    public static void createPurgerScheduler() {
        Thread purgerThread = new Thread(new LicensePurger(), "LicensePurger");
        purgerThread.setDaemon(true);
        purgerThread.setPriority(Thread.MIN_PRIORITY);
        purgerThread.start();
    }

    @Override
    public void run() {
        LicenseTable.checkExpired();
    }
}
