package niwer.photon.web.endpoints.accounts.totp;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.PhotonLogTypes;
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

        try {
            final ObjectUserAccount account = SessionManager.requireAccount(handler);
            if (account == null) return;
    
            /* Check if 2FA is already enabled */
            if(account.isTotpEnabled() /* || (account.getTotpSecret() != null && !account.getTotpSecret().isBlank()) */) {
                handler.status(HttpStatus.BAD_REQUEST).result("2FA is already enabled for this account");
                return;
            }
    
            try {
                String rawSecret = TotpManager.generateRawSecret();
                String encryptedSecret = TotpManager.encrypt(rawSecret);
                String qrUri = TotpManager.generateQrDataUri(account.getEmail(), rawSecret);
                PlayerAccountTable.setTotpSecret(account.getUuid(), encryptedSecret); // Save secret to database (totpEnabled remains false until verified)
                handler.status(HttpStatus.OK).json(Map.of(
                    "qrCode", qrUri,
                    "manualKey", rawSecret
                ));
                Console.log("2FA setup initiated for account %s (UUID: %s)", account.getUsername(), account.getUuid()).sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
            } catch (Exception e) {
                Console.log("Error while setting up 2FA for account %s: %s", account.getUsername(), e.getMessage()).error().sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
                handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to initiate 2FA setup");
            }
        } catch (Exception e) {
            IEndpoint.handleException(Setup2FAEndpoint.class, handler, e);
        }
    }
}