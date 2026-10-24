package niwer.photon.objects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a product, this class is mainly used inside the config.json file.
 * 
 * @author Niwer
 */
public class ObjectProduct implements IPayloadProvider {

    @SerializedName("id") private String id;
    @SerializedName("name") private String name;

    @SerializedName("discord_role_id") private String discordRoleId; // The Discord role ID associated when a user buys or subscribes to this product.
    
    @SerializedName("is_license_required") private boolean isLicenseRequired = true; // If set to true, this product requires users to create licenses to use it. If set to false, users can use this product without a license (E.G : Content-Packs).
    @SerializedName("default_license_duration_days") private Long defaultLicenseDurationDays;

    @SerializedName("stripe_price_ids") private Set<String> stripePriceIDs;
    @SerializedName("is_subscription") private boolean isSubscription;

    @SerializedName("repo_owner") private String repoOwner;
    @SerializedName("repo_name") private String repoName;
    @SerializedName("excluded_release_tags") private Set<String> excludedReleaseTags; // Excluded release tags (e.g., ["v0.1.0-alpha", "v1.0.4-broken"])

    @SerializedName("github_template") private ObjectGithubTemplate githubTemplate; // The GitHub template repository to use when creating new repositories for users.

    public ObjectProduct() {}

    public ObjectProduct(String id, String name, Long defaultDurationDays) {
        this.id = id;
        this.name = name;
        this.defaultLicenseDurationDays = defaultDurationDays;
    }

    public ObjectProduct(String id, String name, Long defaultDurationDays, Set<String> stripePriceId) {
        this(id, name, defaultDurationDays);
        this.stripePriceIDs = stripePriceId;
    }

    public String id() { return this.id; }

    public String name() { return this.name; }

    public String discordRoleId() { return this.discordRoleId; }

    public boolean isLicenseRequired() { return this.isLicenseRequired; }

    public Long defaultLicenseDurationDays() { return this.defaultLicenseDurationDays; }

    public Set<String> stripePriceIds() { return this.stripePriceIDs; }

    public boolean isSubscription() { return this.isSubscription; }

    public String repoOwner() { return this.repoOwner; }

    public String repoName() { return this.repoName; }
    
    public String repoKey() { return this.repoOwner + "/" + this.repoName; }

    public Set<String> excludedReleaseTags() {
        return this.excludedReleaseTags == null ? Collections.emptySet() : this.excludedReleaseTags;
    }

    public ObjectGithubTemplate githubTemplate() { return this.githubTemplate; }

    /**
     * @return True if the config for this product has repo owner and has a repo name.
     */
    public boolean hasRepo() {
        return (this.repoOwner != null && !this.repoOwner.isEmpty())
            && (this.repoName != null && !this.repoName.isEmpty());
    }

    @Override 
    public Map<String, Object> payload() {
        final Map<String, Object> PAYLOAD = new LinkedHashMap<>();
        PAYLOAD.put("id", this.id);
        PAYLOAD.put("name", this.name);
        PAYLOAD.put("defaultLicenseDurationDays", this.defaultLicenseDurationDays);
        return PAYLOAD;
    }

    @Override
    public String toString() {
        return String.format("ObjectProduct{id='%s', name='%s', is_license_required=%s, default_license_duration_days=%s, stripe_price_ids=%s, repo_owner='%s', repo_name='%s', excluded_release_tags=%s}",
            id, name, isLicenseRequired, defaultLicenseDurationDays, stripePriceIDs, repoOwner, repoName, excludedReleaseTags);
    }

    public static final class ObjectGithubTemplate {
        @SerializedName("template_repo_owner") private String repoOwner; // The owner of the template repository to use when creating new repositories for users.
        @SerializedName("template_repo_name") private String repoName; // The name of the template repository to use when creating new repositories for users.
        @SerializedName("new_repo_owner") private String newRepoOwner; // The owner of the new repository to create.
        @SerializedName("customer_teams") private Set<String> customerTeams = Set.of("customers"); // The GitHub teams (slugs) that will be added to the new repository when created.

        public boolean isEmpty() {
            return (this.repoOwner == null || this.repoOwner.isBlank())
                && (this.repoName == null || this.repoName.isBlank())
                && (this.newRepoOwner == null || this.newRepoOwner.isBlank())
                && (this.customerTeams == null || this.customerTeams.isEmpty());
        }

        public String repoOwner() { return this.repoOwner; }

        public String repoName() { return this.repoName; }

        public String newRepoOwner() { return this.newRepoOwner; }

        public Set<String> customerTeams() { return this.customerTeams; }
    }
}
