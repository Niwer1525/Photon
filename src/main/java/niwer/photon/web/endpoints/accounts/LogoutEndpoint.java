package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.util.session.SessionManager;
import niwer.photon.util.session.SessionScope;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class LogoutEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/logout"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        /* Try to login as admin first */
        for(final SessionScope SCOPE : SessionScope.values()) {
            final String TOKEN = SCOPE.extractToken(handler);
            if(TOKEN == null || TOKEN.isBlank()) continue;

            SessionManager.logout(TOKEN, SCOPE);
        }

        handler.res().addHeader("Set-Cookie", "photon_admin=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict");
        handler.res().addHeader("Set-Cookie", "photon_csrf=; Path=/; Max-Age=0; SameSite=Strict");
        handler.status(200);
    }
}