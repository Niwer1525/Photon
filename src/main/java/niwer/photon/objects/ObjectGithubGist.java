package niwer.photon.objects;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class ObjectGithubGist {

    @SerializedName("id") public String id;
    @SerializedName("description") public String description;
    @SerializedName("public") public boolean isPublic;
    @SerializedName("html_url") public String htmlUrl;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("updated_at") public String updatedAt;
    @SerializedName("files") public Map<String, ObjectGithubGistFile> files;
    
    public class ObjectGithubGistFile {
        @SerializedName("filename") private String filename;
        @SerializedName("type") private String type;
        @SerializedName("language") private String language;
        @SerializedName("raw_url") private String rawUrl;
        @SerializedName("size") private int size;

        public String getFilename() { return filename; }
        public String getType() { return type; }
        public String getLanguage() { return language; }
        public String getRawUrl() { return rawUrl; }
        public int getSize() { return size; }
    }
}
