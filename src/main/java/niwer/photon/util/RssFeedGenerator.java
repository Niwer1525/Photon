package niwer.photon.util;

import java.util.List;

import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.util.TranslationManager.Language;

public class RssFeedGenerator {

    public static String buildRssXml(List<ObjectBlogPost> posts, Language lang) {
        String baseUrl = lang == Language.FRENCH ? "https://niwer.dev/fr/blog" : "https://niwer.dev/blog";
        String feedTitle = lang == Language.FRENCH ? "Niwer Blog (FR)" : "Niwer Blog";
        String feedDesc = "DESCRIPTION";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        sb.append("<channel>\n");
        sb.append("  <title>").append(escapeXml(feedTitle)).append("</title>\n");
        sb.append("  <link>").append(baseUrl).append("</link>\n");
        sb.append("  <description>").append(escapeXml(feedDesc)).append("</description>\n");
        sb.append("  <language>").append(lang).append("</language>\n");
        sb.append("  <atom:link href=\"https://panel.niwer.dev/api/blog/rss?lang=").append(lang)
          .append("\" rel=\"self\" type=\"application/rss+xml\" />\n");

        for (ObjectBlogPost post : posts) {
            String itemUrl = baseUrl + "/" + post.getSlug();
            sb.append("  <item>\n");
            sb.append("    <title>").append(escapeXml(post.getTitle())).append("</title>\n");
            sb.append("    <link>").append(itemUrl).append("</link>\n");
            sb.append("    <guid isPermaLink=\"true\">").append(itemUrl).append("</guid>\n");
            sb.append("    <description>").append(escapeXml(post.getSummary())).append("</description>\n");
            if (post.getPublishedAt() != null) {
                // RSS requires RFC-1123 for dates (e.g: Mon, 19 Oct 2026 14:00:00 GMT)
                // String rfcDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.parse(post.getPublishedAt()).atZone(java.time.ZoneOffset.UTC));
                // sb.append("    <pubDate>").append(rfcDate).append("</pubDate>\n");
            }
            sb.append("  </item>\n");
        }

        sb.append("</channel>\n");
        sb.append("</rss>");
        return sb.toString();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
