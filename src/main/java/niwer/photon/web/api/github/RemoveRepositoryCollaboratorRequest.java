package niwer.photon.web.api.github;

import java.net.http.HttpResponse;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * This class is used to revoke a user's collaborator access from a GitHub repository,
 * preventing any further read/write/clone access.
 * 
 * @author Niwer
 */
public class RemoveRepositoryCollaboratorRequest extends GithubApiRequest<Void> {

    private final transient String repositoryName;
    private final transient String githubUsername;

    public RemoveRepositoryCollaboratorRequest(String githubUsername) {
        this.githubUsername = githubUsername;
        this.repositoryName = Directories.config().github_template_repo + "-" + this.sanitizeAndLower(githubUsername);
    }

    public RemoveRepositoryCollaboratorRequest(String repositoryName, String githubUsername) {
        this.repositoryName = repositoryName;
        this.githubUsername = githubUsername;
    }

    @Override
    public String url() {
        return String.format("https://api.github.com/repos/%s/%s/collaborators/%s", Directories.config().github_new_repo_owner, this.repositoryName, this.githubUsername);
    }

    @Override
    public HttpMethod method() { return HttpMethod.DELETE; }

    @Override
    public Void request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch (RESPONSE.statusCode()) {
                case 204 -> Console.log(String.format("User '%s' access revoked from repository '%s'.", this.githubUsername, this.repositoryName)).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                default  -> Console.log(String.format("Error (%d) : %s", RESPONSE.statusCode(), RESPONSE.body())).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}