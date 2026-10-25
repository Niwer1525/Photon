package niwer.photon.web.endpoints.admin;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.sql.BlogPostsTable;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AdminPostEndpoint implements IEndpoint {

    @Override public String path() { return "/admin/blog/posts/{uuid}"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.MINUTES);

        try {
            if (SessionManager.requireAdministrator(handler) == null) return;
            
            int uuid;
            try {
                uuid = Integer.parseInt(handler.pathParam("uuid"));
            } catch (NumberFormatException e) {
                handler.status(HttpStatus.BAD_REQUEST).result("Missing uuid parameter");
                return;
            }
    
            final ObjectBlogPost POST = BlogPostsTable.getPostById(uuid);
            handler.status(HttpStatus.OK).json(POST.payload());
        } catch (Exception e) {
            IEndpoint.handleException(AdminDeletePostEndpoint.class, handler, e);
        }
    }
}