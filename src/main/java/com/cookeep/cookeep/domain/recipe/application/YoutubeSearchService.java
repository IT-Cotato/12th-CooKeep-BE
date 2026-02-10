package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.domain.recipe.dto.YoutubeReferenceDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeSearchService {

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final int MAX_RESULTS_PER_QUERY = 1;

    /**
     * [검색 방법]
     * 1. Gemini가 유튜브 검색어 리스트 생성
     * 2. YouTube Data API로 실제 영상을 검색하여 결과 리턴
     *
     * @param searchQueries Gemini가 생성한 한국어 검색어 목록 (예: ["김치볶음밥 만들기", "간단한 볶음밥"])
     * @return 실제 유효한 YoutubeReferenceDto 리스트
     */
    public List<YoutubeReferenceDto> searchVideos(List<String> searchQueries) {
        if (searchQueries == null || searchQueries.isEmpty()) {
            return new ArrayList<>();
        }

        List<YoutubeReferenceDto> results = new ArrayList<>();

        for (String query : searchQueries) {
            try {
                YoutubeReferenceDto video = searchSingleVideo(query);
                if (video != null) {
                    results.add(video);
                }
            } catch (Exception e) {
                // 하나의 검색어가 실패해도 나머지는 계속 진행
                log.warn("유튜브 검색 실패 (검색어: '{}'): {}", query, e.getMessage());
            }
        }

        return results;
    }

    // --- 내부 메서드 ---

    // 유튜브 영상 1개 검색 수행
    private YoutubeReferenceDto searchSingleVideo(String query) {
        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/youtube/v3/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", query)
                            .queryParam("type", "video")
                            .queryParam("maxResults", MAX_RESULTS_PER_QUERY)
                            .queryParam("regionCode", "KR")       // 한국 지역 영상 우선
                            .queryParam("relevanceLanguage", "ko") // 한국어 영상 우선
                            .queryParam("key", youtubeApiKey)
                            .build()
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSearchResult(responseBody, query);

        } catch (Exception e) {
            log.error("YouTube API 호출 실패 (검색어: '{}')", query, e);
            return null;
        }
    }

    // YouTube API 응답 dto로 파싱
    private YoutubeReferenceDto parseSearchResult(String responseBody, String searchQuery) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("items");

            if (items.isEmpty()) {
                log.warn("유튜브 검색 결과 없음 (검색어: '{}')", searchQuery);
                return null;
            }

            JsonNode firstItem = items.get(0);
            JsonNode snippet = firstItem.path("snippet");
            JsonNode videoId = firstItem.path("id").path("videoId");

            if (videoId.isMissingNode() || videoId.isNull()) {
                log.warn("videoId 없음 (검색어: '{}')", searchQuery);
                return null;
            }

            String id = videoId.asText();
            String title = snippet.path("title").asText(searchQuery);
            // YouTube API가 제공하는 실제 썸네일 URL
            String thumbnail = snippet.path("thumbnails").path("medium").path("url").asText(
                    "https://img.youtube.com/vi/" + id + "/mqdefault.jpg"
            );

            log.info("유튜브 검색 성공 - 검색어: '{}', 제목: '{}', videoId: {}", searchQuery, title, id);

            return YoutubeReferenceDto.builder()
                    .title(title)
                    .url("https://www.youtube.com/watch?v=" + id)
                    .thumbnail(thumbnail)
                    .build();

        } catch (Exception e) {
            log.error("유튜브 응답 파싱 실패 (검색어: '{}')", searchQuery, e);
            return null;
        }
    }
}
