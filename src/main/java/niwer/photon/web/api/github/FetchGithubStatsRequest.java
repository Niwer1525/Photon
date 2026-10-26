package niwer.photon.web.api.github;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.IPayloadProvider;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

public class FetchGithubStatsRequest extends GithubApiRequest<FetchGithubStatsRequest.GithubStatsResponse> {

    private static final String VIEWER_BASE_QUERY = """
        query {
          viewer {
            login
            createdAt
            repositories(first: 100, ownerAffiliations: OWNER) {
              totalCount
              pageInfo { hasNextPage endCursor }
              nodes { stargazerCount }
            }
          }
        }
        """;

    private static final String REPOSITORIES_PAGE_QUERY = """
        query($after: String!) {
          viewer {
            repositories(first: 100, after: $after, ownerAffiliations: OWNER) {
              pageInfo { hasNextPage endCursor }
              nodes { stargazerCount }
            }
          }
        }
        """;

    private static final String CONTRIBUTIONS_FIELDS = """
        totalCommitContributions
        totalPullRequestContributions
        totalIssueContributions
        totalPullRequestReviewContributions
        restrictedContributionsCount
        contributionCalendar { totalContributions }
        """;

    // Request payload fields expected by the parent class / serializer
    private final String query;
    private final Map<String, Object> variables;

    public FetchGithubStatsRequest() {
        this(VIEWER_BASE_QUERY, Map.of());
    }

    public FetchGithubStatsRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }

    private FetchGithubStatsRequest(String query, Map<String, Object> variables, HttpClient client) {
        super(client);
        this.query = query;
        this.variables = variables;
    }

    @Override
    public String url() { return "https://api.github.com/graphql"; }

    @Override
    public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public GithubStatsResponse request() {
        final String token = Directories.config().github_pat;
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("GitHub PAT is not configured.");
        }

        try {
            // Initial request: gets profile info and the first 100 repositories
            final JsonObject viewerData = executeQuery(VIEWER_BASE_QUERY, Map.of()).getAsJsonObject("viewer");
            final String username = viewerData.get("login").getAsString();
            final int startYear = OffsetDateTime.parse(viewerData.get("createdAt").getAsString()).getYear();
            final int currentYear = Year.now(ZoneOffset.UTC).getValue();

            // Fetch all years' contributions in ONE batch query via GraphQL aliases
            final Map<Integer, GithubYearStats> years = fetchAllYearsAliased(startYear, currentYear);

            // Accumulate stars (includes initial batch + subsequent pages if >100 repos)
            final int totalStars = fetchAllStars(viewerData.getAsJsonObject("repositories"));

            return new GithubStatsResponse(
                username,
                Instant.now().toString(),
                years,
                totalStars
            );
        } catch (Exception e) {
            Console.log("Failed to fetch GitHub statistics: %s", e.getMessage())
                .error().type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            return null;
        }
    }

    /**
     * Builds a single query with aliases (e.g. y2021: contributionsCollection(...))
     * eliminating N sequential network round-trips.
     */
    private Map<Integer, GithubYearStats> fetchAllYearsAliased(int startYear, int currentYear) throws Exception {
        final StringBuilder batchQuery = new StringBuilder(Math.max(256, 256 + (currentYear - startYear + 1) * 200))
            .append("query {\n  viewer {\n");
        for (int y = startYear; y <= currentYear; y++) {
            batchQuery.append("    y").append(y)
                .append(": contributionsCollection(from: \"").append(y)
                .append("-01-01T00:00:00Z\", to: \"").append(y)
                .append("-12-31T23:59:59Z\") { ").append(CONTRIBUTIONS_FIELDS)
                .append(" }\n");
        }
        batchQuery.append("  }\n}");

        final JsonObject viewer = executeQuery(batchQuery.toString(), Map.of()).getAsJsonObject("viewer");
        final Map<Integer, GithubYearStats> statsMap = new LinkedHashMap<>();

        for (int y = startYear; y <= currentYear; y++) {
            final JsonObject col = viewer.getAsJsonObject("y" + y);
            statsMap.put(y, new GithubYearStats(
                getInt(col, "totalCommitContributions"),
                getInt(col, "totalPullRequestContributions"),
                getInt(col, "totalIssueContributions"),
                getInt(col, "totalPullRequestReviewContributions"),
                getInt(col, "restrictedContributionsCount"),
                col.getAsJsonObject("contributionCalendar").get("totalContributions").getAsInt()
            ));
        }
        return statsMap;
    }

    private int fetchAllStars(JsonObject reposBlock) throws Exception {
        int stars = sumStarsInNodes(reposBlock.getAsJsonArray("nodes"));
        JsonObject pageInfo = reposBlock.getAsJsonObject("pageInfo");

        while (pageInfo.get("hasNextPage").getAsBoolean()) {
            final String cursor = pageInfo.get("endCursor").getAsString();
            final JsonObject data = executeQuery(REPOSITORIES_PAGE_QUERY, Map.of("after", cursor));
            final JsonObject repos = data.getAsJsonObject("viewer").getAsJsonObject("repositories");

            stars += sumStarsInNodes(repos.getAsJsonArray("nodes"));
            pageInfo = repos.getAsJsonObject("pageInfo");
        }
        return stars;
    }

    private static int sumStarsInNodes(JsonArray nodes) {
        int count = 0;
        for (JsonElement node : nodes) {
            count += node.getAsJsonObject().get("stargazerCount").getAsInt();
        }
        return count;
    }

    /**
     * Stateless GraphQL execution. Uses a dedicated payload object while reusing this
     * request's HTTP client.
     */
    private JsonObject executeQuery(String queryText, Map<String, Object> vars) throws Exception {
        final FetchGithubStatsRequest delegate = new FetchGithubStatsRequest(queryText, vars, CLIENT);
        final HttpResponse<String> response = delegate.sendHttpRequest(HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("GitHub GraphQL returned HTTP " + response.statusCode());
        }

        final JsonObject payload = GsonUtils.parseJsonObject(response.body());
        if (payload == null || !payload.has("data") || payload.has("errors")) {
            final String err = payload != null && payload.has("errors") ? payload.get("errors").toString() : "unknown";
            throw new IllegalStateException("GitHub GraphQL error: " + err);
        }
        return payload.getAsJsonObject("data");
    }

    private static int getInt(JsonObject obj, String member) {
        return obj.get(member).getAsInt();
    }

    public record GithubStatsResponse(
        String user,
        @SerializedName("generated_at") String generatedAt,
        Map<Integer, GithubYearStats> years,
        @SerializedName("all_time_stars") int allTimeStars
    ) implements IPayloadProvider {}

    public record GithubYearStats(
        int commits,
        @SerializedName("pull_requests") int pullRequests,
        int issues,
        int reviews,
        @SerializedName("private_contributions") int privateContributions,
        @SerializedName("total_combined") int totalCombined
    ) implements IPayloadProvider {}
}