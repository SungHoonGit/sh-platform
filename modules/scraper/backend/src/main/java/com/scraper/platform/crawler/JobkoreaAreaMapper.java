package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 잡코리아 지역 매핑코드(예: B000, I130)를 한글 지역명으로 변환한다.
 * <p>
 * 잡코리아 프론트가 사용하는 CDN 코드 데이터(county/city JSON)를 한 번 로드해
 * mappingCode → "시도 구군" 형태의 역매핑 테이블을 메모리에 캐시한다.
 */
@Slf4j
@Component
public class JobkoreaAreaMapper {

    private static final String COUNTY_URL = "https://cdn-assets.jobkorea.co.kr/code/client/"
            + "county-jobkorea-active-visible-code-parentCode-type-continentCode-nationCode-cityName-code"
            + "-mappingCode-mappingParentCode-name-displayName.json";
    private static final String CITY_URL = "https://cdn-assets.jobkorea.co.kr/code/client/"
            + "city-jobkorea-active-visible-code-parentCode-type-continentCode-nationCode-cityName-code"
            + "-mappingCode-mappingParentCode-name-displayName.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final Map<String, String> EXTRA_CODES = Map.of(
            "Q000", "전국"
    );

    private volatile Map<String, String> codeToName = Collections.emptyMap();
    private volatile boolean loaded = false;

    /**
     * 지역 코드 목록을 한글 지역명(콤마 구분)으로 변환한다.
     * 변환에 실패하거나 코드가 없으면 fallback(검색 지역)을 반환한다.
     *
     * @param areaCodes 잡코리아 매핑코드 목록
     * @param fallback  변환 실패 시 사용할 대체 지역명
     * @return 변환된 지역명
     */
    public String toAreaText(List<String> areaCodes, String fallback) {
        if (areaCodes == null || areaCodes.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        Map<String, String> map = getCodeMap();
        Set<String> names = new LinkedHashSet<>();
        for (String code : areaCodes) {
            String name = map.getOrDefault(code, "");
            if (name.isBlank()) {
                continue;
            }
            names.add(name);
        }
        if (names.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return String.join(", ", new ArrayList<>(names));
    }

    private Map<String, String> getCodeMap() {
        if (loaded) {
            return codeToName;
        }
        synchronized (this) {
            if (!loaded) {
                codeToName = loadCodeMap();
                loaded = true;
            }
            return codeToName;
        }
    }

    private Map<String, String> loadCodeMap() {
        Map<String, String> map = new HashMap<>(EXTRA_CODES);
        try {
            for (String url : List.of(COUNTY_URL, CITY_URL)) {
                JsonNode root = fetchJson(url);
                if (root == null || !root.isObject()) {
                    continue;
                }
                root.fields().forEachRemaining(entry -> {
                    JsonNode value = entry.getValue();
                    JsonNode mapping = value.path("foreignMappings").get(0);
                    if (mapping == null) {
                        return;
                    }
                    String mappingCode = mapping.path("mappingCode").asText("");
                    if (mappingCode.isBlank()) {
                        return;
                    }
                    JsonNode attr = value.path("foreignAttributes").get(0);
                    String displayName = attr != null ? attr.path("displayName").asText("") : "";
                    String cityName = value.path("cityName").asText("");
                    boolean wholeRegion = mappingCode.equals(mapping.path("mappingParentCode").asText(""));

                    String name;
                    if (wholeRegion && !cityName.isBlank()) {
                        name = cityName;
                    } else if (!cityName.isBlank() && !displayName.isBlank()) {
                        name = cityName + " " + displayName;
                    } else if (!displayName.isBlank()) {
                        name = displayName;
                    } else if (!cityName.isBlank()) {
                        name = cityName;
                    } else {
                        return;
                    }
                    map.putIfAbsent(mappingCode, name);
                });
            }
            log.info("Jobkorea area code map loaded: {} codes", map.size());
        } catch (Exception e) {
            log.warn("Failed to load Jobkorea area code map from CDN, using empty map", e);
        }
        return Collections.unmodifiableMap(map);
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Jobkorea area code request failed: HTTP {} for {}", response.statusCode(), url);
            return null;
        }
        return OBJECT_MAPPER.readTree(response.body());
    }
}
