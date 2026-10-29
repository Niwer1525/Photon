package niwer.photon.objects;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import niwer.photon.sql.BlogPostsTable.BlogPostStatus;
import niwer.photon.util.TranslationManager.Language;
import niwer.queryon.SQLSerializable;
import niwer.queryon.tables.api.IColumnField;
import niwer.queryon.tables.api.IDefaultValue;

public class ObjectBlogPost extends SQLSerializable<ObjectBlogPost> implements IPayloadProvider {

    private static transient final int AVERAGE_READING_SPEED_WPM = 200; // Average reading speed in words per minute

    @IColumnField(name = "id", primaryKey = true, autoIncrement = true)
    private Integer id;

    @SerializedName("slug")
    @IColumnField(name = "slug", notNull = true, charLimit = 255)
    private String slug;

    @SerializedName("lang")
    @IColumnField(name = "lang", defaultValue = @IDefaultValue(value = "en"))
    private String lang = Language.ENGLISH.code();

    @SerializedName("title")
    @IColumnField(name = "title", notNull = true, charLimit = 255)
    private String title;

    @SerializedName("summary")
    @IColumnField(name = "summary", notNull = true, charLimit = 1024)
    private String summary;

    @SerializedName("content_markdown")
    @IColumnField(name = "content_markdown", notNull = true, charLimit = 65535)
    private String contentMarkdown;

    @SerializedName("cover_image")
    @IColumnField(name = "cover_image")
    private String coverImage; // Base64-encoded image data or URL to the cover image

    @SerializedName("status")
    @IColumnField(name = "status")
    private BlogPostStatus status = BlogPostStatus.DRAFT;

    @SerializedName("published_at")
    @IColumnField(name = "published_at")
    private LocalDateTime publishedAt;

    @SerializedName("created_at")
    @IColumnField(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @SerializedName("updated_at")
    @IColumnField(name = "updated_at")
    private LocalDateTime updatedAt;

    private transient List<ObjectBlogPostSource> sources = List.of();

    public Integer getId() { return id; }

    public String getSlug() { return slug; }

    public String getLang() { return lang; }

    public String getTitle() { return title; }

    public String getSummary() { return summary; }

    public String getContentMarkdown() { return contentMarkdown; }

    public String getCoverImage() { return coverImage; }

    public BlogPostStatus getStatus() { return status; }

    public LocalDateTime getPublishedAt() { return publishedAt; }

    public void setSources(List<ObjectBlogPostSource> sources) {
        this.sources = sources == null ? List.of() : sources;
    }

    /**
     * Calculates the estimated reading time for the blog post based on its content.
     * @return The estimated reading time in minutes. Returns 1 minute if the content is empty or null.
     */
    public int getReadingTimeMinutes() {
        if (contentMarkdown == null || contentMarkdown.isBlank()) return 1;

        final int WORD_COUNT = contentMarkdown.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(WORD_COUNT / (double) AVERAGE_READING_SPEED_WPM));
    }

    @Override
    public Map<String, Object> payload() {
        final Map<String, Object> PAYLOAD = IPayloadProvider.super.payload();
        PAYLOAD.put("reading_time_minutes", getReadingTimeMinutes());
        PAYLOAD.put("sources", sources == null ? Collections.emptyList() : sources.stream().map(ObjectBlogPostSource::payload).toList());
        return PAYLOAD;
    }

    public boolean isValid() {
        return slug != null && !slug.isBlank() &&
               title != null && !title.isBlank() &&
               summary != null && !summary.isBlank() &&
               contentMarkdown != null && !contentMarkdown.isBlank();
    }
}
