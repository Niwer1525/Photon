package niwer.photon.sql;

import niwer.photon.objects.ObjectBlogPost;
import niwer.queryon.DataBase;
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
}
