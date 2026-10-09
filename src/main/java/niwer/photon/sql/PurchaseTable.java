package niwer.photon.sql;

import java.util.Date;
import java.util.List;

import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionListLineItemsParams;

import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectPurchase;
import niwer.photon.util.stripe.StripeHelper;
import niwer.photon.util.stripe.StripePurchaseStatus;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.UpdateManager;
import niwer.queryon.tables.Table;

public class PurchaseTable extends Table {

    public PurchaseTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectPurchase.class).execute();
    }

    @Override public String name() { return "Purchase"; }

	/**
	 * Creates or retrieves a pending purchase record based on the provided purchase token and checkout session ID.
	 * If a purchase with the given token already exists, it will be returned. Otherwise, a new pending purchase record will be created in the database.
	 * 
	 * @param purchaseToken The unique token associated with the purchase.
	 * @param checkoutSessionId The Stripe checkout session ID associated with the purchase.
	 * @param customerEmail The email address of the customer.
	 * @param customerName The name of the customer.
	 * @param productId The ID of the product.
	 * @return The created or retrieved pending purchase.
	 */
    public static ObjectPurchase createOrRetrievePendingPurchase(String purchaseToken, String checkoutSessionId, String customerEmail, String customerName, String productId) {
        final ObjectPurchase CURRENT = getByPurchaseReference(purchaseToken);
        if (CURRENT != null) return CURRENT;

        final String TOKEN = StripeHelper.normalizeToken(purchaseToken);
        if (TOKEN == null || TOKEN.isBlank()) return null;

        final String CHECKOUT_SESSION_ID = checkoutSessionId == null || checkoutSessionId.isBlank() ? TOKEN : checkoutSessionId.trim();
        final Date NOW = new Date();
        InsertionManager.insert(PhotonEngine.DATA_BASE, PurchaseTable.class, "purchase_token", "checkout_session_id", "customer_email", "customer_name", "product_id", "status", "created_at", "updated_at")
            .row(TOKEN, CHECKOUT_SESSION_ID, StripeHelper.normalizeEmail(customerEmail), customerName, productId, StripePurchaseStatus.PENDING, NOW, NOW)
            .execute();
        return getByToken(TOKEN);
    }

	/**
	 * Upserts a completed purchase record based on the provided Stripe checkout session.
	 * This method retrieves the necessary details from the session, including customer information and product ID, and either creates a new purchase record or updates an existing one in the database.
	 * 
	 * @param session The Stripe checkout session containing the purchase details.
	 * @return The upserted ObjectPurchase record, or null if the session is invalid.
	 */
	public static ObjectPurchase upsertCompletedPurchase(Session session) {
        if (session == null || session.getId() == null) return null;

        Price price = null;
        try {
            final var LINE_ITEMS = session.listLineItems(SessionListLineItemsParams.builder().setLimit(1L).build());
            if (LINE_ITEMS != null && !LINE_ITEMS.getData().isEmpty()) price = LINE_ITEMS.getData().get(0).getPrice();
        } catch (Exception ignored) {}

        final Customer CUSTOMER = session.getCustomerObject();
        final var DETAILS = session.getCustomerDetails();

        final String EMAIL = CUSTOMER != null && CUSTOMER.getEmail() != null && !CUSTOMER.getEmail().isBlank() ? CUSTOMER.getEmail() : (DETAILS != null ? DETAILS.getEmail() : null);
        final String NAME = CUSTOMER != null && CUSTOMER.getName() != null && !CUSTOMER.getName().isBlank() ? CUSTOMER.getName() : (DETAILS != null ? DETAILS.getName() : null);
        final String CUSTOMER_ID = CUSTOMER != null ? CUSTOMER.getId() : session.getCustomer();
        final StripePurchaseStatus STATUS = "paid".equalsIgnoreCase(session.getPaymentStatus()) ? StripePurchaseStatus.ACTIVE : StripePurchaseStatus.PENDING;
        final String TOKEN = session.getClientReferenceId() != null && !session.getClientReferenceId().isBlank() ? session.getClientReferenceId() : session.getId();
        final String GH_USERNAME = session.getMetadata() != null ? session.getMetadata().get("github_username") : null;

        return upsertCompletedPurchase(TOKEN, session.getId(), CUSTOMER_ID, EMAIL, NAME, STATUS, GH_USERNAME, StripeHelper.resolveProductId(price));
    }

	public static ObjectPurchase upsertCompletedPurchase(String purchaseToken, String checkoutSessionId, String stripeCustomerId, String customerEmail, String customerName, StripePurchaseStatus status, String githubUsername, String productId) {
		final String token = purchaseToken != null && !purchaseToken.isBlank() ? StripeHelper.normalizeToken(purchaseToken) : StripeHelper.normalizeToken(checkoutSessionId);
		if (token == null || token.isBlank()) return null;

		ObjectPurchase existing = getByPurchaseReference(token);
		if (existing == null && checkoutSessionId != null && !checkoutSessionId.isBlank()) existing = getByCheckoutSessionId(checkoutSessionId);

		if (existing == null) {
			final Date now = new Date();
			InsertionManager.insert(PhotonEngine.DATA_BASE, PurchaseTable.class, "purchase_token", "checkout_session_id", "stripe_customer_id", "customer_email", "customer_name", "product_id", "status", "github_username", "created_at", "updated_at")
				.row(token, checkoutSessionId, stripeCustomerId, StripeHelper.normalizeEmail(customerEmail), customerName, productId, status, githubUsername, now, now)
				.execute();
			return getByToken(token);
		}

		return completePurchase(token, checkoutSessionId, stripeCustomerId, customerEmail, customerName, status, githubUsername, productId);
	}

    private static ObjectPurchase completePurchase(String purchaseToken, String checkoutSessionId, String stripeCustomerId, String customerEmail, String customerName, StripePurchaseStatus status, String githubUsername, String productId) {
        final ObjectPurchase PURCHASE = createOrRetrievePendingPurchase(purchaseToken, checkoutSessionId, customerEmail, customerName, productId);
        if (PURCHASE == null) return null;

        UpdateManager.update(PhotonEngine.DATA_BASE, PurchaseTable.class)
            .set("checkout_session_id", checkoutSessionId)
            .set("stripe_customer_id", stripeCustomerId)
            .set("customer_email", StripeHelper.normalizeEmail(customerEmail != null && !customerEmail.isBlank() ? customerEmail : PURCHASE.customerEmail()))
            .set("customer_name", customerName != null && !customerName.isBlank() ? customerName : PURCHASE.customerName())
            .set("product_id", productId != null && !productId.isBlank() ? productId : PURCHASE.productId())
            .set("status", status == null ? PURCHASE.status() : status)
            .set("updated_at", new Date())
            .set("github_username", githubUsername != null && !githubUsername.isBlank() ? githubUsername : PURCHASE.githubUsername())
            .where(Expressions.isEqualTo("purchase_token", StripeHelper.normalizeToken(purchaseToken)))
            .execute();

        return getByToken(purchaseToken);
    }

	/**
	 * Retrieves a purchase record by its associated Stripe customer ID.
	 * This method queries the database for a purchase that matches the provided Stripe customer ID and returns the corresponding ObjectPurchase instance if found.
	 * 
	 * @param stripeCustomerId The Stripe customer ID associated with the purchase to retrieve.
	 * @return The ObjectPurchase record corresponding to the provided Stripe customer ID, or null if no matching record is found.
	 */
    public static ObjectPurchase getByCustomerId(String stripeCustomerId) {
        if (stripeCustomerId == null || stripeCustomerId.isBlank()) return null;
        return SelectionManager.select(PhotonEngine.DATA_BASE, PurchaseTable.class)
            .where(Expressions.isEqualTo("stripe_customer_id", stripeCustomerId.trim()))
            .limit(1)
            .executeSerializable(ObjectPurchase.class);
    }

    private static ObjectPurchase getByToken(String purchaseToken) {
        if (purchaseToken == null || purchaseToken.isBlank()) return null;
        return SelectionManager.select(PhotonEngine.DATA_BASE, PurchaseTable.class)
            .where(Expressions.isEqualTo("purchase_token", StripeHelper.normalizeToken(purchaseToken)))
            .limit(1)
            .executeSerializable(ObjectPurchase.class);
    }

	/**
	 * Retrieves a purchase record by its associated Stripe checkout session ID. This method queries the database for a purchase that matches the provided checkout session ID and returns the corresponding ObjectPurchase instance if found.
	 * 
	 * @param checkoutSessionId The Stripe checkout session ID associated with the purchase to retrieve.
	 * @return The ObjectPurchase record corresponding to the provided checkout session ID, or null if no matching record is found.
	 */
    public static ObjectPurchase getByCheckoutSessionId(String checkoutSessionId) {
        if (checkoutSessionId == null || checkoutSessionId.isBlank()) return null;
        return SelectionManager.select(PhotonEngine.DATA_BASE, PurchaseTable.class)
            .where(Expressions.isEqualTo("checkout_session_id", checkoutSessionId.trim()))
            .limit(1)
            .executeSerializable(ObjectPurchase.class);
    }

	/**
	 *  Retrieves a list of purchase records associated with a specific user account UUID. This method queries the database for all purchases linked to the provided account UUID and returns them as a list of ObjectPurchase instances.
	 * 
	 * @param accountUuid The UUID of the user account for which to retrieve purchase records. 
	 * @return A list of ObjectPurchase instances associated with the specified account UUID. If no purchases are found, an empty list is returned.
	 */
    public static List<ObjectPurchase> getByAccountUuid(String accountUuid) {
        if (accountUuid == null || accountUuid.isBlank()) return List.of();
        return SelectionManager.select(PhotonEngine.DATA_BASE, PurchaseTable.class)
            .where(Expressions.isEqualTo("linked_account_uuid", accountUuid))
            .executeList(ObjectPurchase.class);
    }

	/**
	 * Retrieves a purchase record by its purchase reference, which can be either the purchase token or the checkout session ID. This method first attempts to find the record by token, and if not found, it tries to find it by checkout session ID.
	 * 
	 * @param purchaseReference The purchase reference to search for, which can be either a purchase token or a checkout session ID.
	 * @return The ObjectPurchase record if found, or null if no matching record exists.
	 */
    public static ObjectPurchase getByPurchaseReference(String purchaseReference) {
        final ObjectPurchase byToken = getByToken(purchaseReference);
        if (byToken != null) return byToken;
        return getByCheckoutSessionId(purchaseReference);
    }

	/**
	 * Checks if a purchase token can be redeemed, meaning it exists and is not already linked to an account.
	 * 
	 * @param purchaseToken The purchase token to check for redeemability.
	 * @return True if the purchase token can be redeemed, false otherwise.
	 */
    public static boolean canRedeem(String purchaseToken) {
        final ObjectPurchase TOKEN = getByPurchaseReference(purchaseToken);
        return TOKEN != null && (TOKEN.linkedAccountUuid() == null || TOKEN.linkedAccountUuid().isBlank());
    }

	/**
	 * Marks a purchase as redeemed by linking it to a user account UUID and updating its status to active.
	 * This method updates the corresponding purchase record in the database with the provided account UUID, sets the redeemed timestamp, and changes the status to active.
	 * 
	 * @param purchaseToken The purchase token of the purchase to mark as redeemed.
	 * @param accountUuid The UUID of the user account to link to the redeemed purchase.
	 * @return True if the purchase was successfully marked as redeemed, false otherwise.
	 */
    public static boolean markAsRedeemed(String purchaseToken, String accountUuid) {
        if (purchaseToken == null || purchaseToken.isBlank() || accountUuid == null || accountUuid.isBlank()) return false;
        final ObjectPurchase PURCHASE = getByPurchaseReference(purchaseToken);
        if (PURCHASE == null || PURCHASE.purchaseToken() == null || PURCHASE.purchaseToken().isBlank()) return false;
        UpdateManager.update(PhotonEngine.DATA_BASE, PurchaseTable.class)
            .set("linked_account_uuid", accountUuid)
            .set("redeemed_at", new Date())
            .set("status", StripePurchaseStatus.ACTIVE)
            .set("updated_at", new Date())
            .where(Expressions.isEqualTo("purchase_token", PURCHASE.purchaseToken()))
            .execute();
        return true;
    }
}