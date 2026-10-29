package niwer.photon.sql;

import java.time.LocalDate;
import java.util.List;

import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectBlogPostSource;
import niwer.photon.util.GsonUtils;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.DeletionManager;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.tables.Table;

public class BlogPostSourcesTable extends Table {

    public BlogPostSourcesTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectBlogPostSource.class).execute();
    }

    @Override
    public String name() {
        return "blog_post_sources";
    }

    /**
     * Retrieves a list of ObjectBlogPostSource entries associated with the specified post slug.
     * 
     * @param postSlug The slug of the blog post for which to retrieve source entries.
     * @return A list of ObjectBlogPostSource entries associated with the specified post slug.
     */
    public static List<ObjectBlogPostSource> getByPostSlug(String postSlug) {
        return SelectionManager.select(PhotonEngine.DATA_BASE, BlogPostSourcesTable.class)
            .where(Expressions.isEqualTo("post_slug", postSlug))
            .executeList(ObjectBlogPostSource.class);
    }

    /**
     * Replaces the source entries for a specific blog post identified by its slug.
     * 
     * @param postSlug The slug of the blog post for which to replace source entries.
     * @param sources A list of SourceData objects representing the new source entries to be associated with the blog post.
     */
    public static void replaceForPost(String postSlug, List<SourceData> sources) {
        DeletionManager.delete(PhotonEngine.DATA_BASE, BlogPostSourcesTable.class)
            .where(Expressions.isEqualTo("post_slug", postSlug))
            .execute();

        if (sources == null) return;
        for (SourceData source : sources) {
            InsertionManager.insert(PhotonEngine.DATA_BASE, BlogPostSourcesTable.class,
                    "post_slug", "source_date", "url", "authors_json")
                .row(postSlug, source.date().toString(), source.url(), GsonUtils.GSON.toJson(source.authors()))
                .execute();
        }
    }

    public record SourceData(LocalDate date, String url, List<String> authors) {}
}
