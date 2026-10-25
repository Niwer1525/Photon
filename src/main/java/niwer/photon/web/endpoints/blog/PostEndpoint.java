package niwer.photon.web.endpoints.blog;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.sql.BlogPostsTable;
import niwer.photon.util.TranslationManager.Language;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class PostEndpoint implements IEndpoint {

    @Override public String path() { return "/blog/posts/{slug}"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.MINUTES);

        final String SLUG = handler.pathParam("slug");
        if(SLUG == null || SLUG.isBlank()) {
            handler.status(HttpStatus.BAD_REQUEST).result("Missing slug parameter");
            return;
        }

        /* Get the post by it's slug */
        final Language LANGUAGE = Language.fromString(handler.queryParam("lang"));
        final ObjectBlogPost POST = BlogPostsTable.getPublishedPostBySlug(SLUG, LANGUAGE);
        if(POST == null) {
            handler.status(HttpStatus.NOT_FOUND).result("Blog post not found");
            return;
        }
        
        handler.status(HttpStatus.OK).json(POST.payload());
    }
}