package niwer.photon.sql;

import java.util.Date;
import java.util.List;

import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;

import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectSubscription;
import niwer.photon.util.stripe.StripeHelper;
import niwer.photon.util.stripe.StripePurchaseStatus;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.UpdateManager;
import niwer.queryon.tables.Table;

/**
 * @author Niwer
 */
public class SubscriptionTable extends Table {

    public SubscriptionTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectSubscription.class).execute();
    }

    @Override public String name() { return "Subscription"; }

    /**
     * Retrieves a subscription record by the associated email address.
     * 
     * @param email The email address associated with the subscription.
     * @return An ObjectSubscription instance if found, otherwise null.
     */
    public static ObjectSubscription getByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return SelectionManager.select(PhotonEngine.DATA_BASE, SubscriptionTable.class)
            .where(Expressions.isEqualTo("customer_email", StripeHelper.normalizeEmail(email)))
            .limit(1)
            .executeSerializable(ObjectSubscription.class);
    }

    private static ObjectSubscription getBySubscriptionId(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) return null;
        return SelectionManager.select(PhotonEngine.DATA_BASE, SubscriptionTable.class)
            .where(Expressions.isEqualTo("subscription_id", subscriptionId))
            .limit(1)
            .executeSerializable(ObjectSubscription.class);
    }

    /**
     * Retrieves a subscription record by the associated Stripe customer ID.
     * 
     * @param customerId The Stripe customer ID associated with the subscription.
     * @return An ObjectSubscription instance if found, otherwise null.
     */
    public static ObjectSubscription getByCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) return null;
        return SelectionManager.select(PhotonEngine.DATA_BASE, SubscriptionTable.class)
            .where(Expressions.isEqualTo("customer_id", customerId))
            .limit(1)
            .executeSerializable(ObjectSubscription.class);
    }

    /**
     * This method retrieves all subscription records associated with a specific account UUID. 
     */
    public static List<ObjectSubscription> getByAccountUuid(String accountUuid) {
        if (accountUuid == null || accountUuid.isBlank()) return List.of();
        return SelectionManager.select(PhotonEngine.DATA_BASE, SubscriptionTable.class)
            .where(Expressions.isEqualTo("account_uuid", accountUuid))
            .executeList(ObjectSubscription.class);
    }

    /**
     * Upserts a subscription record based on the provided Stripe Subscription object. If a subscription with the same subscription ID already exists, it will be updated; otherwise, a new record will be created.
     * 
     * @param sub The Stripe Subscription object containing the subscription details to be upserted.
     * @return An ObjectSubscription instance representing the upserted subscription record.
     */
    public static ObjectSubscription upsertSubscription(Subscription sub) {
        if (sub == null) return null;

        Customer CUSTOMER = sub.getCustomerObject();
        if (CUSTOMER == null && sub.getCustomer() != null && !sub.getCustomer().isBlank()) {
            try {
                CUSTOMER = Customer.retrieve(sub.getCustomer());
            } catch (Exception ignored) {}
        }
        final String EMAIL = StripeHelper.normalizeEmail(CUSTOMER != null ? CUSTOMER.getEmail() : null);
        if (EMAIL == null || EMAIL.isBlank()) return null;

        final Price PRICE = (sub.getItems() != null && !sub.getItems().getData().isEmpty()) ? sub.getItems().getData().get(0).getPrice() : null;

        Date expiresAt = null;
        if (sub.getEndedAt() != null) expiresAt = new Date(sub.getEndedAt() * 1000L);
        else if (sub.getItems() != null && !sub.getItems().getData().isEmpty()) {
            final SubscriptionItem item = sub.getItems().getData().get(0);
            if (item.getCurrentPeriodEnd() != null) expiresAt = new Date(item.getCurrentPeriodEnd() * 1000L);
        }

        final StripePurchaseStatus STATUS = StripeHelper.mapSubscriptionStatus(sub.getStatus());
        final String NAME = CUSTOMER != null ? CUSTOMER.getName() : null;

        // Preserve existing linked account UUID if present
        final ObjectSubscription EXISTING = getBySubscriptionId(sub.getId());
        final String ACCOUNT_UUID = EXISTING != null ? EXISTING.accountUuid() : null;

        return upsertSubscription(EMAIL, NAME, sub.getCustomer(), sub.getId(), STATUS, expiresAt, ACCOUNT_UUID, StripeHelper.resolveProductId(PRICE));
    }

    /**
     * Upserts a subscription record with the provided details. If a subscription with the same subscription ID already exists, it will be updated; otherwise, a new record will be created.
     * 
     * @param email The email address associated with the subscription.
     * @param customerName The name of the customer associated with the subscription.
     * @param customerId The Stripe customer ID associated with the subscription.
     * @param subscriptionId The Stripe subscription ID.
     * @param status The status of the subscription (e.g., ACTIVE, CANCELED).
     * @param expiresAt The expiration date of the subscription, if applicable.
     * @param accountUuid The UUID of the account associated with the subscription.
     * @param productId The product ID associated with the subscription.
     * @return An ObjectSubscription instance representing the upserted subscription record.
     */
    public static ObjectSubscription upsertSubscription(String email, String customerName, String customerId, String subscriptionId, StripePurchaseStatus status, Date expiresAt, String accountUuid, String productId) {
        final String normalizedEmail = StripeHelper.normalizeEmail(email);
        final Date updatedAt = new Date();
        final ObjectSubscription current = getBySubscriptionId(subscriptionId);
        final ObjectSubscription currentByCustomerId = current == null ? getByCustomerId(customerId) : null;
        final ObjectSubscription currentByAccountUuid = current == null && currentByCustomerId == null ? getByAccountUuid(accountUuid).stream().filter(subscription -> productId == null || productId.equals(subscription.productId())).findFirst().orElse(null) : null;
        final ObjectSubscription existing = current != null ? current : (currentByCustomerId != null ? currentByCustomerId : currentByAccountUuid);
        if (existing == null && (subscriptionId == null || subscriptionId.isBlank())) return null;
        final String nextAccountUuid = accountUuid != null && !accountUuid.isBlank() ? accountUuid : (existing == null ? null : existing.accountUuid());

        if (existing == null) {
            InsertionManager.insert(PhotonEngine.DATA_BASE, SubscriptionTable.class, "customer_email", "account_uuid", "customer_name", "customer_id", "subscription_id", "product_id", "status", "expires_at", "updated_at")
                .row(normalizedEmail, nextAccountUuid, customerName, customerId, subscriptionId, productId, status.name(), expiresAt, updatedAt)
                .execute();
        } else {
            UpdateManager.update(PhotonEngine.DATA_BASE, SubscriptionTable.class)
                .set("customer_email", normalizedEmail)
                .set("account_uuid", nextAccountUuid)
                .set("customer_name", customerName)
                .set("customer_id", customerId)
                .set("product_id", productId != null && !productId.isBlank() ? productId : existing.productId())
                .set("subscription_id", subscriptionId != null && !subscriptionId.isBlank() ? subscriptionId : existing.subscriptionId())
                .set("status", status.name())
                .set("expires_at", expiresAt)
                .set("updated_at", updatedAt)
                .where(Expressions.isEqualTo(existing.subscriptionId() != null && !existing.subscriptionId().isBlank() ? "subscription_id" : "id", existing.subscriptionId() != null && !existing.subscriptionId().isBlank() ? existing.subscriptionId() : existing.id()))
                .execute();
        }

        return existing == null ? getBySubscriptionId(subscriptionId) : existing;
    }

    /**
     * Cancels a subscription by its subscription ID. This will update the subscription's status to CANCELED and set the expires_at timestamp to the current time.
     * 
     * @param subscriptionId The ID of the subscription to cancel.
     */
    public static void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) return;
        final ObjectSubscription existing = getBySubscriptionId(subscriptionId);
        if (existing != null) {
            upsertSubscription(
                existing.customerEmail(),
                existing.customerName(),
                existing.customerId(),
                existing.subscriptionId(),
                StripePurchaseStatus.CANCELED,
                new Date(),
                existing.accountUuid(),
                existing.productId()
            );
        }
    }
}