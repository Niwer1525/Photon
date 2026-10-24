package niwer.photon.objects;

import java.time.LocalDateTime;

import niwer.photon.sql.BlogPostsTable.BlogPostStatus;
import niwer.photon.util.TranslationManager.Language;
import niwer.queryon.SQLSerializable;
import niwer.queryon.tables.api.IColumnField;
import niwer.queryon.tables.api.IDefaultValue;

public class ObjectBlogPost extends SQLSerializable<ObjectBlogPost> {

    private static final int AVERAGE_READING_SPEED_WPM = 200; // Average reading speed in words per minute

    @IColumnField(name = "id", primaryKey = true, autoIncrement = true)
    private Integer id;

    @IColumnField(name = "slug", notNull = true, charLimit = 255)
    private String slug;

    @IColumnField(name = "lang", defaultValue = @IDefaultValue(value = "en"))
    private String lang = Language.ENGLISH.code();

    @IColumnField(name = "title", notNull = true, charLimit = 255)
    private String title;

    @IColumnField(name = "summary", notNull = true, charLimit = 1024)
    private String summary;

    @IColumnField(name = "content_markdown", notNull = true, charLimit = 65535)
    private String contentMarkdown;

    @IColumnField(name = "cover_image")
    private String coverImage; // Base64-encoded image data or URL to the cover image

    @IColumnField(name = "status")
    private BlogPostStatus status = BlogPostStatus.DRAFT;

    @IColumnField(name = "published_at")
    private LocalDateTime publishedAt;

    @IColumnField(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @IColumnField(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getSlug() { return slug; }

    public String getLang() { return lang; }

    public String getTitle() { return title; }

    public String getSummary() { return summary; }

    public String getContentMarkdown() { return contentMarkdown; }

    public String getCoverImage() { return coverImage; }

    public BlogPostStatus getStatus() { return status; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    
    /**
     * Calculates the estimated reading time for the blog post based on its content.
     * @return The estimated reading time in minutes. Returns 1 minute if the content is empty or null.
     */
    public int getReadingTimeMinutes() {
        if (contentMarkdown == null || contentMarkdown.isBlank()) return 1;

        final int WORD_COUNT = contentMarkdown.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(WORD_COUNT / (double) AVERAGE_READING_SPEED_WPM));
    }
}
