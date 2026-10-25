package niwer.photon.web.endpoints.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.BlogPostsTable;
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
            if(PAYLOAD == null || !PAYLOAD.isValid()) {
                handler.status(HttpStatus.BAD_REQUEST).result("Invalid payload");
                return;
            }

            final LocalDateTime PUBLISHED_AT = PAYLOAD.isScheduled() ? PAYLOAD.getScheduledAt() : LocalDateTime.now();
            final BlogPostStatus STATUS = PAYLOAD.getScheduledAt() != null ? BlogPostStatus.SCHEDULED : BlogPostStatus.PUBLISHED;

            BlogPostsTable.updatePost(
                PAYLOAD.post_id(),
                PAYLOAD.slug(),
                STATUS,
                PUBLISHED_AT,
                PAYLOAD.title(),
                PAYLOAD.summary(),
                PAYLOAD.content()
            );
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
        String content
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
    }
}