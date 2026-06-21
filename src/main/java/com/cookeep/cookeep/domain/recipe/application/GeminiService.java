package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeRequestDto;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.IngredientDetailDto;
import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
import com.cookeep.cookeep.domain.recipe.entity.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${google.gemini.api-key}")
    private String apiKey;

    @Value("${google.gemini.model}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int RANDOM_MIN_SELECT_COUNT = 3;

    // 일반 신규
    public GeminiRecipeResponseDto generateRecipe(
            List<IngredientDetailDto> ingredients,
            Feature feature,
            List<String> dislikedIngredients) {
        return generateRecipeByPrompt(buildPrompt(ingredients, feature, List.of(), dislikedIngredients));
    }

    // 일반 재요청
    public GeminiRecipeResponseDto generateRecipeWithExclusion(
            List<IngredientDetailDto> ingredients,
            Feature feature,
            List<String> excludedTitles,
            List<String> dislikedIngredients
    ) {
        return generateRecipeByPrompt(buildPrompt(ingredients, feature, excludedTitles, dislikedIngredients));
    }

    // 랜덤 신규
    public GeminiRecipeResponseDto generateRandomRecipe(
            List<IngredientDetailDto> allIngredients,
            List<String> dislikedIngredients) {
        String prompt = buildRandomPrompt(allIngredients, dislikedIngredients, List.of());
        return generateRecipeByPrompt(prompt, RANDOM_MIN_SELECT_COUNT);
    }

    // 랜덤 재요청
    public GeminiRecipeResponseDto generateRandomRecipeWithExclusion(
            List<IngredientDetailDto> allIngredients,
            List<String> dislikedIngredients,
            List<String> excludedTitles) {
        String prompt = buildRandomPrompt(allIngredients, dislikedIngredients, excludedTitles);
        return generateRecipeByPrompt(prompt, RANDOM_MIN_SELECT_COUNT);
    }

    // 레시피 생성
    public GeminiRecipeResponseDto generateRecipeByPrompt(String prompt) {
        try {

            log.info("========== Gemini 요청 시작 ==========");
            log.info("Gemini model = {}", model);
            log.info("Prompt length = {}", prompt.length());
            log.debug("Prompt 내용 = \n{}", prompt);

            GeminiRecipeRequestDto requestBody = GeminiRecipeRequestDto.from(prompt);

            log.info("Gemini requestBody 생성 완료");
            log.debug("Gemini requestBody JSON = {}", objectMapper.writeValueAsString(requestBody));

            log.info("Gemini API 호출 시작");

            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("generativelanguage.googleapis.com")
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new WebClientResponseException(
                                                    clientResponse.statusCode().value(),
                                                    "Gemini 5xx error: " + body,
                                                    null, null, null
                                            )
                                    ))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(2))
                                    .maxBackoff(Duration.ofSeconds(5))
                                    .filter(this::isRetryableError)
                                    .doBeforeRetry(retrySignal ->
                                            log.warn("Gemini 재시도 중... attempt={}, cause={}",
                                                    retrySignal.totalRetries() + 1,
                                                    retrySignal.failure().getMessage())
                                    )
                    )
                    .block();

            log.info("Gemini API 응답 수신 완료");
            log.info("Gemini raw response = {}", response);

            return parseResponse(response);

        } catch (WebClientResponseException e) {

            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Gemini 429 Rate Limit 발생 - 응답 바디: {}", e.getResponseBodyAsString());
                throw new AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
            }

            log.error("Gemini API 호출 실패 - status={}", e.getStatusCode(), e);
            throw new AppException(ErrorCode.AI_SERVER_FAILED);

        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new AppException(ErrorCode.AI_SERVER_FAILED);
        }
    }

    // 랜덤레시피 프롬프트
    public GeminiRecipeResponseDto generateRecipeByPrompt(String prompt, Integer minUserIngredients) {

        try {

            log.info("========== Gemini 요청 시작 ==========");
            log.info("Gemini model = {}", model);
            log.info("Prompt length = {}", prompt.length());
            log.debug("Prompt 내용 = \n{}", prompt);

            GeminiRecipeRequestDto requestBody = GeminiRecipeRequestDto.from(prompt, minUserIngredients);

            log.info("Gemini requestBody 생성 완료");
            log.debug("Gemini requestBody JSON = {}", objectMapper.writeValueAsString(requestBody));

            log.info("Gemini API 호출 시작");

            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("generativelanguage.googleapis.com")
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new WebClientResponseException(
                                                    clientResponse.statusCode().value(),
                                                    "Gemini 5xx error: " + body,
                                                    null, null, null
                                            )
                                    ))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(2))
                                    .maxBackoff(Duration.ofSeconds(5))
                                    .filter(this::isRetryableError)
                                    .doBeforeRetry(retrySignal ->
                                            log.warn("Gemini 재시도 중... attempt={}, cause={}",
                                                    retrySignal.totalRetries() + 1,
                                                    retrySignal.failure().getMessage())
                                    )
                    )
                    .block();

            log.info("Gemini API 응답 수신 완료");
            log.info("Gemini raw response = {}", response);

            return parseResponse(response);

        } catch (WebClientResponseException e) {

            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Gemini 429 Rate Limit 발생 - 응답 바디: {}", e.getResponseBodyAsString());
                throw new AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
            }

            log.error("Gemini API 호출 실패 - status={}", e.getStatusCode(), e);
            throw new AppException(ErrorCode.AI_SERVER_FAILED);

        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new AppException(ErrorCode.AI_SERVER_FAILED);
        }

    }

    // Gemini 응답 파싱 (DTO에 맞게)
    private GeminiRecipeResponseDto parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode textNode = root
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
            }

            // 펜스가 있으면 제거 (에러 방지)
            String rawText = textNode.asText().trim();
            if (rawText.startsWith("```")) {
                rawText = rawText
                        .replaceAll("```json\\n?", "")
                        .replaceAll("\\n?```", "")
                        .trim();
            }

            log.info("[Gemini Parsed JSON]\n{}", rawText);

            GeminiRecipeResponseDto result = objectMapper.readValue(rawText, GeminiRecipeResponseDto.class);

            // 필수 필드 검증
            validateRecipeResponse(result);

            return result;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("응답 파싱 실패", e);
            throw new AppException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    private void validateRecipeResponse(GeminiRecipeResponseDto response) {
        if (response.getTitle() == null || response.getTitle().isBlank()) {
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
        }
        if (response.getIngredients() == null) {
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
        }
        if (response.getSteps() == null || response.getSteps().isEmpty()) {
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
        }
    }

    // 일반 프롬프트 생성
    private String buildPrompt(
            List<IngredientDetailDto> ingredients,
            //Difficulty difficulty,
            Feature feature,
            List<String> excludedTitles,
            List<String> dislikedIngredients) {

        // 재료 리스트 (이름 + 단위만)
        String ingredientsJson = ingredients.stream()
                .map(i -> String.format(
                        "{\"ingredientId\":%d,\"name\":\"%s\",\"unit\":\"%s\"}",
                        i.getIngredientId(), i.getName(), i.getUnit()))
                .collect(Collectors.joining(",", "[", "]"));

        // 레시피 재요청 시 이전 요리 제외 (첫 요청은 제외)
        String exclusionBlock = (excludedTitles == null || excludedTitles.isEmpty()) ? "" :
                "\n[제외할 요리]\n" +
                        excludedTitles.stream().map(t -> "- " + t).collect(Collectors.joining("\n")) +
                        "\n위 요리와 겹치지 않는 새로운 레시피를 추천하세요.\n";

        // 못 먹는 재료 제외
        String dislikedBlock = (dislikedIngredients == null || dislikedIngredients.isEmpty()) ? "" :
                "\n[절대 사용 금지 재료]\n" +
                        dislikedIngredients.stream().map(t -> "- " + t).collect(Collectors.joining("\n")) +
                        "\n위 재료는 additional_ingredients, optional_ingredients 어디에도 절대 포함하지 마세요.\n";

        // feature가 ANY(아무거나)이면 종류 제한 없음, 그 외엔 해당 종류로 제한
        String featureBlock = (feature == null || feature == Feature.ANY)
                ? "\n[요리 종류] 제한 없음 (어떤 종류의 요리도 추천 가능)\n"
                : "\n[요리 종류] " + feature.getDisplayName() + " 종류의 레시피를 추천하세요.\n";

        return "당신은 요리 레시피 전문가입니다.\n\n" +
                featureBlock +
                exclusionBlock + dislikedBlock +
                "\n[재료 구성 규칙]\n" +
                // 규칙 1: user_ingredients — 원본 데이터 그대로 사용
                "1. user_ingredients의 ingredientId, name, unit은 아래 데이터 값 그대로 사용하세요. quantity는 0보다 큰 양수로 생성하세요.\n" +
                // 규칙 2: additional_ingredients — 레시피에 필요한 추가 재료 (필수/선택/대체 포함)
                "2. additional_ingredients는 레시피에 필요한 추가 재료 목록입니다. 필수 재료뿐 아니라 없어도 되는 재료나 다른 재료로 대체 가능한 재료도 모두 이 목록에 먼저 추가하세요.\n" +
                // 규칙 3: optional_ingredients — additional_ingredients에서만 선택
                "3. optional_ingredients는 additional_ingredients에 이미 추가한 재료 중 일부를 선택해서 '생략 가능' 또는 '대체 가능' 여부를 표시하는 목록입니다. " +
                "additional_ingredients에 없는 새로운 재료를 이 목록에 추가하는 것은 절대 금지합니다. " +
                "description은 \"이 재료는 [대체재료]로 대체 가능합니다\" 또는 \"이 재료는 생략 가능합니다\" 중 하나로만 작성하세요.\n" +
                "4. youtube_search_queries는 한국어 검색어 1~3개를 작성하세요.\n" +
                "5. steps는 단계별 조리 방법을 작성하세요.\n\n" +
                "[user_ingredients]\n" +
                ingredientsJson + "\n";
    }

    // 랜덤 프롬프트생성
    private String buildRandomPrompt(
            List<IngredientDetailDto> allIngredients,
            List<String> dislikedIngredients,
            List<String> excludedTitles) {

        String ingredientsJson = allIngredients.stream()
                .map(i -> String.format(
                        "{\"ingredientId\":%d,\"name\":\"%s\",\"unit\":\"%s\"}",
                        i.getIngredientId(), i.getName(), i.getUnit()))
                .collect(Collectors.joining(",", "[", "]"));

        String exclusionBlock = (excludedTitles == null || excludedTitles.isEmpty()) ? "" :
                "\n[제외할 요리]\n" +
                        excludedTitles.stream().map(t -> "- " + t).collect(Collectors.joining("\n")) +
                        "\n위 요리와 겹치지 않는 새로운 레시피를 추천하세요.\n";

        String dislikedBlock = (dislikedIngredients == null || dislikedIngredients.isEmpty()) ? "" :
                "\n[절대 사용 금지 재료]\n" +
                        dislikedIngredients.stream().map(t -> "- " + t).collect(Collectors.joining("\n")) +
                        "\n위 재료는 additional_ingredients, optional_ingredients 어디에도 절대 포함하지 마세요.\n";

        return "당신은 요리 레시피 전문가입니다.\n\n" +
                "[요리 종류] 제한 없음 (어떤 종류의 요리도 추천 가능)\n" +
                exclusionBlock + dislikedBlock +
                "\n[재료 선택 규칙]\n" +
                "1. 아래 전체 보유 재료 목록 중에서 서로 조합이 좋은 재료를 최소 " + RANDOM_MIN_SELECT_COUNT + "개 이상 선택하세요.\n" +
                "2. 선택한 재료의 ingredientId, name, unit은 반드시 아래 데이터 값 그대로 사용하세요. 목록에 없는 재료를 만들어내지 마세요.\n" +
                "3. additional_ingredients는 레시피에 필요한 추가 재료 목록입니다. 필수 재료뿐 아니라 없어도 되는 재료나 다른 재료로 대체 가능한 재료도 모두 이 목록에 먼저 추가하세요.\n" +
                // 규칙 3: optional_ingredients — additional_ingredients에서만 선택
                "4. optional_ingredients는 additional_ingredients에 이미 추가한 재료 중 일부를 선택해서 '생략 가능' 또는 '대체 가능' 여부를 표시하는 목록입니다. " +
                "additional_ingredients에 없는 새로운 재료를 이 목록에 추가하는 것은 절대 금지합니다. " +
                "description은 \"이 재료는 [대체재료]로 대체 가능합니다\" 또는 \"이 재료는 생략 가능합니다\" 중 하나로만 작성하세요.\n" +
                "5. youtube_search_queries는 한국어 검색어 1~3개를 작성하세요.\n" +
                "6. steps는 단계별 조리 방법을 작성하세요.\n\n" +
                "[user_ingredients]\n" +
                ingredientsJson + "\n";

    }

    private boolean isRetryableError(Throwable e) {
        if (e instanceof java.util.concurrent.TimeoutException) {
            return true;
        }
        if (e instanceof WebClientResponseException webEx) {
            return webEx.getStatusCode().is5xxServerError();
        }
        return false;
    }
}
