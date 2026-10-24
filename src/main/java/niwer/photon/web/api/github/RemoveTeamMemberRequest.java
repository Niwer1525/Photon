package niwer.photon.web.api.github;

import java.net.http.HttpResponse;

import niwer.lumen.Console;
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
    private final transient String newRepoOwner;
    private final transient String teamSlug;

    public RemoveTeamMemberRequest(String githubUsername, String newRepoOwner, String teamSlug) {
        this.githubUsername = githubUsername;
        this.newRepoOwner = newRepoOwner;
        this.teamSlug = teamSlug;
    }

    @Override
    public String url() {
        return String.format("https://api.github.com/orgs/%s/teams/%s/memberships/%s", this.newRepoOwner, teamSlug, this.githubUsername);
    }

    @Override
    public HttpMethod method() { return HttpMethod.DELETE; }

    @Override
    public Void request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch (RESPONSE.statusCode()) {
                case 204 -> Console.log(String.format("User '%s' successfully removed from team '%s'.", this.githubUsername, teamSlug)).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                default  -> Console.log(String.format("Error (%d) : %s", RESPONSE.statusCode(), RESPONSE.body())).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}