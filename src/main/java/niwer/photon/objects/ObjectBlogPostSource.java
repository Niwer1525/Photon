package niwer.photon.objects;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;

import niwer.queryon.SQLSerializable;
import niwer.queryon.tables.api.IColumnField;

public class ObjectBlogPostSource extends SQLSerializable<ObjectBlogPostSource> implements IPayloadProvider {

    private static final Gson GSON = new Gson();

    @IColumnField(name = "id", primaryKey = true, autoIncrement = true)
    private Integer id;

    @IColumnField(name = "post_slug", notNull = true, charLimit = 255)
    private String postSlug;

    @SerializedName("date")
    @IColumnField(name = "source_date", notNull = true)
    private String sourceDate;

    @SerializedName("url")
    @IColumnField(name = "url", notNull = true, charLimit = 2048)
    private String url;

    @IColumnField(name = "authors_json", charLimit = 65535)
    private String authorsJson;

    public LocalDate getDate() {
        return sourceDate == null ? null : LocalDate.parse(sourceDate);
    }

    public String getUrl() { return url; }

    public List<String> getAuthors() {
        if (authorsJson == null || authorsJson.isBlank()) return List.of();
        final List<String> AUTHORS = GSON.fromJson(authorsJson, new TypeToken<List<String>>() {}.getType());
        return AUTHORS == null ? List.of() : List.copyOf(AUTHORS);
    }

    @Override
    public Map<String, Object> payload() {
        final Map<String, Object> PAYLOAD = new LinkedHashMap<>();
        PAYLOAD.put("date", sourceDate);
        PAYLOAD.put("url", url);
        PAYLOAD.put("authors", new ArrayList<>(getAuthors()));
        return PAYLOAD;
    }
}
