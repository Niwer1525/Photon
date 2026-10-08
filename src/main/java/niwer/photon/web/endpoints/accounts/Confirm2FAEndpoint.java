package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.TotpManager;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * Endpoint for confirming Two-Factor Authentication (2FA) setup for a user account.
 * This endpoint verifies the TOTP code provided by the user against the stored encrypted secret and enables 2FA if the code is valid.
 * 
 * @author Niwer
 */
public class Confirm2FAEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/2fa/confirm"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context ctx) {
        IEndpoint.setupRateLimit(ctx, 5, TimeUnit.MINUTES);

        final ObjectUserAccount account = SessionManager.requireAccount(ctx);
        if (account == null) return;

        String code = EndpointUtils.getString(ctx, EndpointUtils.parseBody(ctx.body()), "code");
        if (code == null || code.isBlank()) {
            ctx.status(400).result("Missing 2FA code");
            return;
        }

        ObjectUserAccount freshAccount = PlayerAccountTable.getAccountByUUID(account.getUuid()); // Fetch fresh copy of the account with the TOTP secret

        if (freshAccount != null && TotpManager.verifyCode(freshAccount.getTotpSecret(), code)) {
            PlayerAccountTable.setTotpEnabled(freshAccount.getUuid(), true);
            ctx.status(200).result("2FA enabled successfully");
        } else ctx.status(400).result("Invalid 2FA code");
    }
}