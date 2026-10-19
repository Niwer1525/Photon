package niwer.photon.web.endpoints.accounts.licenses;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.objects.ObjectLicense;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.license.LicenseManager;
import niwer.photon.util.session.SessionManager;
import niwer.photon.util.stripe.EntitlementManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

public class AccountLicenseCreateEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/licenses"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.SECONDS);

        try {
            final var ACCOUNT = SessionManager.requireAccount(handler);
            if (ACCOUNT == null) return;
    
            final JsonObject BODY = EndpointUtils.readBody(handler);
            final String REQUESTED_PRODUCT_ID = GsonUtils.getString(BODY, "product_id", "productId", null);
            final var PRODUCTS = Directories.getConfig().getProducts();
            final var PRODUCT = PRODUCTS.stream()
                .filter(candidate -> Objects.equals(candidate.id(), REQUESTED_PRODUCT_ID) || (REQUESTED_PRODUCT_ID == null && candidate == PRODUCTS.get(0)))
                .findFirst()
                .orElse(null);
    
            if (PRODUCT == null || PRODUCT.id() == null || PRODUCT.id().isBlank()) {
                handler.status(400).result("Unknown license product");
                return;
            }
    
            final String PRODUCT_ID = PRODUCT.id();
            if (!ACCOUNT.isAdministrator() && !EntitlementManager.hasAccess(ACCOUNT.getUuid(), PRODUCT_ID)) {
                handler.status(403).result("Purchase or active subscription required for this product");
                return;
            }
            final String NAME = GsonUtils.getString(BODY, "name", "name", ACCOUNT.getUsername());
            final Long DURATION_DAYS = GsonUtils.getLong(BODY, "duration_days", "durationDays", PRODUCT.defaultLicenseDurationDays());
            final Long EXPIRES_AT = GsonUtils.getLong(BODY, "expires_at", "expiresAt", null);
    
            final Long COMPUTED_EXPIRES_AT = EXPIRES_AT != null ? EXPIRES_AT : (DURATION_DAYS == null || DURATION_DAYS <= 0L ? null : System.currentTimeMillis() + (DURATION_DAYS * 86400000L));
            final ObjectLicense LICENSE = LicenseManager.issueLicense(PRODUCT_ID, NAME, ACCOUNT.getEmail(), ACCOUNT.getUuid(), COMPUTED_EXPIRES_AT);
            if(LICENSE == null) {
                handler.status(500).result("Failed to create license");
                return;
            }
            
            handler.status(200).json(LICENSE.payload());
        } catch (Exception e) {
            IEndpoint.handleException(AccountLicenseCreateEndpoint.class, handler, e);
        }
    }
}