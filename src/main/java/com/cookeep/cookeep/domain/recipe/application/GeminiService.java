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
        return generateRecipeByPrompt(buildPrompt(ingredients, difficulty));
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
                .map(i -> String.format(
                        "{ \"ingredientId\": %d, \"name\": \"%s\", \"unit\": \"%s\" }",
                        i.getIngredientId(), i.getName(), i.getUnit()))
                .collect(Collectors.joining(",\n    ", "[\n    ", "\n  ]"));

        return """
        당신은 요리 레시피 전문가입니다.
        
        [사용 가능한 재료]
        %s
        
        [난이도]
        %s
        
        [규칙]
        1. 제공된 재료의 단위를 반드시 사용하세요.
        2. 각 재료의 적절한 수량(quantity)을 생성하세요.
           - 수량은 반드시 0보다 큰 양수여야 합니다. (0은 절대 불가)
           - 소수점 첫 번째 자리까지 표현 가능합니다. 예: 0.5, 1, 1.5, 2
        3. user_ingredients에는 ingredientId, name, quantity, unit을 모두 포함하세요.
           - ingredientId는 제공된 값을 그대로 사용하세요. 절대 임의로 변경하지 마세요.
        4. additional_ingredients에는 레시피에 반드시 필요한 추가 재료만 포함하세요.
           - description 필드는 절대 생성하지 마세요.
           - description은 null로도 생성하지 말고, 아예 포함하지 마세요.
        5. optional_ingredients에는 절대 새로운 재료를 생성하지 마세요.
           - 반드시 user_ingredients 또는 additional_ingredients에 이미 존재하는 재료만 사용하세요.
           - 위 두 목록에 없는 새로운 재료를 생성하는 것은 절대 금지합니다.
           - optional_ingredients에 포함된 재료는 반드시 user_ingredients 또는 additional_ingredients 중 하나와 동일한 name을 가져야 합니다.

           - description 필드는 반드시 포함해야 합니다.
           - description은 반드시 아래 두 형식 중 하나로만 작성하세요:

             1) "이 재료는 [다른 재료]로 대체 가능합니다"
               - [다른 재료]는 반드시 user_ingredients 또는 additional_ingredients에 존재하는 실제 재료명을 사용하세요.
               - 존재하지 않는 재료명은 절대 사용하지 마세요.

             2) "이 재료는 생략 가능합니다"
             
            - 위 형식 외의 문장은 절대 생성하지 마세요.
        6. additional_ingredients와 optional_ingredients의 unit은 반드시 아래 목록 중 하나만 사용하세요:
           [개, 팩, 봉지, 병, 묶음, 캔, g, ml, 티스푼, 테이블스푼]
           - 위 목록에 없는 단위는 절대 사용하지 마세요.
           - 영어 단위(piece, cup 등)나 "적당량", "약간" 같은 표현은 금지합니다.
           - user_ingredients에는 해당 단위 제한을 적용하지 않습니다.
        7. 추가로 필요한 재료가 있다면 additional_ingredients에 포함하세요.
        8. youtube_search_queries에는 이 레시피를 유튜브에서 검색할 때 사용할 한국어 검색어를 1~3개 작성하세요.
           - 실제로 유튜브에서 검색했을 때 관련 영상이 나올만한 구체적인 표현을 사용하세요.
           - 예: "김치볶음밥 만들기", "간단한 김치볶음밥 레시피"
        9. URL이나 링크는 절대 생성하지 마세요. 검색어만 제공하세요.
        10. steps는 간결하고 실용적인 요리 방법을 단계별로 작성하세요.
        11. 반드시 JSON만 응답하세요. 설명 문구는 절대 포함하지 마세요.
        
        [응답 형식]
        {
          "title": "요리 제목",
          "ingredients": {
            "user_ingredients": %s,
            "additional_ingredients": [
              {"name": "간장", "quantity": 2.0, "unit": "GRAM"}
            ],
            "optional_ingredients": [
              {"name": "참기름", "quantity": 0.5, "unit": "GRAM"}
            ]
          },
          "steps": [
            "1. 양파를 채썬다.",
            "2. 팬에 기름을 두르고 양파를 볶는다.",
            "3. 간장으로 간을 맞춘다."
          ],
          "youtube_search_queries": [
           "김치볶음밥 만들기",
           "간단한 김치볶음밥 레시피"
         ]
        }
        """.formatted(
                ingredientList,
                difficulty.name(),
                ingredientsJson
        );
    }
}
