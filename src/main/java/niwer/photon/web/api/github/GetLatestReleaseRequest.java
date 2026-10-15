package niwer.photon.web.api.github;

import java.net.http.HttpResponse;
import java.util.Optional;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectGithubRelease;
import niwer.photon.objects.ObjectProduct;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * Fetches the latest published release of a GitHub repository.
 * 
 * @author Niwer
 */
public class GetLatestReleaseRequest extends GithubApiRequest<Optional<ObjectGithubRelease>> {

    private final String owner;
    private final String repo;

    public GetLatestReleaseRequest(ObjectProduct product) {
        this(product.repoOwner(), product.repoName());
    }

    public GetLatestReleaseRequest(String owner, String repo) {
        this.owner = owner;
        this.repo = repo;
    }

    @Override
    public String url() {
        return "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public Optional<ObjectGithubRelease> request() {
        try {
            final HttpResponse<String> response = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                // No published releases found
                return Optional.empty();
            }

            if (response.statusCode() != 200) {
                Console.log("Failed to fetch latest release for %s/%s (Status: %d)", owner, repo, response.statusCode())
                        .type(PhotonLogTypes.WEB_SERVER)
                        .container(PhotonEngine.LOGGER)
                        .send();
                return Optional.empty();
            }

            final ObjectGithubRelease release = GsonUtils.GSON.fromJson(response.body(), ObjectGithubRelease.class);
            return Optional.ofNullable(release);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}