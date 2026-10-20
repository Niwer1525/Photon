package niwer.photon.web.endpoints.accounts;

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

public class Disable2FAEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/2fa/disable"; }
    
    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        try {
            final ObjectUserAccount ACCOUNT = SessionManager.requireAccount(handler);
            if (ACCOUNT == null) return;
            
            /* Fetch the most up-to-date account information */
            final ObjectUserAccount FRESH_ACCOUNT = PlayerAccountTable.getAccountByUUID(ACCOUNT.getUuid());
            if (FRESH_ACCOUNT == null || !FRESH_ACCOUNT.isTotpEnabled()) {
                handler.status(400).result("2FA is not enabled");
                return;
            }
            
            /* Verify the provided verification code */
            final String CODE = EndpointUtils.getString(handler, EndpointUtils.parseBody(handler.body()), "code");
            if (CODE == null || CODE.isBlank()) {
                handler.status(400).result("Verification code is required to disable 2FA");
                return;
            }
            if (!TotpManager.verifyCodeWithRecovery(FRESH_ACCOUNT.getTotpSecret(), CODE, FRESH_ACCOUNT.getUuid())) {
                handler.status(401).result("Invalid verification code");
                return;
            }
    
            /* Disable 2FA and remove secret */
            PlayerAccountTable.setTotpEnabled(FRESH_ACCOUNT.getUuid(), false);
            PlayerAccountTable.setTotpSecret(FRESH_ACCOUNT.getUuid(), null);
            RecoveryCodesTable.deleteAllCodesForUser(FRESH_ACCOUNT.getUuid()); // Remove all recovery codes for the user since 2FA is being disabled
    
            handler.status(200).result("2FA disabled successfully");
            Console.log("2FA disabled for account %s (UUID: %s)", FRESH_ACCOUNT.getUsername(), FRESH_ACCOUNT.getUuid()).sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
        } catch (Exception e) {
            IEndpoint.handleException(Disable2FAEndpoint.class, handler, e);
        }
    }
}