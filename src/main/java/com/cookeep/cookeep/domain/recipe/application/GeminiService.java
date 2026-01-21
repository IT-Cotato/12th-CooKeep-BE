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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    public GeminiRecipeResponseDto generateRecipe(List<IngredientDetailDto> ingredients, Difficulty difficulty) {
        String prompt = buildPrompt(ingredients, difficulty);
        return generateRecipeByPrompt(prompt);
    }

    // 레시피 생성
    public GeminiRecipeResponseDto generateRecipeByPrompt(String prompt) {
        try {
            GeminiRecipeRequestDto requestBody = GeminiRecipeRequestDto.from(prompt);

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

            log.info("Gemini raw response = {}", response);

            return parseResponse(response);

        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        }
    }

    // Gemini 응답 파싱 (DTO에 맞게)
    private GeminiRecipeResponseDto parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode textNode = root
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode() || textNode.isNull()) {
            throw new IllegalStateException("Gemini 응답에 text 필드가 없습니다");
        }

        String rawText = textNode.asText().trim();

        // ```json 제거
        String cleanedJson = rawText
                .replaceAll("```json\\n?", "")
                .replaceAll("\\n?```", "")
                .trim();

        log.info("[Gemini Parsed JSON]\n{}", cleanedJson);

        // ✅ 너가 만든 GeminiRecipeResponseDto 구조로 변환
        return objectMapper.readValue(cleanedJson, GeminiRecipeResponseDto.class);
    }

    /**
     * 프롬프트 생성
     */
    private String buildPrompt(List<IngredientDetailDto> ingredients, Difficulty difficulty) {

        String ingredientList = ingredients.stream()
                .map(i -> String.format("%s %d%s",
                        i.getName(),
                        i.getQuantity(),
                        i.getUnit()))
                .collect(Collectors.joining(", "));

        String ingredientsJson = ingredients.stream()
                .map(i -> String.format("""
                        {
                          "type": "%s",
                          "referenceId": %d,
                          "name": "%s",
                          "quantity": %d,
                          "unit": "%s"
                        }
                        """,
                        i.getType(),
                        i.getReferenceId(),
                        i.getName(),
                        i.getQuantity(),
                        i.getUnit()))
                .collect(Collectors.joining(",", "[", "]"));

        return """
            당신은 요리 레시피 전문가입니다.
            
            [사용 가능한 재료]
            %s
            
            [난이도]
            %s
            
            [규칙]
            1. 제공된 재료의 단위와 수량을 그대로 사용하세요.
            2. 제공된 재료보다 많은 양을 사용하면 안 됩니다.
            3. user_ingredients에는 type과 referenceId를 반드시 포함하세요.
            4. 반드시 JSON만 응답하세요. 설명 문구는 절대 포함하지 마세요.
            
            [응답 형식]
            {
              "title": "요리 제목",
              "ingredients": {
                "user_ingredients": %s,
                "additional_ingredients": [],
                "optional_ingredients": []
              },
              "steps": [
                "1. ...",
                "2. ..."
              ]
            }
            """.formatted(
                ingredientList,
                difficulty.name(),
                ingredientsJson
        );
    }
}
