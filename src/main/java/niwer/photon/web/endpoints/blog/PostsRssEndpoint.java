package niwer.photon.web.endpoints.blog;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.sql.BlogPostsTable;
import niwer.photon.util.RssFeedGenerator;
import niwer.photon.util.TranslationManager.Language;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class PostsRssEndpoint implements IEndpoint {

    @Override public String path() { return "/blog/posts/rss"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.MINUTES);

        final Language LANGUAGE = Language.fromString(handler.queryParam("lang"));
        final List<ObjectBlogPost> POSTS = BlogPostsTable.getAllPublishedPosts(LANGUAGE);
        final String RSS_XML = RssFeedGenerator.buildRssXml(POSTS, LANGUAGE);
        if(RSS_XML == null || RSS_XML.isBlank()) {
            handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to generate RSS feed");
            return;
        }
        handler
            .status(HttpStatus.OK)
            .contentType("application/rss+xml; charset=utf-8")
            .result(RSS_XML);
    }
}