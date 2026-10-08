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

public class Disable2FAEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/2fa/disable"; }
    
    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        final ObjectUserAccount account = SessionManager.requireAccount(handler);
        if (account == null) return;

        ObjectUserAccount freshAccount = PlayerAccountTable.getAccountByUUID(account.getUuid());
        if (freshAccount == null || !freshAccount.isTotpEnabled()) {
            handler.status(400).result("2FA is not enabled");
            return;
        }

        String code = EndpointUtils.getString(handler, EndpointUtils.parseBody(handler.body()), "code");
        if (code == null || code.isBlank()) {
            handler.status(400).result("Verification code is required to disable 2FA");
            return;
        }

        if (!TotpManager.verifyCode(freshAccount.getTotpSecret(), code)) {
            handler.status(401).result("Invalid verification code");
            return;
        }

        // Disable 2FA and remove secret[cite: 2]
        PlayerAccountTable.setTotpEnabled(freshAccount.getUuid(), false);
        PlayerAccountTable.setTotpSecret(freshAccount.getUuid(), null);

        handler.status(200).result("2FA disabled successfully");
    }
}