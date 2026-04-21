package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.domain.recipe.dto.YoutubeReferenceDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class YoutubeSearchServiceTest {

    private MockWebServer mockWebServer;
    private YoutubeSearchService youtubeSearchService;

    // 정상 응답 JSON 템플릿
    private static final String SUCCESS_RESPONSE = """
            {
              "items": [
                {
                  "id": { "videoId": "abc123" },
                  "snippet": {
                    "title": "테스트 영상",
                    "thumbnails": {
                      "medium": { "url": "https://img.youtube.com/vi/abc123/mqdefault.jpg" }
                    }
                  }
                }
              ]
            }
            """;

    // 검색 결과 없는 응답 JSON
    private static final String EMPTY_RESPONSE = """
            { "items": [] }
            """;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .filter((request, next) -> {
                    URI originalUri = request.url();

                    String pathWithQuery = originalUri.getRawPath();
                    if (originalUri.getRawQuery() != null) {
                        pathWithQuery += "?" + originalUri.getRawQuery();
                    }

                    URI newUri = mockWebServer.url(pathWithQuery).uri();

                    return next.exchange(
                            ClientRequest.from(request)
                                    .url(newUri)
                                    .build()
                    );
                })
                .build();

        youtubeSearchService = new YoutubeSearchService(webClient);
        ReflectionTestUtils.setField(youtubeSearchService, "youtubeApiKey", "test-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("searchVideos - null/빈 입력 처리")
    class NullAndEmptyInput {

        @Test
        @DisplayName("null이 들어오면 빈 리스트를 반환한다")
        void null_입력_빈리스트_반환() {
            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트가 들어오면 빈 리스트를 반환한다")
        void 빈리스트_입력_빈리스트_반환() {
            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(List.of());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchVideos - 정상 응답 처리")
    class SuccessResponse {

        @Test
        @DisplayName("검색어 1개에 대해 정상 응답이면 DTO 1개를 반환한다")
        void 검색어_1개_정상응답_DTO_1개() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(SUCCESS_RESPONSE)
                    .addHeader("Content-Type", "application/json"));

            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(List.of("김치볶음밥"));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("테스트 영상");
            assertThat(result.get(0).getUrl()).isEqualTo("https://www.youtube.com/watch?v=abc123");
            assertThat(result.get(0).getThumbnail()).isEqualTo("https://img.youtube.com/vi/abc123/mqdefault.jpg");
        }

        @Test
        @DisplayName("검색 결과가 없으면 해당 검색어는 결과에 포함되지 않는다")
        void 검색결과없음_결과_미포함() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(EMPTY_RESPONSE)
                    .addHeader("Content-Type", "application/json"));

            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(List.of("검색결과없는쿼리"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchVideos - 병렬 처리 및 부분 실패")
    class ParallelAndPartialFailure {

        @Test
        @DisplayName("검색어 3개 중 1개 실패해도 나머지 2개 결과를 반환한다")
        void 검색어3개_1개실패_나머지2개_반환() {
            // 1번: 성공
            mockWebServer.enqueue(new MockResponse()
                    .setBody(SUCCESS_RESPONSE)
                    .addHeader("Content-Type", "application/json"));
            // 2번: 500 에러
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            // 3번: 성공
            mockWebServer.enqueue(new MockResponse()
                    .setBody(SUCCESS_RESPONSE)
                    .addHeader("Content-Type", "application/json"));

            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(
                    List.of("쿼리1", "쿼리2실패", "쿼리3"));

            // 실패한 1개는 제외, 성공한 2개만 반환
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("모든 검색어가 실패하면 빈 리스트를 반환한다")
        void 모든검색어_실패_빈리스트_반환() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(
                    List.of("쿼리1", "쿼리2"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchVideos - 타임아웃 처리")
    class TimeoutHandling {

        @Test
        @DisplayName("응답이 타임아웃 내에 오지 않으면 해당 검색어는 건너뛴다")
        void 타임아웃_발생_해당검색어_스킵() throws InterruptedException {
            // 응답을 25초 지연 (YOUTUBE_TIMEOUT = 20초보다 길게)
            mockWebServer.enqueue(new MockResponse()
                    .setBody(SUCCESS_RESPONSE)
                    .addHeader("Content-Type", "application/json")
                    .setBodyDelay(25, TimeUnit.SECONDS));
            // 두 번째 검색어는 정상 응답
            mockWebServer.enqueue(new MockResponse()
                    .setBody(SUCCESS_RESPONSE)
                    .addHeader("Content-Type", "application/json"));

            long start = System.currentTimeMillis();
            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(
                    List.of("타임아웃쿼리", "정상쿼리"));
            long elapsed = System.currentTimeMillis() - start;

            // 타임아웃이 적용되므로 25초까지 기다리지 않음
            assertThat(elapsed).isLessThan(25_000L);
            // 타임아웃된 첫 번째는 제외, 두 번째 성공 결과만 반환 (또는 빈 리스트)
            // flatMap 병렬 처리 + onErrorResume이므로 성공한 것만 포함
            assertThat(result.size()).isLessThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("searchVideos - 응답 파싱")
    class ResponseParsing {

        @Test
        @DisplayName("videoId가 없는 응답은 결과에서 제외된다")
        void videoId_없는_응답_제외() {
            String noVideoIdResponse = """
                    {
                      "items": [
                        {
                          "id": {},
                          "snippet": { "title": "제목" }
                        }
                      ]
                    }
                    """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(noVideoIdResponse)
                    .addHeader("Content-Type", "application/json"));

            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(List.of("쿼리"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("썸네일 URL이 없으면 기본 썸네일 URL이 적용된다")
        void 썸네일_없으면_기본URL_적용() {
            String noThumbnailResponse = """
                    {
                      "items": [
                        {
                          "id": { "videoId": "xyz789" },
                          "snippet": {
                            "title": "썸네일없는영상",
                            "thumbnails": {}
                          }
                        }
                      ]
                    }
                    """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(noThumbnailResponse)
                    .addHeader("Content-Type", "application/json"));

            List<YoutubeReferenceDto> result = youtubeSearchService.searchVideos(List.of("쿼리"));

            assertThat(result).hasSize(1);
            // 기본 썸네일 URL 형식 적용
            assertThat(result.get(0).getThumbnail())
                    .isEqualTo("https://img.youtube.com/vi/xyz789/mqdefault.jpg");
        }
    }
}
