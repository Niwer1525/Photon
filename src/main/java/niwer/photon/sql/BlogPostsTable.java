package niwer.photon.sql;

import java.time.LocalDateTime;
import java.util.List;

import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.util.TranslationManager.Language;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.SelectionManager.EnumOrder;
import niwer.queryon.tables.Table;

public class BlogPostsTable extends Table {

    public static enum BlogPostStatus {
        DRAFT,
        SCHEDULED,
        PUBLISHED
    }

    public BlogPostsTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectBlogPost.class).execute();
    }

    @Override
    public String name() {
        return "blog_posts";
    }

    /**
     * Retrieves all published blog posts in the specified language, ordered by creation date in descending order.
     * 
     * @param lang The language in which to retrieve the published blog posts.
     * @return A list of ObjectBlogPost instances representing the published blog posts in the specified language.
     */
    public static List<ObjectBlogPost> getAllPublishedPosts(Language lang) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, BlogPostsTable.class)
                .where(
                    Expressions.and(
                        Expressions.isEqualTo("status", BlogPostStatus.PUBLISHED),
                        Expressions.and(
                            Expressions.isLessThanOrEqualTo("published_at", LocalDateTime.now()),
                            Expressions.isEqualTo("language", lang.code())
                        )
                    )
                )
                .orderBy("created_at", EnumOrder.DESC)
                .executeList(ObjectBlogPost.class);
    }

    /**
     * Retrieves a published blog post by its slug and language.
     * 
     * @param slug The slug of the blog post to retrieve.
     * @param lang The language in which to retrieve the blog post.
     * @return An ObjectBlogPost instance representing the published blog post with the specified slug and language, or null if no such post exists.
     */
    public static ObjectBlogPost getPublishedPostBySlug(String slug, Language lang) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, BlogPostsTable.class)
                .where(
                    Expressions.and(
                        Expressions.isEqualTo("status", BlogPostStatus.PUBLISHED),
                        Expressions.and(
                            Expressions.isLessThanOrEqualTo("published_at", LocalDateTime.now()),
                            Expressions.isEqualTo("language", lang.code())
                        ),
                        Expressions.isEqualTo("slug", slug)
                    )
                )
                .executeSerializable(ObjectBlogPost.class);
    }
}
