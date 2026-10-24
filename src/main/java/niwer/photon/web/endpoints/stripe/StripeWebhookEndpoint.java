package niwer.photon.web.endpoints.stripe;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import io.javalin.http.Context;
import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectProduct;
import niwer.photon.sql.PurchaseTable;
import niwer.photon.sql.SubscriptionTable;
import niwer.photon.util.GitHubProvisioningService;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.stripe.StripeHelper;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class StripeWebhookEndpoint implements IEndpoint {

    @Override public String path() { return "/stripe/webhook"; }
    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context ctx) {
        final String API_KEY = Directories.config().stripe_api_key;
        if (API_KEY == null || API_KEY.isBlank()) {
            ctx.status(500).result("stripe_api_key is not configured");
            return;
        }
        Stripe.apiKey = API_KEY;

        final Event EVENT;
        try {
            final String WEBHOOK_SECRET = Directories.config().stripe_webhook_signature;
            final String SIG_HEADER = ctx.header("Stripe-Signature");
            EVENT = (WEBHOOK_SECRET != null && !WEBHOOK_SECRET.isBlank()) ? Webhook.constructEvent(ctx.body(), SIG_HEADER, WEBHOOK_SECRET) : Event.GSON.fromJson(ctx.body(), Event.class);
        } catch (SignatureVerificationException e) {
            Console.log("Invalid signature: " + e.getMessage()).type(PhotonLogTypes.STRIPE).error().container(PhotonEngine.LOGGER).send();
            ctx.status(400).result("Invalid signature");
            return;
        } catch (Exception e) {
            ctx.status(400).result("Invalid payload");
            return;
        }

        final StripeObject STRIPE_OBJECT = EVENT.getDataObjectDeserializer().getObject().orElse(null);
        if (STRIPE_OBJECT == null) {
            ctx.status(200).result("Ignored: schema mismatch");
            return;
        }

        try {
            switch (EVENT.getType()) {
                case "checkout.session.completed" -> handleCheckoutSession((Session) STRIPE_OBJECT);
                case "customer.subscription.created", "customer.subscription.updated" -> syncSubscription((Subscription) STRIPE_OBJECT);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted((Subscription) STRIPE_OBJECT);
                // case "invoice.paid" -> 
                default -> Console.log("Unhandled webhook event: " + EVENT.getType()).type(PhotonLogTypes.STRIPE).container(PhotonEngine.LOGGER).send();
            }
            ctx.status(200).result("ok");
        } catch (Exception e) {
            Console.log("Error processing webhook " + EVENT.getType() + ": " + e.getMessage()).type(PhotonLogTypes.STRIPE).error().container(PhotonEngine.LOGGER).send();
            ctx.status(500).result("Internal error");
        }
    }

    private void handleCheckoutSession(Session session) throws Exception {
        /* Seed the purchase reference used by account creation/login redemption. */
        PurchaseTable.upsertCompletedPurchase(session);

        if ("subscription".equalsIgnoreCase(session.getMode())) {
            /* If the session is for a subscription, we need to sync the subscription. */
            if (session.getSubscription() != null) syncSubscription(Subscription.retrieve(session.getSubscription()));
            return;
        }
    }

    private void syncSubscription(Subscription sub) {
        SubscriptionTable.upsertSubscription(sub);

        /* Manage access of the repository to the user if one is provided. */
        final String GITHUB_USERNAME = sub.getMetadata() != null ? sub.getMetadata().get("github_username") : null;
        final ObjectProduct PRODUCT = getProductFromSubscription(sub);
        if (StripeHelper.mapSubscriptionStatus(sub.getStatus()).isActive()) // If the subscription is active, provision access to the repository for the user.
            GitHubProvisioningService.provisionAsync(GITHUB_USERNAME, PRODUCT);
        else // Otherwise, revoke access to the repository for the user.
            GitHubProvisioningService.revokeAsync(sub.getCustomer(), PRODUCT);
    }

    private void handleSubscriptionDeleted(Subscription sub) {
        SubscriptionTable.cancelSubscription(sub.getId());
        GitHubProvisioningService.revokeAsync(sub.getCustomer(), getProductFromSubscription(sub));
    }

    private static ObjectProduct getProductFromSubscription(Object obj) {
        if(obj instanceof Session session) {
            final var PRICE = StripeHelper.getPriceFromSession(session);
            if (PRICE == null) return null;

            return Directories.config().productByStripePriceId(PRICE.getId());
        }
        if(obj instanceof Subscription sub) {
            final var PRICE = (sub.getItems() != null && !sub.getItems().getData().isEmpty()) ? sub.getItems().getData().get(0).getPrice() : null;
            if (PRICE == null) return null;

            return Directories.config().productByStripePriceId(PRICE.getId());
        }
        return null;
    }
}