package niwer.photon.web.api;

import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import com.google.gson.annotations.SerializedName;

import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.IPayloadProvider;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.api.FetchWakatimeLanguagesRequests.WakatimeLanguagesResponse;

public class FetchWakatimeLanguagesRequests extends ApiRequest<WakatimeLanguagesResponse> {

    public static void main(String[] args) {
        Directories.load();

        var x  = new FetchWakatimeLanguagesRequests().request();

        Console.debug(x.payload());
    }

    public FetchWakatimeLanguagesRequests() {}

    @Override
    public String url() {
        return String.format("https://wakatime.com/api/v1/users/current/stats/all_time?api_key=%s", Directories.config().wakatime_api_key);
    }

    @Override
    public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public WakatimeLanguagesResponse request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch(RESPONSE.statusCode()) {
                case 200:
                    return GsonUtils.GSON.fromJson(RESPONSE.body(), WakatimeLanguagesResponse.class);
                default:
                    Console.log(String.format("Error (%d) : %s", RESPONSE.statusCode(), RESPONSE.body())).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override public void addHeaders(Builder builder) {}

    public static class WakatimeLanguagesResponse implements IPayloadProvider {
        @SerializedName("data") private WakatimeLanguagesData data = new WakatimeLanguagesData();

        public static class WakatimeLanguagesData implements IPayloadProvider {
            // @SerializedName("holidays") private Number holidays;
            @SerializedName("created_at") private String created_at;
            @SerializedName("daily_average") private Number daily_average;
            @SerializedName("total_seconds_including_other_language") private Number total_seconds_including_other_language;
            // @SerializedName("days_including_holidays") private Number days_including_holidays;
            @SerializedName("daily_average_including_other_language") private Number daily_average_including_other_language;
            // @SerializedName("human_readable_daily_average") private String human_readable_daily_average;
            @SerializedName("best_day") private WakatimeLanguageBestDay best_day = new WakatimeLanguageBestDay();
            @SerializedName("editors") private WakatimeLanguageEditor[] editors;
            @SerializedName("categories") private WakatimeCategory[] categories;
            @SerializedName("languages") private WakatimeLanguage[] languages;
        }

        public static class WakatimeLanguageBestDay implements IPayloadProvider {
            @SerializedName("date") private String date;
            @SerializedName("total_seconds") private Number total_seconds;
            @SerializedName("text") private String text;
        }

        public static class WakatimeLanguageEditor implements IPayloadProvider {
            @SerializedName("total_seconds") private Number total_seconds;
            @SerializedName("name") private String name;
            // @SerializedName("ai_coding_seconds") private String ai_coding_seconds;
            // @SerializedName("manual_coding_seconds") private String manual_coding_seconds;
            // @SerializedName("ai_additions") private Number ai_additions;
            // @SerializedName("ai_deletions") private Number ai_deletions;
            // @SerializedName("human_additions") private Number human_additions;
            // @SerializedName("human_deletions") private Number human_deletions;
            @SerializedName("percent") private Number percent;
            @SerializedName("digital") private String digital;
            @SerializedName("decimal") private String decimal;
            // @SerializedName("text") private String text;
            @SerializedName("hours") private Number hours;
            @SerializedName("minutes") private Number minutes;
        }

        public static class WakatimeCategory implements IPayloadProvider {
            @SerializedName("name") private String name;
            @SerializedName("total_seconds") private Number total_seconds;
            @SerializedName("percent") private Number percent;
            @SerializedName("digital") private String digital;
            @SerializedName("decimal") private String decimal;
            // @SerializedName("text") private String text;
            @SerializedName("hours") private Number hours;
            @SerializedName("minutes") private Number minutes;
        }

        public static class WakatimeLanguage implements IPayloadProvider {
            @SerializedName("name") private String name;
            @SerializedName("total_seconds") private Number total_seconds;
            // @SerializedName ("ai_coding_seconds") private String ai_coding_seconds;
            // @SerializedName("manual_coding_seconds") private String manual_coding_seconds;
            @SerializedName("percent") private Number percent;
            @SerializedName("digital") private String digital;
            @SerializedName("decimal") private String decimal;
            // @SerializedName("text") private String text;
            @SerializedName("hours") private Number hours;
            @SerializedName("minutes") private Number minutes;
        }
    }
}