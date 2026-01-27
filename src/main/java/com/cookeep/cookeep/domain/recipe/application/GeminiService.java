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

    public GeminiRecipeResponseDto generateRecipe(
            List<IngredientDetailDto> ingredients,
            Difficulty difficulty) {
        String prompt = buildPrompt(ingredients, difficulty);
        return generateRecipeByPrompt(prompt);
    }

    public GeminiRecipeResponseDto generateRecipeWithExclusion(
            List<IngredientDetailDto> ingredients,
            Difficulty difficulty,
            List<String> excludedTitles
    ) {
        String prompt = buildPrompt(ingredients, difficulty);

        if (!excludedTitles.isEmpty()) {
            prompt += "\n\n이전에 추천한 다음 요리는 제외하고 추천해줘:\n";
            for (String title : excludedTitles) {
                prompt += "- " + title + "\n";
            }
        }

        prompt += "\n기존 요리와 겹치지 않는 새로운 레시피를 추천해줘.";

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
    private GeminiRecipeResponseDto parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode textNode = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
            }

            String rawText = textNode.asText().trim();

            String cleanedJson = rawText
                    .replaceAll("```json\\n?", "")
                    .replaceAll("\\n?```", "")
                    .trim();

            log.info("[Gemini Parsed JSON]\n{}", cleanedJson);

            GeminiRecipeResponseDto result = objectMapper.readValue(cleanedJson, GeminiRecipeResponseDto.class);

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
    private String buildPrompt(List<IngredientDetailDto> ingredients, Difficulty difficulty) {

        // 재료 리스트 (이름 + 단위만)
        String ingredientList = ingredients.stream()
                .map(i -> String.format("%s (%s 단위 사용)",
                        i.getName(),
                        i.getUnit()))
                .collect(Collectors.joining(", "));

        // JSON 형식 (type, referenceId, name, unit만 전달)
        String ingredientsJson = ingredients.stream()
                .map(i -> String.format("""
                    {
                      "type": "%s",
                      "referenceId": %d,
                      "name": "%s",
                      "unit": "%s"
                    }
                    """,
                        i.getType(),
                        i.getReferenceId(),
                        i.getName(),
                        i.getUnit()))
                .collect(Collectors.joining(",", "[", "]"));

        return """
        당신은 요리 레시피 전문가입니다.
        
        [사용 가능한 재료]
        %s
        
        [난이도]
        %s
        
        [규칙]
        1. 제공된 재료의 단위를 반드시 사용하세요.
        2. 각 재료의 적절한 수량(quantity)을 생성하세요. 수량은 0개 이상이어야 하며, 소수점 첫 번째 자리까지 표현 가능합니다.
        3. user_ingredients에는 type, referenceId, name, quantity, unit을 모두 포함하세요.
        4. 추가로 필요한 재료가 있다면 additional_ingredients에 포함하세요.
        5. 생략 가능하거나 대체 가능한 재료는 optional_ingredients에 포함하세요.
        6. 레시피와 관련된 실제 유튜브 동영상 1-3개를 추천해주세요.
        7. 유튜브 인네일 URL은 https://img.youtube.com/vi/{VIDEO_ID}/default.jpg 형식을 사용하세요.
        8. 반드시 JSON만 응답하세요. 설명 문구는 절대 포함하지 마세요.
        9. steps는 간결하고 실용적인 요리 방법을 단계별로 작성하세요.
        
        [응답 형식]
        {
          "title": "요리 제목",
          "ingredients": {
            "user_ingredients": %s,
            "additional_ingredients": [
              {"name": "간장", "quantity": 2, "unit": "TABLESPOON"}
            ],
            "optional_ingredients": [
              {"name": "참기름", "quantity": 1, "unit": "TEASPOON"}
            ]
          },
          "steps": [
            "1. 양파를 채썬다.",
            "2. 팬에 기름을 두르고 양파를 볶는다.",
            "3. 간장으로 간을 맞춘다."
          ],
          "youtube_references": [
            {
              "title": "유튜브 영상 제목",
              "url": "https://www.youtube.com/watch?v=VIDEO_ID",
              "thumbnail": "https://img.youtube.com/vi/VIDEO_ID/default.jpg"
            }
          ]
        }
        """.formatted(
                ingredientList,
                difficulty.name(),
                ingredientsJson
        );
    }
}
