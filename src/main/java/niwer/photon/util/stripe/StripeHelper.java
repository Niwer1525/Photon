package niwer.photon.util.stripe;

import java.util.Date;

import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionListLineItemsParams;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.util.PhotonLogTypes;

public final class StripeHelper {

    private StripeHelper() {}

    /**
     * Retrieves the Price object associated with a Stripe Checkout Session. This method fetches the line items of the session and returns the price of the first item, if available.
     * 
     * @param session The Stripe Checkout Session from which to retrieve the price.
     * @return The Price object of the first line item in the session, or null if it cannot be retrieved.
     */
    public static Price getPriceFromSession(Session session) {
        try {
            final var LINE_ITEMS = session.listLineItems(SessionListLineItemsParams.builder().setLimit(1L).build());
            if (LINE_ITEMS != null && !LINE_ITEMS.getData().isEmpty()) return LINE_ITEMS.getData().get(0).getPrice();
        } catch (Exception ignored) {
            Console.log("Failed to retrieve line items for session " + session.getId() + ": " + ignored.getMessage()).error().type(PhotonLogTypes.STRIPE).error().container(PhotonEngine.LOGGER).send();
            return null;
        }
        return null;
    }

    /**
     * Maps a Stripe subscription status to a StripePurchaseStatus enum value.
     *  
     * @param stripeStatus The status string from Stripe's subscription object.
     * @return The corresponding StripePurchaseStatus enum value.
     */
    public static StripePurchaseStatus mapSubscriptionStatus(String stripeStatus) {
        if (stripeStatus == null) return StripePurchaseStatus.EXPIRED;
        return switch (stripeStatus) {
            case "active", "trialing" -> StripePurchaseStatus.ACTIVE;
            case "past_due", "unpaid" -> StripePurchaseStatus.PENDING;
            case "canceled" -> StripePurchaseStatus.CANCELED;
            default -> StripePurchaseStatus.EXPIRED;
        };
    }

    /**
     * Resolves the product ID from a Stripe Price object, using the application's configuration to map Stripe price to internal product IDs.
     * 
     * @param price The Stripe Price object from which to resolve the product ID.
     * @return The internal product ID corresponding to the Stripe Price, or null if it cannot be resolved.
     */
    public static String resolveProductId(Price price) {
        if (price == null) return null;
        
        final var PRODUCT = Directories.config().productByStripePriceId(price.getId());
        return PRODUCT != null ? PRODUCT.id() : null;
    }

    /**
     * Resolves the expiry date of a Stripe subscription, prioritizing the 'ended_at' timestamp, and falling back to the 'current_period_end' of the first subscription item if necessary.
     * 
     * @param sub The Stripe Subscription object from which to resolve the expiry date.
     * @return The expiry date of the subscription, or null if it cannot be determined.
     */
    public static Date resolveSubscriptionExpiry(Subscription sub) {
        if (sub.getEndedAt() != null) return new Date(sub.getEndedAt() * 1000L);
        if (sub.getItems() != null && !sub.getItems().getData().isEmpty()) {
            final SubscriptionItem ITEM = sub.getItems().getData().get(0);
            if (ITEM.getCurrentPeriodEnd() != null) return new Date(ITEM.getCurrentPeriodEnd() * 1000L);
        }
        return null;
    }

    /**
     * Extracts the Price object from a Stripe Subscription, returning the first subscription item's price if available.
     * 
     * @param sub The Stripe Subscription object from which to extract the Price.
     * @return The Price object of the first subscription item, or null if it cannot be found.
     */
    public static Price extractPrice(Subscription sub) {
        if (sub.getItems() != null && !sub.getItems().getData().isEmpty()) return sub.getItems().getData().get(0).getPrice();
        return null;
    }

    /**
     * Resolves the customer's email from either the Stripe Customer object or the Session.CustomerDetails object, prioritizing the Customer's email if available.
     * 
     * @param customer The Stripe Customer object from which to resolve the email.
     * @param details The Session.CustomerDetails object from which to resolve the email.
     * @return The customer's email, or null if it cannot be determined.
     */
    public static String resolveEmail(Customer customer, Session.CustomerDetails details) {
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) return customer.getEmail();
        return details != null ? details.getEmail() : null;
    }

    /**
     * Resolves the customer's name from either the Stripe Customer object or the Session.CustomerDetails object, prioritizing the Customer's name if available.
     * 
     * @param customer The Stripe Customer object from which to resolve the name.
     * @param details The Session.CustomerDetails object from which to resolve the name.
     * @return The customer's name, or null if it cannot be determined.
     */
    public static String resolveName(Customer customer, Session.CustomerDetails details) {
        if (customer != null && customer.getName() != null && !customer.getName().isBlank()) return customer.getName();
        return details != null ? details.getName() : null;
    }

    /**
     * Normalizes a purchase token by trimming whitespace. This ensures consistent formatting for storage and comparison.
     * 
     * @param purchaseToken The purchase token to normalize.
     * @return The normalized purchase token, or null if the input is null or blank.
     */
    public static String normalizeToken(String purchaseToken) {
        return purchaseToken == null ? null : purchaseToken.trim();
    }

    /**
     * Normalizes an email address by trimming whitespace and converting it to lowercase.
     * 
     * @param email The email address to normalize.
     * @return The normalized email address, or null if the input is null or blank.
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}