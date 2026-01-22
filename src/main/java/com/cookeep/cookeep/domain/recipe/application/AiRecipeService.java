package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiMessageRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiRecipeRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiSessionRepository;
import com.cookeep.cookeep.domain.recipe.dto.*;
import com.cookeep.cookeep.domain.recipe.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiRecipeService {

    private static final int MAX_CHANGE_COUNT = 5;

    private final GeminiService geminiService;
    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiRecipeRepository aiRecipeRepository;
    private final ObjectMapper objectMapper;
    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;

    public AiRecipeResponseDto generateRecipe(
            Long userId,
            AiRecipeRequestDto request
    ) {

        // 1. 요청 검증
        validateRequest(request);

        // 2. 세션 조회 or 생성
        AiSession session = getOrCreateSession(userId, request);

        // 3. 세션 완료 여부 확인
        if (session.getIsCompleted()) {
            throw new AppException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }

        // 4. 변경 횟수 제한 체크
        if (session.isChangeLimitExceeded(MAX_CHANGE_COUNT)) {
            throw new AppException(ErrorCode.AI_RECIPE_CHANGE_LIMIT_EXCEEDED);
        }

        // 5. 시도 횟수 증가
        session.incrementAttemptNumber();

        // 6. USER 프리셋 메시지 저장
        MessageType messageType = (session.getAttemptNumber() == 1)
                ? MessageType.INITIAL_REQUEST
                : MessageType.RETRY_REQUEST;
        saveUserMessage(session, messageType);

        // 7. 재료 이름 enrichment
        List<IngredientDetailDto> enrichedIngredients =
                enrichIngredientsWithNames(request.getIngredients());

        // 8. Gemini 호출
        GeminiRecipeResponseDto geminiResponse =
                geminiService.generateRecipe(
                        enrichedIngredients,
                        request.getDifficulty()
                );

        // 9. AI 메시지 저장
        saveAiMessage(session, geminiResponse);

        // 10. 세션 제목 갱신
        session.updateTitle(geminiResponse.getTitle());

        return AiRecipeResponseDto.builder()
                .sessionId(session.getId())
                .changeCount(session.getAttemptNumber())
                .recipe(geminiResponse)
                .build();
    }

    public AiRecipeAdoptResponseDto adoptRecipe(Long userId, Long sessionId) {
        // 1. 세션 조회
        AiSession session = aiSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        // 2. 이미 채택된 세션인지 확인
        if (session.getIsCompleted()) {
            throw new AppException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }

        // 3. 마지막 AI 메시지 조회 (채택할 레시피)
        AiMessage lastAiMessage = getLastAiMessage(session);

        // 4. AI 메시지를 레시피로 파싱
        GeminiRecipeResponseDto recipe = parseAiMessageToRecipe(lastAiMessage.getContent());

        // 5. ai_recipes 테이블에 저장
        AiRecipe savedRecipe = saveAdoptedRecipe(session, recipe, userId);

        // 6. USER 채택 메시지 저장
        saveUserMessage(session, MessageType.ADOPT_RECIPE);

        // 7. 세션 완료 처리
        session.complete();

        // 8. TODO: 쿠키 1개 지급 (다른 팀원 기능과 연동 예정)
        // cookieService.grantCookie(userId, 1);

        // 9. TODO: 재료 섭취 완료 처리
        // ingredientConsumptionService.consumeIngredients(userId, recipe);

        return AiRecipeAdoptResponseDto.builder()
                .sessionId(session.getId())
                .recipeId(savedRecipe.getId())
                .message("레시피가 성공적으로 채택되었습니다.")
                .completedAt(session.getCompletedAt())
                .build();
    }

    private void validateRequest(AiRecipeRequestDto request) {
        // 재료 목록 검증
        if (request.getIngredients() == null || request.getIngredients().isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENTS_REQUIRED);
        }

        // 난이도 검증
        if (request.getDifficulty() == null) {
            throw new AppException(ErrorCode.INVALID_DIFFICULTY);
        }
    }

    // 내부 메서드
    private AiSession getOrCreateSession(Long userId, AiRecipeRequestDto request) {

        if (request.getSessionId() == null) {
            // 재료 ID 목록을 JSON으로 변환
            String ingredientIdsJson = convertIngredientIdsToJson(request.getIngredients());

            return aiSessionRepository.save(
                    AiSession.builder()
                            .userId(userId)
                            .difficulty(request.getDifficulty())
                            .attemptNumber(0)
                            .isPinned(false)
                            .isCompleted(false)
                            .userIngredientIds(ingredientIdsJson)
                            .build()
            );
        }

        return aiSessionRepository.findByIdAndUserId(
                        request.getSessionId(), userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.AI_SESSION_NOT_FOUND));
    }

    private String convertIngredientIdsToJson(List<IngredientDetailDto> ingredients) {
        try {
            List<Long> ids = ingredients.stream()
                    .map(IngredientDetailDto::getReferenceId)
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            log.error("재료 ID JSON 변환 실패", e);
            return "[]";
        }
    }

    private void saveUserMessage(AiSession session, MessageType messageType) {
        aiMessageRepository.save(
                AiMessage.builder()
                        .session(session)
                        .role(Role.USER)
                        .content(messageType.getDescription())
                        .build()
        );
    }

    private void saveAiMessage(
            AiSession session,
            GeminiRecipeResponseDto response
    ) {
        try {
            String json = objectMapper.writeValueAsString(response);

            aiMessageRepository.save(
                    AiMessage.builder()
                            .session(session)
                            .role(Role.AI)
                            .content(json)
                            .build()
            );
        } catch (Exception e) {
            log.error("AI 메시지 저장 실패", e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        }
    }

    private AiMessage getLastAiMessage(AiSession session) {
        return aiMessageRepository.findTopBySessionAndRoleOrderByCreatedAtDesc(session, Role.AI)
                .orElseThrow(() -> new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT));
    }

    private GeminiRecipeResponseDto parseAiMessageToRecipe(String content) {
        try {
            return objectMapper.readValue(content, GeminiRecipeResponseDto.class);
        } catch (Exception e) {
            log.error("AI 메시지 파싱 실패", e);
            throw new AppException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    private AiRecipe saveAdoptedRecipe(AiSession session, GeminiRecipeResponseDto recipe, Long userId) {
        try {
            String ingredientsJson = objectMapper.writeValueAsString(recipe.getIngredients());
            String stepsJson = objectMapper.writeValueAsString(recipe.getSteps());
            String youtubeUrlJson = objectMapper.writeValueAsString(recipe.getYoutubeReferences());

            AiRecipe aiRecipe = AiRecipe.builder()
                    .title(recipe.getTitle())
                    .ingredientsJson(ingredientsJson)
                    .stepsJson(stepsJson)
                    .youtubeUrlJson(youtubeUrlJson)
                    .userId(userId)
                    .session(session)
                    .build();

            return aiRecipeRepository.save(aiRecipe);
        } catch (Exception e) {
            log.error("채택 레시피 저장 실패", e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        }
    }

    // 재료 이름 채우기
    private List<IngredientDetailDto> enrichIngredientsWithNames(
            List<IngredientDetailDto> ingredients
    ) {
        return ingredients.stream()
                .map(this::enrichIngredientWithName)
                .collect(Collectors.toList());
    }

    private IngredientDetailDto enrichIngredientWithName(IngredientDetailDto dto) {
        String name = getIngredientName(dto.getType(), dto.getReferenceId());

        // DTO를 불변으로 만들거나, 빌더 패턴 사용
        return IngredientDetailDto.builder()
                .type(dto.getType())
                .referenceId(dto.getReferenceId())
                .name(name)
                .quantity(dto.getQuantity())
                .unit(dto.getUnit())
                .build();
    }

    private String getIngredientName(String type, Long referenceId) {
        if ("DEFAULT".equals(type)) {
            return defaultIngredientRepository.findById(referenceId)
                    .map(DefaultIngredient::getIngredient)
                    .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));
        } else if ("CUSTOM".equals(type)) {
            return customIngredientRepository.findById(referenceId)
                    .map(CustomIngredient::getName)
                    .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));
        }
        throw new AppException(ErrorCode.INVALID_INGREDIENT_TYPE);
    }
}
