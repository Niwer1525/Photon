package niwer.photon.web.api.github;

import java.net.http.HttpResponse;

import com.google.gson.annotations.SerializedName;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * This class is used to create a new repository from a template repository on GitHub.
 * 
 * @author Niwer
 */
public class CreateRepositoryRequest extends GithubApiRequest<Void> {

    @SerializedName("owner") private final String owner;
    @SerializedName("name") private final String name; // Repository name
    @SerializedName("description") private final String description;
    @SerializedName("include_all_branches") private final boolean include_all_branches = false; // We only include the default branch for now
    @SerializedName("private") private final boolean private_repo = true; // We're only creating private repos for now

    private final transient String TEMPLATE_NAME = Directories.config().github_template_repo;

    public CreateRepositoryRequest(String customer) {
        this.owner = Directories.config().github_new_repo_owner;
        this.name = TEMPLATE_NAME + "-" + this.sanitizeAndLower(customer);
        this.description = "Repository for " + customer;
    }
    
    @Override
    public String url() {
        return String.format("https://api.github.com/repos/%s/%s/generate", Directories.config().github_template_owner, TEMPLATE_NAME);
    }

    @Override
    public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public Void request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch(RESPONSE.statusCode()) {
                case 201 -> Console.log("Repository created successfully.").type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
                default -> Console.log("Error (" + RESPONSE.statusCode() + ") : " + RESPONSE.body()).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}