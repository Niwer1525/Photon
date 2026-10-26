package niwer.photon.web.endpoints;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.annotations.SerializedName;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.Directories;
import niwer.photon.objects.IPayloadProvider;
import niwer.photon.objects.ObjectGithubGist;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.api.FetchWakatimeLanguagesRequests;
import niwer.photon.web.api.FetchWakatimeLanguagesRequests.WakatimeLanguagesResponse;
import niwer.photon.web.api.FetchWakatimeStatsRequest;
import niwer.photon.web.api.FetchWakatimeStatsRequest.WakatimeStatsResponse;
import niwer.photon.web.api.github.FetchGithubStatsRequest;
import niwer.photon.web.api.github.FetchGithubStatsRequest.GithubStatsResponse;
import niwer.photon.web.api.github.GetGistsRequest;

/**
 * Used to fetch statistics from Wakatime and Github for the authenticated user.
 * This endpoint aggregates the data from both services and returns it in a single response.
 */
public class StatisticsEndpoint implements IEndpoint {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Override public String path() { return "/statistics"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.MINUTES);

        try {
            /* Fetch all statistics concurrently since the services are independent. */
            final CompletableFuture<WakatimeStatsResponse> WAKA_GLOBAL_STATS_FUTURE = CompletableFuture.supplyAsync(() -> new FetchWakatimeStatsRequest().request(), EXECUTOR);
            final CompletableFuture<WakatimeLanguagesResponse> WAKA_LANGS_STATS_FUTURE = CompletableFuture.supplyAsync(() -> new FetchWakatimeLanguagesRequests().request(), EXECUTOR);
            final CompletableFuture<GithubStatsResponse> GH_STATS_FUTURE = CompletableFuture.supplyAsync(() -> new FetchGithubStatsRequest().request(), EXECUTOR);
            final CompletableFuture<List<ObjectGithubGist>> GH_GISTS_FUTURE = CompletableFuture.supplyAsync(() -> new GetGistsRequest(Directories.config().github_username).request(), EXECUTOR);

            /* Wait for all the futures to complete. */
            CompletableFuture.allOf(WAKA_GLOBAL_STATS_FUTURE, WAKA_LANGS_STATS_FUTURE, GH_STATS_FUTURE, GH_GISTS_FUTURE).join();

            /* Extract the results from the futures. */
            {
                final WakatimeStatsResponse WAKA_GLOBAL_STATS = WAKA_GLOBAL_STATS_FUTURE.join();
                final WakatimeLanguagesResponse WAKA_LANGS_STATS = WAKA_LANGS_STATS_FUTURE.join();
                if(WAKA_GLOBAL_STATS == null || WAKA_LANGS_STATS == null) {
                    handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to fetch Wakatime statistics.");
                    return;
                }
    
                final GithubStatsResponse GH_STATS = GH_STATS_FUTURE.join();
                if(GH_STATS == null) {
                    handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to fetch Github statistics.");
                    return;
                }

                final List<ObjectGithubGist> GH_GISTS = GH_GISTS_FUTURE.join();
                if(GH_GISTS == null) {
                    handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to fetch Github gists.");
                    return;
                }
    
                handler.status(HttpStatus.OK)
                    .contentType("application/json")    
                    .json(new StatisticsResponse(WAKA_GLOBAL_STATS, WAKA_LANGS_STATS, GH_STATS, GH_GISTS).payload());
            }
        } catch (Exception e) {
            IEndpoint.handleException(StatisticsEndpoint.class, handler, e);
        }
    }

    public static class StatisticsResponse implements IPayloadProvider {
        @SerializedName("wakatime_global_stats") WakatimeStatsResponse stats;
        @SerializedName("wakatime_individual_stats") WakatimeLanguagesResponse langs;
        @SerializedName("gh_stats") GithubStatsResponse gh_stats;
        @SerializedName("gh_gists") List<ObjectGithubGist> gh_gists;

        public StatisticsResponse(WakatimeStatsResponse stats, WakatimeLanguagesResponse langs, GithubStatsResponse gh_stats, List<ObjectGithubGist> gh_gists) {
            this.stats = stats;
            this.langs = langs;
            this.gh_stats = gh_stats;
            this.gh_gists = gh_gists;
        }
    }
}