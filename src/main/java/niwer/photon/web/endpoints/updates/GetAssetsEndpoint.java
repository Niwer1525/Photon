package niwer.photon.web.endpoints.updates;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.objects.ObjectGithubRelease;
import niwer.photon.objects.ObjectGithubTag;
import niwer.photon.objects.ObjectProduct;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.api.github.GetReleasesRequest;
import niwer.photon.web.api.github.GetTagRequest;
import niwer.photon.web.api.github.GetTagShaRequest;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * Serves mod update files from GitHub releases.
 * TODO: Replace with Modrinth for hosted projects.
 * 
 * @author Niwer
 */
public class GetAssetsEndpoint implements IEndpoint {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /* Cache Configuration */
    private static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(15);
    private static volatile Map<String, ReleaseResult> cachedData = null;
    private static volatile long lastCacheUpdate = 0;
    private static final Object CACHE_LOCK = new Object();

    @Override public String path() { return "/download/list"; }
    
    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context ctx) {
        IEndpoint.setupRateLimit(ctx, 10, TimeUnit.MINUTES);

        try {
            /* Return cached response if still valid */
            final long NOW = System.currentTimeMillis();
            if (cachedData != null && (NOW - lastCacheUpdate) < CACHE_TTL_MILLIS) {
                ctx.json(cachedData);
                return;
            }
    
            /* Asynchronously refresh data if stale or null */
            ctx.future(() -> getOrRefreshDataAsync().thenAccept(ctx::json).exceptionally(e -> {
                e.printStackTrace();
                /* Fallback to stale cache if GitHub rate limits fail */
                if (cachedData != null) {
                    ctx.json(cachedData);
                    return null;
                }
                ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
                return null;
            }));
        } catch (Exception e) {
            IEndpoint.handleException(GetAssetsEndpoint.class, ctx, e);
        }
    }

    private CompletableFuture<Map<String, ReleaseResult>> getOrRefreshDataAsync() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (CACHE_LOCK) {
                final long NOW = System.currentTimeMillis();
                if (cachedData != null && (NOW - lastCacheUpdate) < CACHE_TTL_MILLIS) return cachedData;

                /* Filter products having a repo */
                final List<ObjectProduct> PRODUCTS = Directories.getConfig().getProducts().stream().parallel().filter(ObjectProduct::hasRepo).toList();

                /* Futures */
                final List<CompletableFuture<Map.Entry<String, ReleaseResult>>> FUTURES = PRODUCTS.stream().map(this::fetchProductAssetsAsync).toList();

                /* Aggregate the results */
                final Map<String, ReleaseResult> REFRESHED = CompletableFuture
                    .allOf(FUTURES.toArray(CompletableFuture[]::new))
                    .thenApply(ignored -> FUTURES.stream().map(future -> future.join()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> existing))).join();

                cachedData = REFRESHED;
                lastCacheUpdate = System.currentTimeMillis();
                return REFRESHED;
            }
        }, EXECUTOR);
    }

    private CompletableFuture<Map.Entry<String, ReleaseResult>> fetchProductAssetsAsync(final ObjectProduct product) {
        return CompletableFuture.supplyAsync(() -> {
            /* Fetch releases for the product */
            final List<ObjectGithubRelease> RELEASES = new GetReleasesRequest(product).request();

            /* Asynchronously resolve tag commit metadata for each release concurrently */
            final List<CompletableFuture<ObjectGithubTag>> TAG_FUTURES = RELEASES.stream().map(release -> fetchTagAsync(product, release)).toList();
            final List<ObjectGithubTag> TAGS = TAG_FUTURES.stream().map(future -> future.join()).filter(Objects::nonNull).toList();

            final ReleaseResult RESULT = new ReleaseResult(RELEASES, TAGS);
            return Map.entry(product.repoKey(), RESULT);
        }, EXECUTOR);
    }

    private CompletableFuture<ObjectGithubTag> fetchTagAsync(final ObjectProduct product, final ObjectGithubRelease release) {
        return CompletableFuture.supplyAsync(() -> {
            final String SHA = new GetTagShaRequest(product, release.tagName).request();
            if (SHA == null) return null;

            final ObjectGithubTag TAG = new GetTagRequest(product, SHA).request();
            return TAG;
        }, EXECUTOR);
    }

    public record ReleaseResult(@SerializedName("releases") List<ObjectGithubRelease> releases, @SerializedName("tags") List<ObjectGithubTag> tags) {
        public ReleaseResult {
            releases = releases != null ? releases : List.of();
            tags = tags != null ? tags : List.of();
        }
    }
}