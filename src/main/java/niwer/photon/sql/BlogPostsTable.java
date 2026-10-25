package niwer.photon.sql;

import java.time.LocalDateTime;
import java.util.List;

import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.util.TranslationManager.Language;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.SelectionManager.EnumOrder;
import niwer.queryon.queries.interaction.UpdateManager;
import niwer.queryon.tables.Table;

public class BlogPostsTable extends Table {

    public static enum BlogPostStatus {
        DRAFT,
        SCHEDULED,
        PUBLISHED,
        DELETED
    }

    public BlogPostsTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectBlogPost.class).execute();
    }

    @Override
    public String name() {
        return "blog_posts";
    }

    public static void checkScheduledPosts() {
        UpdateManager.update(PhotonEngine.DATA_BASE, BlogPostsTable.class)
            .set("status", BlogPostStatus.PUBLISHED)
            .where(Expressions.and(
                Expressions.isEqualTo("status", BlogPostStatus.SCHEDULED),
                Expressions.isLessThanOrEqualTo("published_at", LocalDateTime.now())
            ))
            .execute();
    }

    public static List<ObjectBlogPost> getAllPosts() {
        return SelectionManager.select(PhotonEngine.DATA_BASE, BlogPostsTable.class)
                .orderBy("created_at", EnumOrder.DESC)
                .executeList(ObjectBlogPost.class);
    }

    public static ObjectBlogPost getPostById(int id) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, BlogPostsTable.class)
                .where(Expressions.isEqualTo("id", id))
                .executeSerializable(ObjectBlogPost.class);
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

    /**
     * Deletes a blog post from the database based on its unique identifier (ID).
     * 
     * @param id The unique identifier of the blog post to be deleted.
     */
    public static void deletePostById(int id) {
        UpdateManager.update(PhotonEngine.DATA_BASE, BlogPostsTable.class)
            .set("status", BlogPostStatus.DELETED)
            .where(Expressions.isEqualTo("id", id))
            .execute();
    }

    /**
     * Creates a new blog post in the database based on the provided ObjectBlogPost instance.
     * 
     * @param slug The slug (URL-friendly identifier) for the new blog post.
     * @param language The language of the new blog post.
     * @param status The status of the new blog post (e.g., DRAFT, SCHEDULED, PUBLISHED).
     * @param published_at The date and time when the blog post is scheduled to be published (can be null for drafts).
     * @param title The title of the new blog post.
     * @param summary A brief summary or excerpt of the new blog post.
     * @param content The main content of the new blog post in Markdown format.
     */
    public static void createPost(String slug, Language language, BlogPostStatus status, LocalDateTime published_at, String title, String summary, String content) {
        InsertionManager.insert(PhotonEngine.DATA_BASE, BlogPostsTable.class, "slug", "lang", "title", "summary", "content_markdown", "status", "published_at")
            .row(slug.toLowerCase().replace(" ", "-"), language.code(), title, summary, content, status, published_at)
            .onConflictDoUpdate(
                UpdateManager.update(PhotonEngine.DATA_BASE, BlogPostsTable.class)
                    .set("title", title)
                    .set("summary", summary)
                    .set("content_markdown", content)
                    .set("status", status)
                    .set("published_at", published_at)
                    .where(Expressions.and(
                        Expressions.isEqualTo("slug", slug),
                        Expressions.isEqualTo("lang", language.code())
                    ))
            )
            .execute();
    }

    /**
     * Updates an existing blog post in the database based on the provided ObjectBlogPost instance.
     * 
     * @param id The unique identifier of the blog post to be updated.
     * @param slug The new slug (URL-friendly identifier) for the blog post.
     * @param status The new status of the blog post (e.g., DRAFT, SCHEDULED, PUBLISHED).
     * @param published_at The new date and time when the blog post is scheduled to be published (can be null for drafts).
     * @param title The new title of the blog post.
     * @param summary A new brief summary or excerpt of the blog post.
     * @param content The new main content of the blog post in Markdown format.
     */
    public static void updatePost(int id, String slug, BlogPostStatus status, LocalDateTime published_at, String title, String summary, String content) {
        UpdateManager.update(PhotonEngine.DATA_BASE, BlogPostsTable.class)
            .set("slug", slug)
            .set("status", status)
            .set("published_at", published_at)
            .set("title", title)
            .set("summary", summary)
            .set("content_markdown", content)
            .where(Expressions.isEqualTo("id", id))
            .execute();
    }
}