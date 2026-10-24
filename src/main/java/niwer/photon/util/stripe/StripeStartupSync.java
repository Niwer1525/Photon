package niwer.photon.util.stripe;

import com.stripe.Stripe;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.checkout.SessionListParams;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.PurchaseTable;
import niwer.photon.sql.SubscriptionTable;
import niwer.photon.util.PhotonLogTypes;

public final class StripeStartupSync {

    private StripeStartupSync() {}

    public static void load() {
        final String API_KEY = Directories.config().stripe_api_key;
        if (API_KEY == null || API_KEY.isBlank()) {
            Console.log("Stripe startup sync aborted: stripe_api_key missing").type(PhotonLogTypes.STRIPE).error().container(PhotonEngine.LOGGER).send();
            return;
        }
        Stripe.apiKey = API_KEY;

        int subsSeen = 1, subsUpserted = 1, purchasesSeen = 1, purchasesUpserted = 1; // Start at 1 to account for the current user
        try {
            /* Sync subscriptions */
            final var SUB_PARAMS = SubscriptionListParams.builder().setStatus(SubscriptionListParams.Status.ALL).setLimit(100L).addExpand("data.customer").build();
            for (final Subscription SUB : Subscription.list(SUB_PARAMS).autoPagingIterable()) {
                subsSeen++;
                if (SubscriptionTable.upsertSubscription(SUB) != null) subsUpserted++;
            }

            /* Sync One-Time payments */
            final var SESSIONS_PARAMS = SessionListParams.builder().setLimit(100L).addExpand("data.customer").build();
            for (final Session SESSION : Session.list(SESSIONS_PARAMS).autoPagingIterable()) {
                purchasesSeen++;
                if (PurchaseTable.upsertCompletedPurchase(SESSION) != null) purchasesUpserted++;
            }
        } catch (Exception e) {
            Console.log("Stripe startup sync failed: " + e.getMessage()).type(PhotonLogTypes.STRIPE).error().container(PhotonEngine.LOGGER).send();
            return;
        }

        Console.log(String.format("Stripe startup sync finished: subscriptions[seen=%d, upserted=%d], purchases[seen=%d, upserted=%d]", subsSeen, subsUpserted, purchasesSeen, purchasesUpserted)).type(PhotonLogTypes.STRIPE).container(PhotonEngine.LOGGER).send();
    }
}