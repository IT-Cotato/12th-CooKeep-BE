package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeRequestDto;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.IngredientDetailDto;
import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
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

import java.net.URI;
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

    public GeminiRecipeResponseDto generateRecipe(
            List<IngredientDetailDto> ingredients,
            Difficulty difficulty) {
        return generateRecipeByPrompt(buildPrompt(ingredients, difficulty, List.of()));
    }

    public GeminiRecipeResponseDto generateRecipeWithExclusion(
            List<IngredientDetailDto> ingredients,
            Difficulty difficulty,
            List<String> excludedTitles
    ) {
        return generateRecipeByPrompt(buildPrompt(ingredients, difficulty, excludedTitles));
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
                    .bodyToMono(String.class)
                    .block();

            log.info("Gemini API 응답 수신 완료");
            log.info("Gemini raw response = {}", response);

            return parseResponse(response);

        } catch (WebClientResponseException e) {

            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Gemini 429 Rate Limit 발생");
                throw new AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
            }

            log.error("Gemini API 호출 실패 - status={}", e.getStatusCode(), e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);

        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
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

    // 프롬프트 생성
    private String buildPrompt(
            List<IngredientDetailDto> ingredients,
            Difficulty difficulty,
            List<String> excludedTitles) {

        // 재료 리스트 (이름 + 단위만)
        String ingredientList = ingredients.stream()
                .map(i -> String.format("%s (%s 단위)", i.getName(), i.getUnit()))
                .collect(Collectors.joining(", "));

        // JSON 형식 (type, referenceId, name, unit만 전달)
        String ingredientsJson = ingredients.stream()
                .map(i -> String.format(
                        "{ \"ingredientId\": %d, \"name\": \"%s\", \"unit\": \"%s\" }",
                        i.getIngredientId(), i.getName(), i.getUnit()))
                .collect(Collectors.joining(",\n    ", "[\n    ", "\n  ]"));

        // 제외할 요리 (레시피 재요청 시 이전 요리 제외)
        String exclusionBlock = "";
        if (excludedTitles != null && !excludedTitles.isEmpty()) {
            exclusionBlock = excludedTitles.stream()
                    .map(t -> "- " + t)
                    .collect(Collectors.joining("\n"));
        }

        return """
        당신은 요리 레시피 전문가입니다.
        
        [사용 가능한 재료]
        %s
        
        [난이도]
        %s
        
        [제외할 요리]
        %s
        위 요리와 겹치지 않는 새로운 레시피를 추천하세요.
        
        [규칙]
        1. user_ingredients 의 ingredientId, name, unit 은 반드시 아래 제공된 값을 그대로 사용하세요.
           각 재료의 quantity(수량)는 0보다 큰 양수로 생성하세요.
        2. additional_ingredients 는 레시피에 반드시 필요한 추가 재료만 포함하세요.
        3. optional_ingredients 는 user_ingredients 또는 additional_ingredients 에 이미 존재하는 재료만 사용하세요.
           description 은 아래 두 형식 중 하나로만 작성하세요:
           - "이 재료는 [대체재료]로 대체 가능합니다"  (대체재료는 반드시 목록에 존재하는 재료여야 함)
           - "이 재료는 생략 가능합니다"
        4. youtube_search_queries 는 이 레시피를 검색할 한국어 검색어 1~3개를 작성하세요.
        5. steps 는 간결하고 실용적인 단계별 조리 방법을 작성하세요.

        [user_ingredients 기준 데이터]
        %s
        """.formatted(ingredientList, difficulty.name(), exclusionBlock, ingredientsJson);
    }
}
