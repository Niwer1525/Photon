package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.HashUtils;
import niwer.photon.util.TotpManager;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

public class DeleteAccountEndpoint implements IEndpoint {

    private static final String CONFIRMATION_PHRASE = "DELETE MY ACCOUNT";

    @Override public String path() { return "/accounts/delete_account"; }

    @Override public HttpMethod method() { return HttpMethod.DELETE; }

    @Override
    public void handle(Context handler) {
        /* Rate limit against brute-forcing password/TOTP */
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        /* Require authenticated session */
        final ObjectUserAccount sessionAccount = SessionManager.requireAccount(handler);
        if (sessionAccount == null) return; // Status 401 already set by requireAccount

        final var body = EndpointUtils.parseBody(handler.body());
        final String password = EndpointUtils.getString(handler, body, "password");
        final String confirmationPhrase = EndpointUtils.getString(handler, body, "confirmationPhrase");
        final String code = EndpointUtils.getString(handler, body, "code");

        /* Validate strict confirmation phrase */
        if (confirmationPhrase == null || !CONFIRMATION_PHRASE.equals(confirmationPhrase.trim())) {
            handler.status(400).result("Invalid confirmation phrase. Must be: " + CONFIRMATION_PHRASE);
            return;
        }

        /* Validate password input */
        if (password == null || password.isBlank()) {
            handler.status(400).result("Password is required");
            return;
        }

        /* Fetch fresh state from the database */
        final ObjectUserAccount account = PlayerAccountTable.getAccountByUUID(sessionAccount.getUuid());
        if (account == null || account.password() == null || !HashUtils.passwordMatches(account.password(), password)) {
            handler.status(401).result("Invalid password");
            return;
        }

        /* Enforce 2FA check if enabled */
        if (account.isTotpEnabled()) {
            if (code == null || code.isBlank()) {
                handler.status(400).result("Two-factor authentication code is required");
                return;
            }

            if (!TotpManager.verifyCode(account.getTotpSecret(), code.trim())) {
                handler.status(401).result("Invalid 2FA code");
                return;
            }
        }

        /* Soft delete flag */
        if (!PlayerAccountTable.markPendingDeletion(account.getUuid())) {
            handler.status(500).result("Failed to initiate account deletion");
            return;
        }

        /* Invalidate all active user & admin sessions */
        SessionManager.revokeAllSessions(account.getUuid());

        /* Clear auth and admin cookies */
        handler.res().addHeader("Set-Cookie", "photon_admin=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict");
        handler.res().addHeader("Set-Cookie", "photon_csrf=; Path=/; Max-Age=0; SameSite=Strict");

        handler.status(200).result("Account scheduled for deletion. All sessions revoked.");
    }
}