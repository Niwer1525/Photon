package niwer.photon.util;

import java.io.StringWriter;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import niwer.photon.objects.ObjectBlogPost;
import niwer.photon.util.TranslationManager.Language;

public class RssFeedGenerator {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();
    private static final XMLOutputFactory XML_FACTORY = XMLOutputFactory.newInstance();

    public static String buildRssXml(List<ObjectBlogPost> posts, Language lang) {
        final boolean IS_FRENCH = lang == Language.FRENCH;
        final String BASE_URL = IS_FRENCH ? "https://niwer.dev/fr/blog" : "https://niwer.dev/blog";
        final String FEED_TITLE = IS_FRENCH ? "Niwer Blog (FR)" : "Niwer Blog";
        final String FEED_DESCRIPTION = IS_FRENCH ? "Articles sur le développement, les projets et la technologie." : "Articles about development, projects, and technology.";
        final String SELF_LINK = "https://panel.niwer.dev/blog/rss?lang=" + lang.code();

        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter w = XML_FACTORY.createXMLStreamWriter(out);

            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("rss");
            w.writeAttribute("version", "2.0");
            w.writeNamespace("atom", "http://www.w3.org/2005/Atom");
            w.writeNamespace("content", "http://purl.org/rss/1.0/modules/content/");

            w.writeStartElement("channel");
            writeElement(w, "title", FEED_TITLE);
            writeElement(w, "link", BASE_URL);
            writeElement(w, "description", FEED_DESCRIPTION);
            writeElement(w, "language", lang.code());

            w.writeStartElement("http://www.w3.org/2005/Atom", "link");
            w.writeAttribute("href", SELF_LINK);
            w.writeAttribute("rel", "self");
            w.writeAttribute("type", "application/rss+xml");
            w.writeEndElement(); // atom:link

            for (final ObjectBlogPost POST : posts) {
                final String POST_URL = BASE_URL + "/" + POST.getSlug();

                w.writeStartElement("item");
                writeElement(w, "title", POST.getTitle());
                writeElement(w, "link", POST_URL);

                w.writeStartElement("guid");
                w.writeAttribute("isPermaLink", "true");
                w.writeCharacters(POST_URL);
                w.writeEndElement(); // guid

                writeElement(w, "description", POST.getSummary());

                /* Write the content */
                w.writeStartElement("http://purl.org/rss/1.0/modules/content/", "encoded");
                w.writeCData(markdownToHtml(POST));
                w.writeEndElement();

                /* Write the date */
                if (POST.getPublishedAt() != null) writeElement(w, "pubDate", POST.getAsRFC1123Date());

                w.writeEndElement(); // item
            }

            w.writeEndElement(); // channel
            w.writeEndElement(); // rss
            w.writeEndDocument();
            w.flush();

            return out.toString();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to generate RSS XML", e);
        }
    }

    private static void writeElement(XMLStreamWriter w, String tag, String text) throws XMLStreamException {
        if (text == null) return;
        w.writeStartElement(tag);
        w.writeCharacters(text);
        w.writeEndElement();
    }

    private static String markdownToHtml(ObjectBlogPost post) {
        final String POST_MARKDOWN = post.getContentMarkdown();
        if (POST_MARKDOWN == null || POST_MARKDOWN.isBlank()) return "";

        return HTML_RENDERER.render(MARKDOWN_PARSER.parse(POST_MARKDOWN));
    }
}