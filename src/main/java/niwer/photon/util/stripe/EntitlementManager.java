package niwer.photon.util.stripe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import niwer.photon.Directories;
import niwer.photon.objects.ObjectPurchase;
import niwer.photon.objects.ObjectSubscription;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PurchaseTable;
import niwer.photon.sql.SubscriptionTable;

public final class EntitlementManager {

    private EntitlementManager() {}

    /**
     * Checks if a given account has access to a specific product, either through an active subscription or a valid one-time purchase.
     * 
     * @param accountUuid The UUID of the user account to check access for.
     * @param productId The ID of the product to check access against.
     * @return True if the account has access to the product, false otherwise.
     */
    public static boolean hasAccess(String accountUuid, String productId) {
        if (accountUuid == null || accountUuid.isBlank() || productId == null || productId.isBlank()) return false;

        final boolean HAS_SUB_ACCESS = SubscriptionTable.getByAccountUuid(accountUuid).stream().anyMatch(sub -> productId.equals(sub.productId()) && sub.isActive());
        if (HAS_SUB_ACCESS) return true;

        return PurchaseTable.getByAccountUuid(accountUuid).stream().anyMatch(p -> productId.equals(p.productId()) && p.status() == StripePurchaseStatus.ACTIVE);
    }

    /**
     * Checks if a given account has any access to any product.
     *
     * @param accountUuid The UUID of the user account to check access for.
     * @return True if the account has any access, false otherwise.
     */
    public static boolean hasAnyAccess(String accountUuid) {
        if (accountUuid == null || accountUuid.isBlank()) return false;

        final boolean HAS_SUB_ACCESS = SubscriptionTable.getByAccountUuid(accountUuid).stream().anyMatch(ObjectSubscription::isActive);
        if (HAS_SUB_ACCESS) return true;

        return PurchaseTable.getByAccountUuid(accountUuid).stream().anyMatch(p -> p.status() == StripePurchaseStatus.ACTIVE);
    }

    /**
     * Retrieves a list of entitlements for a given account, including both active subscriptions and one-time purchases.
     * 
     * @param accountUuid The UUID of the user account to retrieve entitlements for.
     * @return A list of maps, each representing an entitlement with product ID, status, type, and relevant timestamps.
     */
    public static List<Map<String, Object>> getEntitlements(String accountUuid) {
        if (accountUuid == null || accountUuid.isBlank()) return List.of();

        final List<Map<String, Object>> SUBSCRIBPTIONS = SubscriptionTable.getByAccountUuid(accountUuid).stream()
            .map(item -> entitlementRecord(item.productId(), item.status(), item.expiresAt() == null ? null : item.expiresAt().getTime(), null))
            .map(item -> entitlement(item, "SUBSCRIPTION"))
            .toList();

        final List<Map<String, Object>> PURCHASES = PurchaseTable.getByAccountUuid(accountUuid).stream()
            .filter(p -> Directories.config().isOneTimeProduct(p))
            .map(item -> entitlementRecord(item.productId(), item.status(), null, item.createdAt() == null ? null : item.createdAt().getTime()))
            .map(item -> entitlement(item, "ONE_TIME"))
            .toList();

        return Stream.concat(SUBSCRIBPTIONS.stream(), PURCHASES.stream()).toList();
    }

    /**
     * Redeems a purchase for a given account. This will link the purchase to the account and also link any subscription if one exists for the customer or email.
     * 
     * @param purchaseToken The purchase token to redeem.
     * @param account The user account to which the purchase should be linked.
     * @return True if the purchase was successfully redeemed and linked, false otherwise.
     */
    public static boolean redeemPurchase(String purchaseToken, ObjectUserAccount account) {
        if (purchaseToken == null || purchaseToken.isBlank() || account == null) return false;

        final ObjectPurchase PURCHASE = PurchaseTable.getByPurchaseReference(purchaseToken);
        if (PURCHASE == null) return false;
        if (PURCHASE.linkedAccountUuid() != null && !PURCHASE.linkedAccountUuid().isBlank() && !PURCHASE.linkedAccountUuid().equals(account.getUuid())) return false;

        /* Link the purchase record first */
        final boolean MARKED = PurchaseTable.markAsRedeemed(purchaseToken, account.getUuid());
        if (!MARKED) return false;

        /* Link subscription if one exists for this customer or email */
        ObjectSubscription sub = null;
        if (PURCHASE.stripeCustomerId() != null && !PURCHASE.stripeCustomerId().isBlank()) sub = SubscriptionTable.getByCustomerId(PURCHASE.stripeCustomerId());
        if (sub == null && PURCHASE.customerEmail() != null && !PURCHASE.customerEmail().isBlank()) sub = SubscriptionTable.getByEmail(PURCHASE.customerEmail());
        if (sub != null) {
            SubscriptionTable.upsertSubscription(
                sub.customerEmail(),
                sub.customerName(),
                sub.customerId(),
                sub.subscriptionId(),
                sub.status(),
                sub.expiresAt(),
                account.getUuid(),
                sub.productId()
            );
        }

        return true;
    }

    private static Map<String, Object> entitlementRecord(String productId, StripePurchaseStatus status, Long expiresAt, Long createdAt) {
        final Map<String, Object> record = new LinkedHashMap<>();
        record.put("productId", productId);
        record.put("status", status);
        if (expiresAt != null) record.put("expiresAt", expiresAt);
        if (createdAt != null) record.put("createdAt", createdAt);
        return record;
    }

    private static Map<String, Object> entitlement(Map<String, Object> item, String type) {
        final Map<String, Object> record = new LinkedHashMap<>(item);
        record.put("type", type);
        return record;
    }
}