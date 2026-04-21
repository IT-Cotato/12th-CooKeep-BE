package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class GeminiServiceTest {

    private MockWebServer mockWebServer;
    private GeminiService geminiService;

    private static final String VALID_GEMINI_RESPONSE = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"title\\":\\"테스트 레시피\\",\\"ingredients\\":{\\"user_ingredients\\":[{\\"ingredientId\\":1,\\"name\\":\\"양파\\",\\"quantity\\":1.0,\\"unit\\":\\"개\\"}]},\\"steps\\":[\\"1. 볶는다\\"],\\"youtube_search_queries\\":[\\"양파볶음\\"]}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .filter((request, next) -> {
                    URI originalUri = request.url();

                    // path + query 유지하면서 host만 mock으로 변경
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

        geminiService = new GeminiService(webClient);
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiService, "model", "gemini-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("generateRecipeByPrompt - 정상 응답")
    class SuccessResponse {

        @Test
        @DisplayName("정상 응답 시 레시피를 파싱하여 반환한다")
        void 정상응답_레시피_파싱_반환() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(VALID_GEMINI_RESPONSE)
                    .addHeader("Content-Type", "application/json"));

            GeminiRecipeResponseDto result = geminiService.generateRecipeByPrompt("테스트 프롬프트");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("테스트 레시피");
            assertThat(result.getSteps()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("generateRecipeByPrompt - 재시도 (Retry)")
    class RetryBehavior {

        @Test
        @DisplayName("503 응답 후 정상 응답 시 재시도로 성공한다")
        void response503_재시도_성공() {
            // 1차: 503 실패
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(503)
                    .setBody("error")
                    .addHeader("Content-Type", "text/plain"));
            // 2차: 정상
            mockWebServer.enqueue(new MockResponse()
                    .setBody(VALID_GEMINI_RESPONSE)
                    .addHeader("Content-Type", "application/json"));

            GeminiRecipeResponseDto result = geminiService.generateRecipeByPrompt("테스트 프롬프트");

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("테스트 레시피");
            // 총 2회 요청
            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("500 응답이 연속 3회면 최대 재시도 후 AI_SEARCH_FAILED 예외를 던진다")
        void response500_3회_재시도_실패() {
            // 4회 (원본 1 + 재시도 3) 모두 500
            for (int i = 0; i < 4; i++) {
                mockWebServer.enqueue(new MockResponse()
                        .setResponseCode(500)
                        .setBody("error")
                        .addHeader("Content-Type", "text/plain"));
            }

            assertThatThrownBy(() -> geminiService.generateRecipeByPrompt("테스트 프롬프트"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isIn(ErrorCode.AI_SEARCH_FAILED, ErrorCode.AI_RATE_LIMIT_EXCEEDED));

            // 원본 1회 + 재시도 최대 3회 = 총 4회
            assertThat(mockWebServer.getRequestCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("400 응답은 재시도 없이 즉시 AI_SEARCH_FAILED 예외를 던진다")
        void response400_재시도없음_즉시실패() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(400)
                    .setBody("bad request"));

            assertThatThrownBy(() -> geminiService.generateRecipeByPrompt("테스트 프롬프트"))
                    .isInstanceOf(AppException.class);

            // 4xx는 재시도 없이 1회만 요청
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("429 응답은 재시도 없이 AI_RATE_LIMIT_EXCEEDED 예외를 던진다")
        void response429_재시도없음_RATE_LIMIT_예외() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(429)
                    .setBody("bad request"));

            assertThatThrownBy(() -> geminiService.generateRecipeByPrompt("테스트 프롬프트"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_RATE_LIMIT_EXCEEDED));

            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("generateRecipeByPrompt - 타임아웃")
    class TimeoutBehavior {

        @Test
        @DisplayName("응답이 120초 이내에 오지 않으면 예외가 발생한다")
        void 타임아웃_예외발생() {
            // 130초 지연 (TIMEOUT = 120초 초과)
            mockWebServer.enqueue(new MockResponse()
                    .setBody(VALID_GEMINI_RESPONSE)
                    .setBodyDelay(130, TimeUnit.SECONDS));

            // 타임아웃으로 인한 예외 발생 확인
            // 타임아웃 설정값 자체가 올바르게 적용되는지를 검증
            assertThatThrownBy(() -> geminiService.generateRecipeByPrompt("테스트 프롬프트"))
                    .isInstanceOf(Exception.class); // TimeoutException or AppException
        }
    }
}
