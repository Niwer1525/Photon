package niwer.photon.web.api.github;

import java.net.http.HttpResponse;

import com.google.gson.annotations.SerializedName;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectProduct.ObjectGithubTemplate;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * This class is used to set the permissions of a user for a specific repository on GitHub.
 * 
 * @author Niwer
 */
public class SetRepositoryPermissionsRequest extends GithubApiRequest<Void> {

    @SerializedName("permission")
    private final String permission; // "admin", "maintain", "write", "triage", or "read"

    private final transient String repositoryName;
    private final transient String githubUsername;
    private final transient String templateRepoOwner;

    public SetRepositoryPermissionsRequest(String githubUsername, ObjectGithubTemplate template) {
        this(githubUsername, template, "admin");
    }

    public SetRepositoryPermissionsRequest(String githubUsername, ObjectGithubTemplate template, String permission) {
        this(githubUsername, template.newRepoOwner(), template.repoName(), permission);
    }

    public SetRepositoryPermissionsRequest(String githubUsername, String templateRepoOwner, String templateRepoName) { this(githubUsername, templateRepoOwner, templateRepoName, "admin"); }

    public SetRepositoryPermissionsRequest(String githubUsername, String templateRepoOwner, String templateRepoName, String permission) {
        this.githubUsername = githubUsername;
        this.templateRepoOwner = templateRepoOwner;
        this.repositoryName = templateRepoName + "-" + this.sanitizeAndLower(githubUsername);
        this.permission = permission != null ? permission : "admin";
    }

    @Override
    public String url() {
        return String.format("https://api.github.com/repos/%s/%s/collaborators/%s", this.templateRepoOwner, this.repositoryName, this.githubUsername);
    }

    @Override
    public HttpMethod method() { return HttpMethod.PUT; }

    @Override
    public Void request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch(RESPONSE.statusCode()) {
                case 201 -> Console.log("Collaborator invitation sent successfully for " + this.githubUsername).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                case 204 -> Console.log("Permission updated successfully for " + this.githubUsername).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                default -> Console.log("Error (" + RESPONSE.statusCode() + ") : " + RESPONSE.body()).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}