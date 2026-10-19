package niwer.photon.web.endpoints.accounts.licenses;

import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import io.javalin.http.Context;
import niwer.photon.objects.ObjectLicense;
import niwer.photon.sql.LicenseTable;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.session.SessionManager;
import niwer.photon.util.stripe.EntitlementManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

public class AccountLicenseRevokeEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/licenses/revoke"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.SECONDS);

        try {
            final var ACCOUNT = SessionManager.requireAccount(handler);
            if (ACCOUNT == null) return;
            
            /* Check if the user has any access to the license system */
            if (!ACCOUNT.isAdministrator() && !EntitlementManager.hasAnyAccess(ACCOUNT.getUuid())) {
                handler.status(403).result("Purchase or active subscription required");
                return;
            }
    
            final JsonObject BODY = EndpointUtils.readBody(handler);
            final String LICENSE_KEY = GsonUtils.getString(BODY, "license_key", "licenseKey", null);
            if (LICENSE_KEY == null || LICENSE_KEY.isBlank()) {
                handler.status(400).result("Missing license key");
                return;
            }
    
            /* Get the license, if it does not exist, return an error */
            final ObjectLicense LICENSE = LicenseTable.getByKey(LICENSE_KEY);
            if (LICENSE == null) {
                handler.status(404).result("License not found");
                return;
            }
    
            /* Check if the user has access to the product */
            if (!ACCOUNT.isAdministrator() && !EntitlementManager.hasAccess(ACCOUNT.getUuid(), LICENSE.productId())) {
                handler.status(403).result("You do not have access to this product");
                return;
            }
    
            /* Check if the user is the creator of the license */
            if (!ACCOUNT.isAdministrator() && (LICENSE.creatorUuid() == null || !LICENSE.creatorUuid().equalsIgnoreCase(ACCOUNT.getUuid()))) {
                handler.status(403).result("You can only revoke your own licenses");
                return;
            }
    
            /* Revoke the license */
            if (!LicenseTable.revoke(LICENSE_KEY)) {
                handler.status(500).result("Failed to revoke license");
                return;
            }
    
            final ObjectLicense UPDATED_LICENSE = LicenseTable.getByKey(LICENSE_KEY);
            handler.json(UPDATED_LICENSE == null ? LICENSE.payload() : UPDATED_LICENSE.payload());
        } catch (Exception e) {
            IEndpoint.handleException(AccountLicenseRevokeEndpoint.class, handler, e);
        }
    }
}