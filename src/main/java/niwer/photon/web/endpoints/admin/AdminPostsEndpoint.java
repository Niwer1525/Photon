package niwer.photon.web.endpoints.admin;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.internal.Lists;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.objects.IPayloadProvider;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.sql.BlogPostsTable;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AdminPostsEndpoint implements IEndpoint {

    @Override public String path() { return "/admin/blog/posts"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);
    
        try {
            if (SessionManager.requireAdministrator(handler) == null) return;

            List<ObjectBlogPost> posts = BlogPostsTable.getAllPosts();
            if(posts == null || posts.isEmpty()) posts = Lists.newArrayList();

            handler.status(HttpStatus.OK).json(IPayloadProvider.payloadCollection(posts));
        } catch (Exception e) {
            IEndpoint.handleException(AdminPostsEndpoint.class, handler, e);
        }
    }
}