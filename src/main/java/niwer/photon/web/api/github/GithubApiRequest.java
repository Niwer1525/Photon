package niwer.photon.web.api.github;

import java.net.http.HttpClient;
import java.net.http.HttpRequest.Builder;

import niwer.photon.Directories;
import niwer.photon.web.api.ApiRequest;

public abstract class GithubApiRequest<T> extends ApiRequest<T> {

    protected GithubApiRequest() {
        super();
    }

    protected GithubApiRequest(HttpClient client) {
        super(client);
    }

    protected GithubApiRequest(HttpClient.Redirect redirect) {
        super(redirect);
    }

    @Override
    public void addHeaders(Builder builder) {
        builder.header("Authorization", "Bearer " + Directories.config().github_pat)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2026-03-10")
            .header("Content-Type", "application/json");
    }
}
