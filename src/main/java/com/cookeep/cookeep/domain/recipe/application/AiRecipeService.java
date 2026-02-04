package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.api.dto.request.AiRecipeRequestDto;
import com.cookeep.cookeep.api.dto.response.AiRecipeAdoptResponseDto;
import com.cookeep.cookeep.api.dto.response.AiRecipeResponseDto;
import com.cookeep.cookeep.api.dto.response.AiSessionDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.AiSessionListResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.recipe.dao.AiMessageRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiRecipeRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiSessionRepository;
import com.cookeep.cookeep.domain.recipe.dto.*;
import com.cookeep.cookeep.domain.recipe.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.cookeep.cookeep.domain.recipe.entity.MessageType.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiRecipeService {

    private static final int MAX_RETRY_COUNT = 5;

    private final GeminiService geminiService;
    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiRecipeRepository aiRecipeRepository;
    private final ObjectMapper objectMapper;
    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;

    // sessionId 유무에 따라 신규/재요청 로직 분기
    public AiRecipeResponseDto generateRecipe(Long userId, AiRecipeRequestDto request) {
        validateRequest(request);

        // sessionId가 null이면 신규 생성 (INITIAL_REQUEST)
        if (request.getSessionId() == null) {
            return generateInitialRecipe(userId, request);
        }

        // sessionId가 있으면 재요청 (RETRY_REQUEST)
        return regenerateRecipe(userId, request.getSessionId());
    }

    // 1. 새 레시피 요청
    private AiRecipeResponseDto generateInitialRecipe(Long userId, AiRecipeRequestDto request) {

        // 필수 입력 필드 검증
        if (request == null || request.getIngredients() == null || request.getIngredients().isEmpty()) {
            throw new AppException(ErrorCode.RECIPE_INGREDIENTS_REQUIRED);
        }

        if (request.getDifficulty() == null) {
            throw new AppException(ErrorCode.INVALID_DIFFICULTY);
        }

        // 1. 재료 정보 enrichment (이름 + 단위 조회)
        List<IngredientDetailDto> enrichedIngredients =
                enrichIngredientsForAI(userId, request.getIngredients());

        for (IngredientSimpleDto dto : request.getIngredients()) {
            if (dto.getType() == null) {
                throw new AppException(ErrorCode.INVALID_INGREDIENT_TYPE);
            }
        }

        // 2. 세션 생성
        AiSession session = AiSession.builder()
                .userId(userId)
                .difficulty(request.getDifficulty())
                .attemptNumber(1)
                .isCompleted(false)
                .userIngredientIds(writeIngredientsAsJson(enrichedIngredients))
                .build();
        aiSessionRepository.save(session);
        // 메시지 db에 저장
        saveInitialUserMessage(session, request);

        // 3. AI 레시피 생성 (이름 + 단위만 전달, AI가 quantity 생성)
        GeminiRecipeResponseDto aiResponse = geminiService.generateRecipe(
                enrichedIngredients,
                request.getDifficulty()
        );

        // 4. AI 메시지 저장
        saveAiMessage(session, aiResponse, MessageType.INITIAL_REQUEST);

        // 5. 세션 제목 업데이트
        updateSessionTitle(session, aiResponse);

        // 6. 응답 반환
        return AiRecipeResponseDto.builder()
                .sessionId(session.getId())
                .changeCount(session.getAttemptNumber())
                .recipe(aiResponse)
                .build();
    }

    // 2. 레시피 재요청
    public AiRecipeResponseDto regenerateRecipe(Long userId, Long sessionId) {
        // 1. 세션 조회 및 검증
        if (sessionId == null) {
            throw new AppException(ErrorCode.RECIPE_SESSIONID_REQUIRED);
        }

        AiSession session = aiSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        if (session.getIsCompleted()) {
            throw new AppException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }

        if (session.getAttemptNumber() >= MAX_RETRY_COUNT) {
            throw new AppException(ErrorCode.AI_RECIPE_CHANGE_LIMIT_EXCEEDED);
        }

        if (session.getDifficulty() == null) {
            throw new AppException(ErrorCode.SESSION_DIFFICULTY_NOT_FOUND);
        }

        // 2. 이전 재료 복원 (이미 이름 + 단위 포함)
        List<IngredientDetailDto> ingredients = readIngredientsFromSession(session);
        if (ingredients == null || ingredients.isEmpty()) {
            throw new AppException(ErrorCode.SESSION_INGREDIENTS_NOT_FOUND);
        }

        // 3. 이전 레시피 제목 목록 조회
        List<String> excludedTitles = extractRecipeTitlesFromMessages(sessionId);

        // 4. 재요청 메시지 저장 (role=USER, RETRY_REQUEST)
        saveSimpleUserMessage(session, MessageType.RETRY_REQUEST);

        // 5. AI 호출 (제외 리스트 포함)
        GeminiRecipeResponseDto aiResponse = geminiService.generateRecipeWithExclusion(
                ingredients,
                session.getDifficulty(),
                excludedTitles
        );

        // 6. 재요청 메시지 저장
        saveAiMessage(session, aiResponse, RETRY_REQUEST);

        // 7. 시도 횟수 증가 및 저장
        session.increaseAttempt();
        aiSessionRepository.save(session);

        // 8. 세션 제목 업데이트
        updateSessionTitle(session, aiResponse);

        // 9. 응답 반환
        return AiRecipeResponseDto.builder()
                .sessionId(session.getId())
                .changeCount(session.getAttemptNumber())
                .recipe(aiResponse)
                .build();
    }

    // 3. 레시피 채택
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
        saveSimpleUserMessage(session, MessageType.ADOPT_RECIPE);

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

    // (MAIN06-1) AI 대화 목록 전체 조회
    @Transactional(readOnly = true)
    public AiSessionListResponseDto getAllSessions(Long userId) {
        List<AiSession> allSessions = aiSessionRepository
                .findAllByUserIdOrderByIsPinnedDescUpdatedAtDesc(userId);

        // 즐겨찾기 대화 별도 정렬
        List<AiSessionListResponseDto.SessionSummary> pinned = allSessions.stream()
                .filter(session -> Boolean.TRUE.equals(session.getIsPinned()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .map(AiSessionListResponseDto.SessionSummary::from)
                .collect(Collectors.toList());

        // 일반 대화 정렬
        List<AiSessionListResponseDto.SessionSummary> sessions = allSessions.stream()
                .filter(session -> !Boolean.TRUE.equals(session.getIsPinned()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .map(AiSessionListResponseDto.SessionSummary::from)
                .collect(Collectors.toList());

        return AiSessionListResponseDto.builder()
                .pinned(pinned)
                .sessions(sessions)
                .build();
    }

    // (MAIN06-2) AI 대화 상세 내역 전체 조회
    @Transactional(readOnly = true)
    public AiSessionDetailResponseDto getSessionDetail(Long userId, Long sessionId) {
        // 1. 세션 조회 및 권한 검증
        AiSession session = aiSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        // 2. 세션 내 모든 메시지 조회
        List<AiMessage> messages = aiMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);

        return AiSessionDetailResponseDto.builder()
                .sessionId(session.getId())
                .messages(messages.stream()
                        .map(AiSessionDetailResponseDto.MessageItem::from)
                        .toList())
                .build();
    }

    // (MAIN06-3) AI 대화 세션 삭제
    public void deleteSession(Long userId, Long sessionId) {
        // 1. 세션 조회
        AiSession session = aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        // 2. 본인 세션인지 확인
        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.AI_SESSION_FORBIDDEN);
        }

        // 3. 연관 레시피&메시지 삭제
        aiRecipeRepository.deleteBySessionId(sessionId);
        aiMessageRepository.deleteBySessionId(sessionId);

        // 4. 세션 삭제
        aiSessionRepository.delete(session);
    }

    // (MAIN07) AI 대화 세션 즐겨찾기 추가/삭제
    public void toggleFavorite(Long userId, Long sessionId) {
        // 1. 세션 조회
        AiSession session = aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        // 2. 본인 세션 여부 체크
        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.AI_SESSION_FORBIDDEN);
        }

        // 3. 즐겨찾기 상태 변경 (T -> F / F -> T)
        if (Boolean.TRUE.equals(session.getIsPinned())) {
            session.unpin();
        } else {
            session.pin();
        }

        // 4. 저장
        aiSessionRepository.save(session);
    }

    // --- 내부 메서드 ---

    // 요청 검증
    private void validateRequest(AiRecipeRequestDto request) {
        // 신규 요청 (sessionId가 null)인 경우에만 재료와 난이도 검증
        if (request.getSessionId() == null) {
            if (request.getIngredients() == null || request.getIngredients().isEmpty()) {
                throw new AppException(ErrorCode.RECIPE_INGREDIENTS_REQUIRED);
            }
            if (request.getDifficulty() == null) {
                throw new AppException(ErrorCode.INVALID_DIFFICULTY);
            }
        }
        // 재요청은 기존 세션에서 정보 가져옴
    }

    // 요청바디에 입력한 타입,아이디로 db에서 재료 단위/이름 조회 & 수량은 AI가 생성
    private List<IngredientDetailDto> enrichIngredientsForAI(
            Long userId,
            List<IngredientSimpleDto> simpleIngredients
    ) {
        return simpleIngredients.stream()
                .map(simple -> {
                    // 1. 재료 이름 조회 (default_ingredients 또는 custom_ingredients)
                    String name = getIngredientName(simple.getType(), simple.getReferenceId());

                    // 2. 단위 조회 (user_ingredients)
                    String unit = getUserIngredientUnit(userId, simple.getType(), simple.getReferenceId());

                    // 3. DTO 생성 (quantity는 null, AI가 생성)
                    return IngredientDetailDto.builder()
                            .type(simple.getType())
                            .referenceId(simple.getReferenceId())
                            .name(name)
                            .quantity(null)
                            .unit(unit)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // user_ingredients에서 단위 조회
    private String getUserIngredientUnit(Long userId, String type, Long referenceId) {
        Type ingredientType = Type.valueOf(type);

        UserIngredient userIngredient = userIngredientRepository
                .findByUserIdAndTypeAndReferenceId(userId, ingredientType, referenceId)
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));

        return userIngredient.getUnit().name();
    }

    // AI 메시지에서 레시피 제목 추출
    private List<String> extractRecipeTitlesFromMessages(Long sessionId) {
        List<AiMessage> aiMessages = aiMessageRepository.findAllBySessionIdAndRoleAi(sessionId);

        return aiMessages.stream()
                .map(message -> {
                    try {
                        GeminiRecipeResponseDto recipe = objectMapper.readValue(
                                message.getContent(),
                                GeminiRecipeResponseDto.class
                        );
                        return recipe.getTitle();
                    } catch (Exception e) {
                        log.warn("레시피 제목 파싱 실패: {}", message.getId());
                        throw new AppException(ErrorCode.RECIPE_TITLE_PARSE_FAILED);
                    }
                })
                .filter(title -> title != null && !title.isBlank())
                .collect(Collectors.toList());
    }

    // AI 메시지 저장 (MessageType 포함)
    private void saveAiMessage(AiSession session, GeminiRecipeResponseDto response, MessageType type) {
        try {
            String json = objectMapper.writeValueAsString(response);

            AiMessage message = AiMessage.builder()
                    .session(session)
                    .role(Role.AI)
                    .messageType(type)
                    .content(json)
                    .build();

            aiMessageRepository.save(message);

        } catch (Exception e) {
            log.error("AI 메시지 저장 실패", e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        }
    }

    // 마지막 AI 메시지 조회
    private AiMessage getLastAiMessage(AiSession session) {
        return aiMessageRepository.findTopBySessionAndRoleOrderByCreatedAtDesc(session, Role.AI)
                .orElseThrow(() -> new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT));
    }

    // AI 메시지를 레시피로 파싱
    private GeminiRecipeResponseDto parseAiMessageToRecipe(String content) {
        try {
            return objectMapper.readValue(content, GeminiRecipeResponseDto.class);
        } catch (Exception e) {
            log.error("AI 메시지 파싱 실패", e);
            throw new AppException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    // 채택된 레시피 저장
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

    // 재료이름조회
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

    // 재료 JSON 형식 변환
    private String writeIngredientsAsJson(List<IngredientDetailDto> ingredients) {
        try {
            return objectMapper.writeValueAsString(ingredients);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 세션에서 재료 조회
    private List<IngredientDetailDto> readIngredientsFromSession(AiSession session) {
        try {
            return objectMapper.readValue(
                    session.getUserIngredientIds(),
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            throw new AppException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    // 유저 메시지 저장 (초기메시지)
    private void saveInitialUserMessage(AiSession session, AiRecipeRequestDto request) {
        try {
            // USER 메시지 content에 재료 + 타입 설명 저장 (요구사항 충족)
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("type", MessageType.INITIAL_REQUEST.name());
            payload.put("message", MessageType.INITIAL_REQUEST.getDescription());
            payload.put("ingredients", request.getIngredients());

            String content = objectMapper.writeValueAsString(payload);

            AiMessage userMsg = AiMessage.userMessage(session, MessageType.INITIAL_REQUEST, content);
            aiMessageRepository.save(userMsg);
            // flush해서 유저 메시지 먼저 저장
            aiMessageRepository.flush();

        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 유저 메시지 저장 (재요청/채택 메시지)
    private void saveSimpleUserMessage(AiSession session, MessageType type) {
        String content = type.getDescription();
        AiMessage userMsg = AiMessage.userMessage(session, type, content);
        aiMessageRepository.save(userMsg);
        // flush해서 유저 메시지 먼저 저장
        aiMessageRepository.flush();
    }

    // 세션 제목 업데이트
    private void updateSessionTitle(AiSession session, GeminiRecipeResponseDto aiResponse) {
        if (aiResponse == null || aiResponse.getTitle() == null) {
            throw new AppException(ErrorCode.AI_RECIPE_TITLE_MISSING);
        }

        session.setTitle(aiResponse.getTitle());
        aiSessionRepository.save(session);
    }

}
