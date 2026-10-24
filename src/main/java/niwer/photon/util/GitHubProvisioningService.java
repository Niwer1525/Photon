package niwer.photon.util;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectProduct;
import niwer.photon.objects.ObjectPurchase;
import niwer.photon.sql.PurchaseTable;
import niwer.photon.web.api.github.AddTeamMemberRequest;
import niwer.photon.web.api.github.CreateRepositoryRequest;
import niwer.photon.web.api.github.RemoveRepositoryCollaboratorRequest;
import niwer.photon.web.api.github.RemoveTeamMemberRequest;
import niwer.photon.web.api.github.SetRepositoryPermissionsRequest;

/**
 * This class handles the provisioning and revocation of GitHub access for users based on their Stripe subscription status.
 * 
 * @author Niwer
 */
public final class GitHubProvisioningService {

    private GitHubProvisioningService() {}

    /**
     * Provisions GitHub access for a user asynchronously. This method creates a repository, sets permissions, and adds the user to the team.
     * 
     * @param githubUsername The GitHub username of the user to provision access for.
     * @param product The product associated with the user's subscription, which may contain GitHub provisioning details.
     */
    public static void provisionAsync(String githubUsername, ObjectProduct product) {
        if (githubUsername == null || githubUsername.isBlank() || product == null) return;

        /* Get the GitHub template for the product */
        final var GH_TEMPLATE = product.githubTemplate();
        if(GH_TEMPLATE == null || GH_TEMPLATE.isEmpty()) {
            Console.log("GitHub provisioning skipped for " + githubUsername + ": No GitHub template configured for product " + product.id()).type(PhotonLogTypes.STRIPE).container(PhotonEngine.LOGGER).send();
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                /* Create the user's repository */
                new CreateRepositoryRequest(githubUsername, GH_TEMPLATE).request();
                Thread.sleep(2500); // Wait for GitHub to process the repository creation before setting permissions and adding the user to the team

                /* Set the repository permissions */
                new SetRepositoryPermissionsRequest(githubUsername, GH_TEMPLATE, "admin").request();
                for (final String TEAM_SLUG : GH_TEMPLATE.customerTeams()) new AddTeamMemberRequest(githubUsername, GH_TEMPLATE.newRepoOwner(), TEAM_SLUG).request();
            } catch (Exception e) {
                Console.log("Failed GitHub provisioning for " + githubUsername + ": " + e.getMessage()).type(PhotonLogTypes.STRIPE).error().container(PhotonEngine.LOGGER).send();
            }
        });
    }

    /**
     * Revokes GitHub access for a user asynchronously. This method removes the user from the repository and team.
     * 
     * @param stripeCustomerId The Stripe customer ID associated with the user whose GitHub access is to be revoked.
     * @param product The product associated with the user's subscription, which may contain GitHub provisioning details.
     */
    public static void revokeAsync(String stripeCustomerId, ObjectProduct product) {
        if (stripeCustomerId == null || stripeCustomerId.isBlank()) return;

        /* Get the GitHub template for the product */
        final var GH_TEMPLATE = product.githubTemplate();
        if(GH_TEMPLATE == null || GH_TEMPLATE.isEmpty()) {
            Console.log("GitHub provisioning skipped for customer " + stripeCustomerId + ": No GitHub template configured for product " + product.id()).type(PhotonLogTypes.STRIPE).container(PhotonEngine.LOGGER).send();
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                /* Get the user's purchase information */
                final ObjectPurchase PURCHASE = PurchaseTable.getByCustomerId(stripeCustomerId);
                if (PURCHASE == null || PURCHASE.githubUsername() == null || PURCHASE.githubUsername().isBlank()) {
                    Console.log("No GitHub username found for customer " + stripeCustomerId).type(PhotonLogTypes.STRIPE).container(PhotonEngine.LOGGER).send();
                    return;
                }

                /* Remove the user from the repository and team */
                new RemoveRepositoryCollaboratorRequest(PURCHASE.githubUsername(), GH_TEMPLATE).request();
                for (final String TEAM_SLUG : GH_TEMPLATE.customerTeams()) new RemoveTeamMemberRequest(PURCHASE.githubUsername(), GH_TEMPLATE.newRepoOwner(), TEAM_SLUG).request();
            } catch (Exception e) {
                Console.log("Error revoking GitHub access for customer " + stripeCustomerId + ": " + e.getMessage()).type(PhotonLogTypes.STRIPE).error().container(PhotonEngine.LOGGER).send();
            }
        });
    }
}