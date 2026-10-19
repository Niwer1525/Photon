package niwer.photon.web.endpoints.stripe;

import java.util.concurrent.TimeUnit;

import com.stripe.Stripe;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionRetrieveParams;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.objects.ObjectPurchase;
import niwer.photon.sql.PurchaseTable;
import niwer.photon.util.stripe.StripeHelper;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

public class StripePurchaseSessionEndpoint implements IEndpoint {

    @Override public String path() { return "/stripe/purchase_session"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.SECONDS);

        try {
            final String SESSION_ID = EndpointUtils.firstNonBlank(
                handler.formParam("checkoutSessionId"),
                handler.formParam("token"),
                handler.queryParam("checkoutSessionId"),
                handler.queryParam("token")
            );
            if (SESSION_ID == null || SESSION_ID.isBlank()) {
                handler.status(400).result("Missing checkout session id");
                return;
            }
    
            final String API_KEY = Directories.getConfig().stripe_api_key;
            if (API_KEY == null || API_KEY.isBlank()) {
                handler.status(500).result("stripe_api_key is not configured");
                return;
            }
            Stripe.apiKey = API_KEY;
    
            try {
                final SessionRetrieveParams PARAMS = SessionRetrieveParams.builder().addExpand("line_items").addExpand("customer").build();
                final Session SESSION = Session.retrieve(SESSION_ID.trim(), PARAMS, null);
    
                final Price PRICE = (SESSION.getLineItems() != null && !SESSION.getLineItems().getData().isEmpty()) ? SESSION.getLineItems().getData().get(0).getPrice() : null;
                final String PRODUCT_ID = StripeHelper.resolveProductId(PRICE);
    
                final String EMAIL = StripeHelper.resolveEmail(SESSION.getCustomerObject(), SESSION.getCustomerDetails());
                final String NAME = StripeHelper.resolveName(SESSION.getCustomerObject(), SESSION.getCustomerDetails());
                final String TOKEN = SESSION.getClientReferenceId() != null && !SESSION.getClientReferenceId().isBlank() ? SESSION.getClientReferenceId() : SESSION.getId();
    
                /* Create or retrieve a pending purchase */
                final ObjectPurchase PURCHASE = PurchaseTable.createOrRetrievePendingPurchase(TOKEN, SESSION.getId(), EMAIL, NAME, PRODUCT_ID);
                if (PURCHASE == null) {
                    handler.status(500).result("Failed to seed purchase session");
                    return;
                }
                handler.json(PURCHASE.payload());
            } catch (Exception e) {
                handler.status(502).result("Failed to resolve checkout session: " + e.getMessage());
            }
        } catch (Exception e) {
            IEndpoint.handleException(StripePurchaseSessionEndpoint.class, handler, e);
        }
    }
}