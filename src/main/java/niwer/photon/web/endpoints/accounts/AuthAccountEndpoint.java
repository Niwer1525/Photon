package niwer.photon.web.endpoints.accounts;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.HashUtils;
import niwer.photon.util.session.Session;
import niwer.photon.util.session.SessionManager;
import niwer.photon.util.session.SessionScope;
import niwer.photon.util.stripe.EntitlementManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

public class AuthAccountEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/auth_account"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        final Credentials CREDENTIALS = readCredentials(handler);
        if (CREDENTIALS == null || CREDENTIALS.email == null || CREDENTIALS.password == null || CREDENTIALS.email.isBlank() || CREDENTIALS.password.isBlank()) {
            handler.status(400).result("Missing or blank parameters");
            return;
        }

        /* Fetch user and verify password */
        final ObjectUserAccount account = PlayerAccountTable.getAccountByEmail(CREDENTIALS.email);
        if (account == null || account.password() == null || !HashUtils.passwordMatches(account.password(), CREDENTIALS.password)) {
            handler.status(401).result("Invalid credentials or access denied");
            return;
        }

        /* Prevent login if account is pending deletion */
        if (PlayerAccountTable.isPendingDeletion(account.getUuid())) {
            handler.status(403).result("This account has been scheduled for deletion and cannot be accessed.");
            return;
        }

        final boolean isAdmin = account.isAdministrator();

        /* Enforce 2FA If enabled (or if user is admin), stop here and return challenge ticket */
        if (account.isTotpEnabled() /* || isAdmin */) { //TODO: Maybe useful in the future to force admins to have 2FA enabled, but for now we allow it
            // if (isAdmin && !account.isTotpEnabled()) {
            //     handler.status(403).result("Administrator accounts must configure 2FA."); //TODO: Maybe useful in the future to force admins to have 2FA enabled, but for now we allow it
            //     return;
            // }

            SessionScope scope = isAdmin ? SessionScope.ADMIN : SessionScope.USER;
            String ticket = SessionManager.createPending2FA(account.getUuid(), scope, CREDENTIALS.token);

            // Tell the frontend to switch to the 6-digit TOTP input
            handler.json(Map.of(
                "status", "2FA_REQUIRED",
                "ticket", ticket
            ));
            return;
        }

        /* Normal login (only reached if 2FA is not enabled) */
        if (CREDENTIALS.token != null && !CREDENTIALS.token.isBlank()) {
            if (!EntitlementManager.redeemPurchase(CREDENTIALS.token, account)) {
                handler.status(403).result("Invalid or expired purchase token");
                return;
            }
        }

        final Session USER_AUTH = SessionManager.createSession(account, SessionScope.USER);
        if (USER_AUTH == null) {
            handler.status(401).result("Failed to create session");
            return;
        }

        handler.json(new LoginResponse(USER_AUTH.token(), account.payload(), false));
    }

    public static void setupAdminCookies(Context handler, Session session) {
        try {
            final String ADMIN_COOKIE = "photon_admin=" + session.token() + "; HttpOnly; Path=/; Max-Age=3600; SameSite=Strict";
            handler.res().addHeader("Set-Cookie", ADMIN_COOKIE);

            final String csrf = SessionManager.getCsrfForToken(session.token());
            if (csrf != null && !csrf.isBlank()) {
                final String csrfCookie = "photon_csrf=" + csrf + "; Path=/; Max-Age=3600; SameSite=Strict";
                handler.res().addHeader("Set-Cookie", csrfCookie);
            }
        } catch (Exception ignored) {}
    }

    private static Credentials readCredentials(Context handler) {
        String email = EndpointUtils.firstNonBlank(handler.formParam("email"), handler.queryParam("email"));
        String password = EndpointUtils.firstNonBlank(handler.formParam("password"), handler.queryParam("password"));
        String token = EndpointUtils.firstNonBlank(
            handler.formParam("checkoutSessionId"),
            handler.formParam("token"),
            handler.queryParam("checkoutSessionId"),
            handler.queryParam("token")
        );

        if (email != null && password != null) return new Credentials(email, password, token);

        try {
            Credentials jsonCreds = GsonUtils.GSON.fromJson(handler.body(), Credentials.class);
            if (jsonCreds != null) return jsonCreds;
        } catch (Exception ignored) {}

        return null;
    }

    private record Credentials(String email, String password, String token) {}
    public record LoginResponse(String token, Object account, boolean isAdmin) {}
}