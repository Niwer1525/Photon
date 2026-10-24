package niwer.photon.web.api.github;

import java.net.http.HttpResponse;

import com.google.gson.annotations.SerializedName;

import niwer.lumen.Console;
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
    private final transient String newRepoOwner;
    private final transient String teamSlug;

    public AddTeamMemberRequest(String githubUsername, String newRepoOwner, String teamSlug) {
        this(githubUsername, newRepoOwner, teamSlug, "member");
    }

    public AddTeamMemberRequest(String githubUsername, String newRepoOwner, String teamSlug, String role) {
        this.githubUsername = githubUsername;
        this.newRepoOwner = newRepoOwner;
        this.teamSlug = teamSlug;
        this.role = role != null ? role : "member";
    }

    @Override
    public String url() {
        return String.format("https://api.github.com/orgs/%s/teams/%s/memberships/%s", this.newRepoOwner, this.teamSlug, this.githubUsername);
    }

    @Override
    public HttpMethod method() { return HttpMethod.PUT; }

    @Override
    public Void request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch(RESPONSE.statusCode()) {
                case 200 -> Console.log(String.format("User '%s' successfully added/invited to team '%s'.", this.githubUsername, this.teamSlug)).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                default -> Console.log(String.format("Error (%d) : %s", RESPONSE.statusCode(), RESPONSE.body())).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}