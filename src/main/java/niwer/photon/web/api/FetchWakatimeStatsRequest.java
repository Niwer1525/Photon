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
import niwer.photon.web.api.FetchWakatimeStatsRequest.WakatimeStatsResponse;

public class FetchWakatimeStatsRequest extends ApiRequest<WakatimeStatsResponse> {

    public FetchWakatimeStatsRequest() {}

    @Override
    public String url() {
        return String.format("https://wakatime.com/api/v1/users/current/all_time_since_today?api_key=%s", Directories.config().wakatime_api_key);
    }

    @Override
    public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public WakatimeStatsResponse request() {
        try {
            final HttpResponse<String> RESPONSE = this.sendHttpRequest(HttpResponse.BodyHandlers.ofString());
            switch(RESPONSE.statusCode()) {
                case 200:
                    return GsonUtils.GSON.fromJson(RESPONSE.body(), WakatimeStatsResponse.class);
                default:
                    Console.log(String.format("Error (%d) : %s", RESPONSE.statusCode(), RESPONSE.body())).type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override public void addHeaders(Builder builder) {}

    public static class WakatimeStatsResponse implements IPayloadProvider {
        @SerializedName("data") private WakatimeStatsData data = new WakatimeStatsData();

        private static class WakatimeStatsData implements IPayloadProvider {
            @SerializedName("range") private WakatimeRange range = new WakatimeRange();
            @SerializedName("total_seconds") private String total_seconds;
            // @SerializedName("text") private String text;
            @SerializedName("decimal") private String decimal;
            @SerializedName("digital") private String digital;
            @SerializedName("daily_average") private String daily_average;
        }

        private static class WakatimeRange implements IPayloadProvider {
            @SerializedName("start") private String start;
            @SerializedName("start_date") private String start_date;
            // @SerializedName("start_text") private String start_text;
            @SerializedName("end") private String end;
            @SerializedName("end_date") private String end_date;
            // @SerializedName("end_text") private String end_text;
            @SerializedName("timezone") private String timezone;
        }
    }
}