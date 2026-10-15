package niwer.photon.web.endpoints.accounts;

import io.javalin.http.Context;
import niwer.photon.util.session.Session;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

public class RefreshSessionEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/refresh"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        final var body = EndpointUtils.parseBody(handler.body());
        final String refreshToken = EndpointUtils.getString(handler, body, "refreshToken");
        final Session session = SessionManager.refreshSession(refreshToken);
        if (session == null) {
            handler.status(401).result("Invalid or expired refresh token");
            return;
        }

        if (session.account().isAdministrator()) {
            AuthAccountEndpoint.setupAdminCookies(handler, session);
            handler.json(new AuthAccountEndpoint.LoginResponse(null, session.refreshToken(), session.account().payload(), true));
        } else {
            handler.json(new AuthAccountEndpoint.LoginResponse(session.token(), session.refreshToken(), session.account().payload(), false));
        }
    }
}
