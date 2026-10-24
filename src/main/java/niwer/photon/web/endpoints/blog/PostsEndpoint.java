package niwer.photon.web.endpoints.blog;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.internal.Lists;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.sql.BlogPostsTable;
import niwer.photon.util.TranslationManager.Language;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class PostsEndpoint implements IEndpoint {

    @Override public String path() { return "/blog/posts"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.MINUTES);

        final Language LANGUAGE = Language.fromString(handler.queryParam("lang"));
        List<ObjectBlogPost> posts = BlogPostsTable.getAllPublishedPosts(LANGUAGE);
        if(posts == null || posts.isEmpty()) posts = Lists.newArrayList();
        
        handler.status(HttpStatus.OK).json(posts);
    }
}