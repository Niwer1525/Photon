package niwer.photon.web.endpoints.accounts.licenses;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.Directories;
import niwer.photon.util.session.SessionManager;
import niwer.photon.util.stripe.EntitlementManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AccountLicenseProductsEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/license-products"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.SECONDS);
        
        try {
            final var ACCOUNT = SessionManager.requireAccount(handler);
            if (ACCOUNT == null) return;
            
            /* Check if the user has any access to the license system */
            if (!ACCOUNT.isAdministrator() && !EntitlementManager.hasAnyAccess(ACCOUNT.getUuid())) {
                handler.status(HttpStatus.FORBIDDEN).result("Purchase or active subscription required");
                return;
            }
            
            /* Get the products to which the user has access */
            final List<Map<String, Object>> PRODUCTS = Directories.config().getProducts().stream()
                .filter(product -> product.isLicenseRequired() && (ACCOUNT.isAdministrator() || EntitlementManager.hasAccess(ACCOUNT.getUuid(), product.id())))
                .map(product -> product.payload())
                .toList();
            handler.status(HttpStatus.OK).json(PRODUCTS);
        } catch (Exception e) {
            IEndpoint.handleException(AccountLicenseProductsEndpoint.class, handler, e);
        }
    }
}