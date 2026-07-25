package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.domain.recipe.dto.YoutubeReferenceDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeSearchService {

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RESULTS_PER_QUERY = 1;
    private static final Duration YOUTUBE_TIMEOUT = Duration.ofSeconds(20);

    // API 호출 자체가 실패(타임아웃/5xx/네트워크 오류)한 경우에만 재시도
    private static final int MAX_API_RETRY_ATTEMPTS = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(500);

    private static final String NOT_FOUND_TITLE = "관련 유튜브 영상 없음";

    // 대체 쿼리 생성 시 제거할 수식어 (실제 운영 로그 기반으로 계속 보강 필요)
    private static final List<String> BROADENING_STOPWORDS = List.of(
            "만드는 법", "만드는법", "만들기", "황금레시피", "레시피",
            "초간단", "초스피드", "간단한", "쉬운", "집에서", "완벽", "정석", "요리"
    );

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

        return Flux.fromIterable(searchQueries)
                .flatMap(query ->
                        searchSingleVideo(query)
                                .onErrorResume(e -> {
                                    log.warn("유튜브 검색 실패 (검색어: '{}', 원인: {})", query, describeError(e));
                                    return Mono.empty();
                                })
                )
                .collectList()
                .block();
    }

    // --- 내부 메서드 ---

    // 유튜브 영상 1개 검색 수행 (1차 검색 → 결과 없으면 대체 쿼리로 2차 검색 → 그래도 없으면 not-found)
    private Mono<YoutubeReferenceDto> searchSingleVideo(String originalQuery) {
        return callYoutubeApi(originalQuery)
                .flatMap(responseBody -> Mono.justOrEmpty(parseSearchResult(responseBody, originalQuery)))
                .switchIfEmpty(Mono.defer(() -> retryWithFallbackQuery(originalQuery)));
    }

    // 1차 검색 결과가 없을 때 대체 쿼리로 재검색
    private Mono<YoutubeReferenceDto> retryWithFallbackQuery(String originalQuery) {
        String fallbackQuery = buildFallbackQuery(originalQuery);

        if (fallbackQuery == null) {
            log.info("대체 쿼리 생성 불가 - 결과 없음 처리 (검색어: '{}')", originalQuery);
            return Mono.just(buildNotFoundReference(originalQuery));
        }

        log.info("1차 검색 결과 없음. 대체 쿼리로 재검색 - 원본: '{}', 대체: '{}'", originalQuery, fallbackQuery);

        return callYoutubeApi(fallbackQuery)
                // 최종 응답에 노출되는 searchQuery는 항상 '원본 검색어' 기준으로 유지 (프론트 일관성)
                .flatMap(responseBody -> Mono.justOrEmpty(parseSearchResult(responseBody, originalQuery)))
                .switchIfEmpty(Mono.just(buildNotFoundReference(originalQuery)));
    }

    // 실제 YouTube API 호출 (타임아웃/5xx/네트워크 오류에 한해서만 재시도)
    private Mono<String> callYoutubeApi(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.googleapis.com")
                        .path("/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query)
                        .queryParam("type", "video")
                        .queryParam("maxResults", MAX_RESULTS_PER_QUERY)
                        .queryParam("regionCode", "KR")
                        .queryParam("relevanceLanguage", "ko")
                        .queryParam("key", youtubeApiKey)
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .timeout(YOUTUBE_TIMEOUT)
                .retryWhen(Retry.backoff(MAX_API_RETRY_ATTEMPTS, RETRY_BACKOFF)
                        .filter(this::isRetriableApiError)
                        .doBeforeRetry(signal -> log.warn("유튜브 API 재시도 (검색어: '{}', 시도: {})",
                                query, signal.totalRetries() + 1)))
                .doOnError(e -> log.error("YouTube API 호출 최종 실패 (검색어: '{}', 원인: {})", query, describeError(e)));
    }

    // 재시도 대상 판별: 타임아웃 / 5xx / 네트워크 오류만 재시도, 4xx나 파싱 오류는 재시도하지 않음
    private boolean isRetriableApiError(Throwable e) {
        if (e instanceof WebClientResponseException wcre) {
            return wcre.getStatusCode().is5xxServerError();
        }
        return e instanceof TimeoutException || e instanceof WebClientRequestException;
    }

    // 검색어에서 흔한 수식어를 제거해 더 넓은 대체 쿼리를 생성
    private String buildFallbackQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return null;
        }

        String simplified = originalQuery;
        for (String stopword : BROADENING_STOPWORDS) {
            simplified = simplified.replace(stopword, "");
        }
        simplified = simplified.replaceAll("\\s+", " ").trim();

        if (simplified.isBlank() || simplified.equals(originalQuery.trim())) {
            return null; // 제거해도 원본과 동일하거나 빈 값이면 null
        }

        return simplified;
    }

    // 검색 결과 없음 placeholder 생성
    private YoutubeReferenceDto buildNotFoundReference(String originalQuery) {
        return YoutubeReferenceDto.builder()
                .title(NOT_FOUND_TITLE)
                .url(null)
                .thumbnail(null)
                .searchQuery(originalQuery)
                .build();
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
            } // 검색 결과 없음 → null 반환, 예외 아님

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
                    .searchQuery(searchQuery)
                    .build();

        } catch (Exception e) {
            log.error("유튜브 응답 파싱 실패 (검색어: '{}')", searchQuery, e);
            return null;
        }
    }

    // 예외 객체 전체(toString/스택트레이스)를 로그에 남기지 않기 위한 안전한 요약 정보 추출
    // URI 쿼리 파라미터(API 키 등) 노출 방지 목적 - 예외 타입과 HTTP 상태코드만 기록
    private String describeError(Throwable e) {
        if (e instanceof WebClientResponseException wcre) {
            return e.getClass().getSimpleName() + " status=" + wcre.getStatusCode().value();
        }
        return e.getClass().getSimpleName();
    }
}
