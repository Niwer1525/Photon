package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class MeEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/me"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.MINUTES);

        try {
            final var ACCOUNT = SessionManager.requireAccount(handler);
            if (ACCOUNT == null) return;
    
            handler.json(ACCOUNT.payload());
        } catch (Exception e) {
            IEndpoint.handleException(MeEndpoint.class, handler, e);
        }
    }
}