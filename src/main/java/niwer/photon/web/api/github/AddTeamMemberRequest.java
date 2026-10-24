package niwer.photon.web.api.github;

import java.net.http.HttpResponse;

import com.google.gson.annotations.SerializedName;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * This class is used to add or invite a GitHub user to an organization team.
 * 
 * @author Niwer
 */
public class AddTeamMemberRequest extends GithubApiRequest<Void> {

    @SerializedName("role")
    private final String role; // "member" or "maintainer"

    private final transient String githubUsername;
    private final transient String TEAM_SLUG = Directories.config().github_customer_team; // The slug of the team to which the user will be added

    public AddTeamMemberRequest(String githubUsername) {
        this(githubUsername, "member");
    }

    public AddTeamMemberRequest(String githubUsername, String role) {
        this.githubUsername = githubUsername;
        this.role = role != null ? role : "member";
    }

    @Override
    public String url() {
        return String.format("https://api.github.com/orgs/%s/teams/%s/memberships/%s", Directories.config().github_new_repo_owner, TEAM_SLUG, this.githubUsername);
    }

    @Override
    public HttpMethod method() { return HttpMethod.PUT; }

    @Override
    public Void request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch(RESPONSE.statusCode()) {
                case 200 -> Console.log(String.format("User '%s' successfully added/invited to team '%s'.", this.githubUsername, TEAM_SLUG)).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                default -> Console.log(String.format("Error (%d) : %s", RESPONSE.statusCode(), RESPONSE.body())).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}