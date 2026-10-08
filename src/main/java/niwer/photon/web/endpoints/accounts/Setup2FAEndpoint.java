package niwer.photon.web.endpoints.accounts;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.TotpManager;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * Endpoint for setting up Two-Factor Authentication (2FA) for a user account.
 * This endpoint generates a TOTP secret, encrypts it, and provides a QR code URI and manual key for the user to configure their authenticator app.
 * 
 * @author Niwer
 */
public class Setup2FAEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/2fa/setup"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.MINUTES);

        final ObjectUserAccount account = SessionManager.requireAccount(handler);
        if (account == null) return;

        /* Check if 2FA is already enabled */
        if(account.isTotpEnabled() /* || (account.getTotpSecret() != null && !account.getTotpSecret().isBlank()) */) {
            handler.status(400).result("2FA is already enabled for this account");
            return;
        }

        try {
            String rawSecret = TotpManager.generateRawSecret();
            String encryptedSecret = TotpManager.encrypt(rawSecret);
            String qrUri = TotpManager.generateQrDataUri(account.getEmail(), rawSecret);

            PlayerAccountTable.setTotpSecret(account.getUuid(), encryptedSecret); // Save secret to database (totpEnabled remains false until verified)

            handler.json(Map.of(
                "qrCode", qrUri,
                "manualKey", rawSecret
            ));
        } catch (Exception e) {
            handler.status(500).result("Failed to initiate 2FA setup");
        }
    }
}