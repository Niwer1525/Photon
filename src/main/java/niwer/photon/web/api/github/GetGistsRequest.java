package niwer.photon.web.api.github;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectGithubGist;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * Fetches the public gists of a GitHub user.
 */
public class GetGistsRequest extends GithubApiRequest<List<ObjectGithubGist>> {

    private final String githubUsername;

    public GetGistsRequest(String githubUsername) { this.githubUsername = githubUsername; }

    @Override
    public String url() { return "https://api.github.com/users/" + githubUsername + "/gists"; }

    @Override
    public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public List<ObjectGithubGist> request() {
        try {
            final HttpResponse<String> response = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Console.log("Failed to fetch gists for %s with status code %d", githubUsername, response.statusCode()).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                return Collections.emptyList();
            }

            final Type LIST_TYPE = new TypeToken<List<ObjectGithubGist>>() {}.getType();
            final List<ObjectGithubGist> GISTS = GsonUtils.GSON.fromJson(response.body(), LIST_TYPE);
            return GISTS == null ? Collections.emptyList() : GISTS;
        } catch (Exception e) {
            Console.log("Failed to fetch gists for %s: %s", githubUsername, e.getMessage()).error().type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            return Collections.emptyList();
        }
    }
}
