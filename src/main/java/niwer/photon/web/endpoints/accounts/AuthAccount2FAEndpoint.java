package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.util.session.Session;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

public class AuthAccount2FAEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/auth_account/2fa"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        final var body = EndpointUtils.parseBody(handler.body());
        final String ticket = EndpointUtils.getString(handler, body, "ticket");
        final String code = EndpointUtils.getString(handler, body, "code");

        if (ticket == null || ticket.isBlank() || code == null || code.isBlank()) {
            handler.status(400).result("Missing ticket or 2FA code");
            return;
        }

        // Validate ticket and 6-digit TOTP code
        final Session session = SessionManager.complete2FA(ticket, code);
        if (session == null) {
            handler.status(401).result("Invalid 2FA code or expired attempt");
            return;
        }

        final boolean isAdmin = session.account().isAdministrator();

        if (isAdmin) {
            AuthAccountEndpoint.setupAdminCookies(handler, session);
            handler.json(new AuthAccountEndpoint.LoginResponse(null, session.account().payload(), true));
            return;
        }

        handler.json(new AuthAccountEndpoint.LoginResponse(session.token(), session.account().payload(), false));
    }
}