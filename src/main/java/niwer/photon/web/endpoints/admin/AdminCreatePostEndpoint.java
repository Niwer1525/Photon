package niwer.photon.web.endpoints.admin;

import io.javalin.http.Context;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AdminCreatePostEndpoint implements IEndpoint {

    @Override public String path() { return "/blog/posts/create"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override public void handle(Context handler) {
        
    }
}