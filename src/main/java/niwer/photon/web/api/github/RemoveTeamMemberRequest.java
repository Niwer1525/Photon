package niwer.photon.web.api.github;

import java.net.http.HttpResponse;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * This class is used to remove a member from an organization team on GitHub.
 * 
 * @author Niwer
 */
public class RemoveTeamMemberRequest extends GithubApiRequest<Void> {

    private final transient String githubUsername;
    private final transient String TEAM_SLUG = Directories.config().github_customer_team;

    public RemoveTeamMemberRequest(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    @Override
    public String url() {
        return String.format("https://api.github.com/orgs/%s/teams/%s/memberships/%s", Directories.config().github_new_repo_owner, TEAM_SLUG, this.githubUsername);
    }

    @Override
    public HttpMethod method() { return HttpMethod.DELETE; }

    @Override
    public Void request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch (RESPONSE.statusCode()) {
                case 204 -> Console.log(String.format("User '%s' successfully removed from team '%s'.", this.githubUsername, TEAM_SLUG)).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                default  -> Console.log(String.format("Error (%d) : %s", RESPONSE.statusCode(), RESPONSE.body())).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}