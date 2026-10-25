package niwer.photon.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import niwer.photon.sql.BlogPostsTable;

public class BlogPostScheduler implements Runnable {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "BlogPostScheduler"));

    public static void createScheduler() {
        SCHEDULER.scheduleAtFixedRate(new BlogPostScheduler(), 0, 15, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        BlogPostsTable.checkScheduledPosts();
    }
}
