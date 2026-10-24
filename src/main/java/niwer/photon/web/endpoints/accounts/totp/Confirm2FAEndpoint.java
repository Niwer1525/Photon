package niwer.photon.web.endpoints.accounts.totp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.sql.RecoveryCodesTable;
import niwer.photon.util.PhotonLogTypes;
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

        try {
            final ObjectUserAccount ACCOUNT = SessionManager.requireAccount(ctx);
            if (ACCOUNT == null) return;
    
            String code = EndpointUtils.getString(ctx, EndpointUtils.parseBody(ctx.body()), "code");
            if (code == null || code.isBlank()) {
                ctx.status(400).result("Missing 2FA code");
                return;
            }
    
            final ObjectUserAccount FRESH_ACCOUNT = PlayerAccountTable.getAccountByUUID(ACCOUNT.getUuid()); // Fetch fresh copy of the account with the TOTP secret
            if (FRESH_ACCOUNT != null && TotpManager.verifyCode(FRESH_ACCOUNT.getTotpSecret(), code)) {
                PlayerAccountTable.setTotpEnabled(FRESH_ACCOUNT.getUuid(), true);
                final List<String> PLAIN_RECOVERY_CODES = RecoveryCodesTable.generateAndSaveCodes(FRESH_ACCOUNT.getUuid());

                ctx.status(200).json(Map.of("recovery_codes", PLAIN_RECOVERY_CODES)); // Send the plaintext recovery codes to the user (they should be shown only once)
                Console.log("2FA enabled for account %s (UUID: %s)", ACCOUNT.getUsername(), ACCOUNT.getUuid()).sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
            } else ctx.status(400).result("Invalid 2FA code");
        } catch (Exception e) {
            IEndpoint.handleException(Confirm2FAEndpoint.class, ctx, e);
        }
    }
}