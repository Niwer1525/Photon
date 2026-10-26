package niwer.photon.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import niwer.photon.web.api.FetchWakatimeLanguagesRequests.WakatimeLanguagesResponse;
import niwer.photon.web.api.github.FetchGithubStatsRequest.GithubStatsResponse;
import niwer.photon.web.api.github.FetchGithubStatsRequest.GithubYearStats;

class IPayloadProviderTest {

    @Test
    void convertsPayloadProviderArraysToPayloadMaps() {
        final WakatimeLanguagesResponse RESPONSE = new Gson().fromJson("""
            {
                "data": {
                    "editors": [
                        {
                            "name": "VS Code",
                            "total_seconds": 120
                        }
                    ]
                }
            }
            """, WakatimeLanguagesResponse.class);

        final Map<?, ?> DATA = (Map<?, ?>) RESPONSE.payload().get("data");
        final List<?> EDITORS = assertInstanceOf(List.class, DATA.get("editors"));
        final Map<?, ?> EDITOR = assertInstanceOf(Map.class, EDITORS.get(0));

        assertEquals("VS Code", EDITOR.get("name"));
        assertEquals(120, ((Number) EDITOR.get("total_seconds")).intValue());
    }

    @Test
    void convertsPayloadProviderValuesInsideMaps() {
        final Map<Integer, GithubYearStats> YEARS = new LinkedHashMap<>();
        YEARS.put(2022, new GithubYearStats(10, 2, 3, 4, 5, 24));

        final Map<?, ?> PAYLOAD = new GithubStatsResponse("niwer", "2026-01-01T00:00:00Z", YEARS, 7).payload();
        final Map<?, ?> YEAR = assertInstanceOf(Map.class, ((Map<?, ?>) PAYLOAD.get("years")).get(2022));

        assertEquals(10, YEAR.get("commits"));
        assertEquals(2, YEAR.get("pull_requests"));
        assertEquals(24, YEAR.get("total_combined"));
    }
}
