package niwer.photon.web.endpoints.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.BlogPostsTable;
import niwer.photon.sql.BlogPostSourcesTable;
import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.sql.BlogPostsTable.BlogPostStatus;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.session.SessionManager;
import niwer.photon.util.summaries.MarkdownSummaryUtils;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AdminUpdatePostEndpoint implements IEndpoint {

    @Override public String path() { return "/admin/blog/posts/update"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);
    
        try {
            if (SessionManager.requireAdministrator(handler) == null) return;

            final BlogPostPayload PAYLOAD = handler.bodyAsClass(BlogPostPayload.class);
            if(PAYLOAD == null || !PAYLOAD.isValid() || !PAYLOAD.sourcesAreValid()) {
                handler.status(HttpStatus.BAD_REQUEST).result("Invalid payload");
                return;
            }

            final LocalDateTime PUBLISHED_AT = PAYLOAD.isScheduled() ? PAYLOAD.getScheduledAt() : LocalDateTime.now();
            final BlogPostStatus STATUS = PAYLOAD.getScheduledAt() != null ? BlogPostStatus.SCHEDULED : BlogPostStatus.PUBLISHED;

            final ObjectBlogPost EXISTING_POST = BlogPostsTable.getPostById(PAYLOAD.post_id());
            if (EXISTING_POST == null) {
                handler.status(HttpStatus.NOT_FOUND).result("Blog post not found");
                return;
            }

            BlogPostsTable.updatePost(
                PAYLOAD.post_id(),
                PAYLOAD.slug(),
                STATUS,
                PUBLISHED_AT,
                PAYLOAD.title(),
                PAYLOAD.summary(),
                PAYLOAD.content()
            );
            if (!EXISTING_POST.getSlug().equals(PAYLOAD.slug())) {
                BlogPostSourcesTable.replaceForPost(EXISTING_POST.getSlug(), List.of());
            }
            BlogPostSourcesTable.replaceForPost(PAYLOAD.slug(), PAYLOAD.sourceData());
            handler.status(HttpStatus.OK).result("Blog post updated successfully");
            Console.log("Blog post (Id: %d, Slug: %s) updated successfully!", PAYLOAD.post_id(), PAYLOAD.slug()).container(PhotonEngine.LOGGER).type(PhotonLogTypes.WEB_SERVER).send();
        } catch (Exception e) {
            IEndpoint.handleException(AdminUpdatePostEndpoint.class, handler, e);
        }
    }

    private static final record BlogPostPayload(
        int post_id,
        String slug,
        String scheduled_at,
        String title,
        String summary,
        String content,
        List<SourcePayload> sources
    ) {

        /**
         * Checks if the payload is valid by ensuring that all required fields are non-null and non-blank.
         * Required fields include `slug`, `title_fr`, `title_en`, `content_fr`, and `content_en`.
         * 
         * Summaries and scheduled_at are optional and do not affect the validity of the payload.
         * 
         * @return true if the payload is valid; false otherwise.
         */
        public boolean isValid() {
            return slug != null && !slug.isBlank() &&
                   title != null && !title.isBlank() &&
                   content != null && !content.isBlank();
        }

        public boolean sourcesAreValid() {
            if (sources == null) return true;
            return sources.stream().allMatch(SourcePayload::isValid);
        }

        public List<BlogPostSourcesTable.SourceData> sourceData() {
            if (sources == null) return List.of();
            return sources.stream().map(SourcePayload::toData).toList();
        }

        public boolean isScheduled() {
            return scheduled_at != null && !scheduled_at.isBlank();
        }

        public String summary() {
            final boolean HAS_EN_SUMMARY = summary != null && !summary.isBlank();
            return HAS_EN_SUMMARY ? summary : MarkdownSummaryUtils.summarize(content, 2);
        }

        public LocalDateTime getScheduledAt() {
            if (!isScheduled()) return null;
            try {
                return LocalDate.parse(scheduled_at).atStartOfDay();
            } catch (Exception e) {
                return null;
            }
        }

        private static final record SourcePayload(String date, String url, List<String> authors) {
            public boolean isValid() {
                if (date == null || url == null || url.isBlank()) return false;
                try {
                    LocalDate.parse(date);
                    final String SCHEME = URI.create(url).getScheme();
                    return ("http".equalsIgnoreCase(SCHEME) || "https".equalsIgnoreCase(SCHEME))
                        && (authors == null || authors.stream().allMatch(author -> author != null && !author.isBlank()));
                } catch (Exception e) {
                    return false;
                }
            }

            public BlogPostSourcesTable.SourceData toData() {
                return new BlogPostSourcesTable.SourceData(LocalDate.parse(date), url, authors == null ? List.of() : authors);
            }
        }
    }
}