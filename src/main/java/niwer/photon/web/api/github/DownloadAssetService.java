package niwer.photon.web.api.github;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * This will download assets from GitHub releases.
 * TODO : In the future, this should be replaced by modrinth for all projects that are hosted there.
 * 
 * @author Niwer
 */

public class DownloadAssetService extends GithubApiRequest<InputStream> {

    private final String owner;
    private final String repo;
    private final long assetId;

    public DownloadAssetService(String owner, String repo, long assetId) {
        super(HttpClient.Redirect.NEVER);
        this.owner = owner;
        this.repo = repo;
        this.assetId = assetId;
    }

    @Override
    public String url() { return String.format("https://api.github.com/repos/%s/%s/releases/assets/%d", owner, repo, assetId); }

    @Override
    public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void addHeaders(Builder builder) {
        builder.header("Authorization", "Bearer " + Directories.config().github_pat)
            .header("Accept", "application/octet-stream")
            .header("X-GitHub-Api-Version", "2026-03-10")
            .header("Content-Type", "application/json");
    }

    @Override
    public InputStream request() {
        try {
            final HttpResponse<Void> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.discarding());
            if (RESPONSE.statusCode() != 302) {
                Console.log("GitHub asset download failed with status: %d", RESPONSE.statusCode()).error().type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                return null;
            }

            /* Download from AWS S3 without the Authorization header */
            String s3Url = RESPONSE.headers().firstValue("Location").orElseThrow(() -> new IllegalStateException("Redirect URL missing"));
            
            final HttpRequest S3_REQUEST = HttpRequest.newBuilder().uri(URI.create(s3Url)).GET().build();
            final HttpResponse<InputStream> S3_RESPONSE = this.CLIENT.send(S3_REQUEST, HttpResponse.BodyHandlers.ofInputStream());
            if (S3_RESPONSE.statusCode() != 200) {
                Console.log("S3 download failed with status: %d", S3_RESPONSE.statusCode()).error().type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                return null;
            }

            /* Stream the data directly to the client */
            return S3_RESPONSE.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
