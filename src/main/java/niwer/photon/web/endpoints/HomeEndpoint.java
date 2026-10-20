package niwer.photon.web.endpoints;

import io.javalin.http.Context;
import niwer.photon.web.HttpMethod;

public class HomeEndpoint implements IEndpoint {

    @Override public String path() { return "/"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        handler.redirect("https://niwer.dev/panel");
    }
}