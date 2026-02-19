package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.api.dto.request.AiRecipeRequestDto;
import com.cookeep.cookeep.api.dto.response.AiRecipeAdoptResponseDto;
import com.cookeep.cookeep.api.dto.response.AiRecipeResponseDto;
import com.cookeep.cookeep.api.dto.response.AiSessionDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.AiSessionListResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.dao.CookieLogRepository;
import com.cookeep.cookeep.domain.cookie.dao.DailyCookieGrantRepository;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.cookie.entity.DailyCookieGrant;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.application.ConsumptionReportService;
import com.cookeep.cookeep.domain.recipe.dao.AiMessageRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiRecipeRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiSessionRepository;
import com.cookeep.cookeep.domain.recipe.dto.*;
import com.cookeep.cookeep.domain.recipe.entity.*;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private static final int URGENT = 0;

    private final GeminiService geminiService;
    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiRecipeRepository aiRecipeRepository;
    private final ObjectMapper objectMapper;
    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;
    private final CookieService cookieService;
    private final YoutubeSearchService youtubeSearchService;
    private final ConsumptionReportService consumptionReportService;
    private final DailyRecipeRepository dailyRecipeRepository;

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

        // 0. 필수 입력 필드 검증
        if (request == null || request.getIngredientIds() == null || request.getIngredientIds().isEmpty()) {
            throw new AppException(ErrorCode.RECIPE_INGREDIENTS_REQUIRED);
        }

        if (request.getDifficulty() == null) {
            throw new AppException(ErrorCode.INVALID_DIFFICULTY);
        }

        // 1. 해당 유저의 재료 조회
        List<UserIngredient> userIngredients = userIngredientRepository
                .findAllByIngredientIdInAndUser_UserId(request.getIngredientIds(), userId);

        if (userIngredients.isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        if (userIngredients.size() != request.getIngredientIds().size()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        // 2. 재료 정보를 IngredientDetailDto로 변환
        List<IngredientDetailDto> enrichedIngredients =
                enrichIngredientsFromUserIngredients(userIngredients);

        // 3. 유통기한 임박 식재료 포함 여부 확인
        boolean hasUrgent = userIngredients.stream()
                .anyMatch(ui -> ui.getLeftDays() == 0);

        // 4. 세션 생성
        AiSession session = AiSession.builder()
                .userId(userId)
                .difficulty(request.getDifficulty())
                .attemptNumber(1)
                .isCompleted(false)
                .userIngredientIds(writeEnrichedIngredientsAsJson(enrichedIngredients))
                .build();
        aiSessionRepository.save(session);

        // 입력한 재료 저장 (채택시 차감)
        session.setIngredientIdsJson(writeIngredientIdsAsJson(request.getIngredientIds()));

        // 메시지 db에 저장
        saveInitialUserMessage(session, request);

        // 3. AI 레시피 생성 (이름 + 단위만 전달, AI가 quantity 생성)
        GeminiRecipeResponseDto aiResponse = geminiService.generateRecipe(
                enrichedIngredients,
                request.getDifficulty()
        );

        // 4. 세션 제목 업데이트
        updateSessionTitle(session, aiResponse);

        // 5. 유튜브 검색어로 실제 영상 조회
        List<YoutubeReferenceDto> youtubeReferences =
                youtubeSearchService.searchVideos(aiResponse.getYoutubeSearchQueries());

        // 6. 레시피 저장
        saveAiMessageWithYoutubeReferences(session, aiResponse, youtubeReferences, MessageType.INITIAL_REQUEST);


        // 7. 응답 반환
        return AiRecipeResponseDto.builder()
                .sessionId(session.getId())
                .changeCount(session.getAttemptNumber())
                .recipe(aiResponse)
                .youtubeReferences(youtubeReferences)
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

        // 6. 시도 횟수 증가 및 저장
        session.increaseAttempt();
        aiSessionRepository.save(session);

        // 7. 세션 제목 업데이트
        updateSessionTitle(session, aiResponse);

        // 8. 유튜브 검색어로 실제 영상 조회
        List<YoutubeReferenceDto> youtubeReferences =
                youtubeSearchService.searchVideos(aiResponse.getYoutubeSearchQueries());

        // 9. 재요청 레시피 저장
        saveAiMessageWithYoutubeReferences(session, aiResponse, youtubeReferences, MessageType.RETRY_REQUEST);


        // 10. 응답 반환
        return AiRecipeResponseDto.builder()
                .sessionId(session.getId())
                .changeCount(session.getAttemptNumber())
                .recipe(aiResponse)
                .youtubeReferences(youtubeReferences)
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

        String rawContent = getLastAiMessage(session).getContent();
        AiRecipe savedRecipe = saveAdoptedRecipe(session, rawContent, userId);

        // 5. USER 채택 메시지 저장
        saveSimpleUserMessage(session, MessageType.ADOPT_RECIPE);

        // 6. 세션 완료 처리
        session.complete();

        // 7. 세션의 ingredientIds 조회
        List<Long> ingredientIds;
        try {
            ingredientIds = objectMapper.readValue(
                    session.getIngredientIdsJson(),
                    new TypeReference<List<Long>>() {}
            );
        } catch (Exception e) {
            log.error("❌ingredientIdsJson 파싱 실패", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 8. 임박 재료(leftDays=0) 포함 세션이면 쿠키 지급 (하루 1회)
        grantUrgentCookieRewardIfEligible(userId, ingredientIds);

        // 9. 요청 재료 전체 수량 -1 (단위 무관, 0이 되면 삭제)
        consumeIngredients(userId, ingredientIds);

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
                .isCompleted(Boolean.TRUE.equals(session.getIsCompleted()))
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

        // 데일리레시피 등록 여부 검사
        if (dailyRecipeRepository.existsByAiRecipe_Session_Id(sessionId)) {
            throw new AppException(ErrorCode.RECIPE_DELETE_NOT_ALLOWED);
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

    // 세션 제목 수정
    public void updateSessionTitle(Long userId, Long sessionId, String newTitle) {

        // 빈 제목 방지
        if (newTitle == null || newTitle.isBlank()) {
            throw new AppException(ErrorCode.TITLE_INVALID_VALUE);
        }

        // 100자 이상 제목 방지
        if (newTitle.trim().length() > 100) {
            throw new AppException(ErrorCode.TITLE_TOO_LONG);
        }

        AiSession session = aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.AI_SESSION_FORBIDDEN);
        }

        session.setTitle(newTitle.trim());
        aiSessionRepository.save(session);

    }

    // --- 내부 메서드 ---

    // 요청 검증
    private void validateRequest(AiRecipeRequestDto request) {
        // 신규 요청 (sessionId가 null)인 경우에만 재료와 난이도 검증
        if (request.getSessionId() == null) {
            if (request.getIngredientIds() == null || request.getIngredientIds().isEmpty()) {
                throw new AppException(ErrorCode.RECIPE_INGREDIENTS_REQUIRED);
            }
            if (request.getDifficulty() == null) {
                throw new AppException(ErrorCode.INVALID_DIFFICULTY);
            }
        }
        // 재요청은 기존 세션에서 정보 가져옴
    }

    // UserIngredient 엔티티에서 IngredientDetailDto로 변환
    private List<IngredientDetailDto> enrichIngredientsFromUserIngredients(
            List<UserIngredient> userIngredients
    ) {
        return userIngredients.stream()
                .map(ui -> {
                    // user_ingredient에서 참조하는 default 또는 custom ingredient의 이름 가져오기
                    String name = getIngredientName(ui.getType().name(), ui.getReferenceId());

                    return IngredientDetailDto.builder()
                            .ingredientId(ui.getIngredientId())
                            .name(name)
                            .quantity(null) // AI가 생성
                            .unit(ui.getUnit().getDisplayName())
                            .build();
                })
                .collect(Collectors.toList());
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

    // 마지막 AI 메시지 조회
    private AiMessage getLastAiMessage(AiSession session) {
        return aiMessageRepository.findTopBySessionAndRoleOrderByCreatedAtDesc(session, Role.AI)
                .orElseThrow(() -> new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT));
    }

    // 채택된 레시피 저장
    private AiRecipe saveAdoptedRecipe(AiSession session, String rawAiContent, Long userId) {
        try {
            JsonNode root = objectMapper.readTree(rawAiContent);
            String title = root.get("title").asText();
            String ingredientsJson =
                    objectMapper.writeValueAsString(root.get("ingredients"));
            String stepsJson =
                    objectMapper.writeValueAsString(root.get("steps"));
            String youtubeUrlJson =
                    objectMapper.writeValueAsString(root.get("youtube_references"));
            String youtubeSearchQueriesJson =
                    objectMapper.writeValueAsString(root.get("youtube_search_queries"));

            AiRecipe aiRecipe = AiRecipe.builder()
                    .title(title)
                    .ingredientsJson(ingredientsJson)
                    .stepsJson(stepsJson)
                    .youtubeUrlJson(youtubeUrlJson)
                    .userId(userId)
                    .session(session)
                    .build();

            aiRecipe.setYoutubeSearchQueries(youtubeSearchQueriesJson);

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
    private String writeIngredientIdsAsJson(List<Long> ingredientIds) {
        try {
            return objectMapper.writeValueAsString(ingredientIds);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String writeEnrichedIngredientsAsJson(List<IngredientDetailDto> enrichedIngredients) {
        try {
            return objectMapper.writeValueAsString(enrichedIngredients);
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
            payload.put("ingredients", request.getIngredientIds());

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

    // 임박 식재료 레시피 채택 시 리워드 지급
    private void grantUrgentCookieRewardIfEligible(Long userId, List<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return;
        }

        // 세션에 요청된 재료 중 leftDays == 0인 항목이 있는지 확인
        List<UserIngredient> targets = userIngredientRepository
                .findAllByIngredientIdInAndUser_UserId(ingredientIds, userId);

        if (targets.isEmpty()) {
            return;
        }

        boolean hasUrgent = targets.stream()
                .anyMatch(ui -> ui.getLeftDays() == URGENT);

        if (!hasUrgent) {
            return;
        }

        CookieLog.CookieLogType urgentType =
                CookieLog.CookieLogType.BONUS_URGENT_INGREDIENT_USE;

        // 1일 1회 제한 + DailyCookieGrant 자동 기록
        boolean granted = cookieService.grantDailyCookie(userId, urgentType);

        if (granted) {
            log.info("임박 재료 쿠키 {}개 지급 완료 - userId={}",
                    urgentType.getDefaultAmount(), userId);
        } else {
            log.info("임박 재료 쿠키 오늘 이미 지급됨 - userId={}", userId);
        }

    }

    // 재료 차감 로직. (단위 상관없이 무조건 -1)
    private void consumeIngredients(Long userId, List<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            log.info("재료 차감 생략 - ingredientIds 없음: userId={}", userId);
            return;
        }

        List<UserIngredient> targets = userIngredientRepository
                .findAllByIngredientIdInAndUser_UserId(ingredientIds, userId);

        if (targets.isEmpty()) {
            log.warn("재료 차감 생략 - 유효한 재료 없음: userId={}", userId);
            return;
        }

        // 주간 소비 리포트 기록 (차감/삭제 전에 호출해야 leftDays 읽기 가능)
        consumptionReportService.markConsumed(userId, targets);

        for (UserIngredient ui : targets) {
            int currentQty = ui.getQuantity();
            if (currentQty > 1) {
                ui.updateQuantity(currentQty - 1);
                log.info("재료 수량 차감: ingredientId={}, {} → {}",
                        ui.getIngredientId(), currentQty, currentQty - 1);
            } else {
                // quantity == 1이면 -1 = 0, 섭취 완료로 삭제
                userIngredientRepository.delete(ui);
                log.info("재료 삭제(수량 0): ingredientId={}", ui.getIngredientId());
            }
        }
    }

    // AI 응답 에러 확인
    private void validateAiResponse(GeminiRecipeResponseDto response) {

        if (response == null) {
            log.error("❌ AI 응답 자체가 null입니다.");
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
        }

        if (response.getIngredients() == null) {
            log.error("❌ ingredients가 null입니다.");
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
        }

        var ingredients = response.getIngredients();

        List<String> allowedUnits = List.of(
                "개", "팩", "봉지", "병", "묶음", "캔", "g", "ml", "티스푼", "테이블스푼"
        );

        // 1. additional_ingredients 검증
        if (ingredients.getAdditionalIngredients() != null) {
            for (var ing : ingredients.getAdditionalIngredients()) {

                if (ing.getDescription() != null) {
                    log.error("❌ additional_ingredients에 description 존재: name={}, description={}",
                            ing.getName(), ing.getDescription());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }

                if (ing.getQuantity() == null) {
                    log.error("❌ additional_ingredients quantity null: name={}", ing.getName());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }

                if (ing.getQuantity() <= 0) {
                    log.error("❌ additional_ingredients quantity <= 0: name={}, quantity={}",
                            ing.getName(), ing.getQuantity());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }

                if (!allowedUnits.contains(ing.getUnit())) {
                    log.error("❌ additional_ingredients 허용되지 않은 unit: name={}, unit={}",
                            ing.getName(), ing.getUnit());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }
            }
        }

        // 2. optional_ingredients 검증
        if (ingredients.getOptionalIngredients() != null) {
            for (var ing : ingredients.getOptionalIngredients()) {

                String desc = ing.getDescription();

                if (desc == null) {
                    log.error("❌ optional_ingredients description null: name={}", ing.getName());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }

                if (desc.isBlank()) {
                    log.error("❌ optional_ingredients description blank: name={}", ing.getName());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }

                desc = desc.trim();

                boolean validFormat =
                        desc.startsWith("이 재료는 ") &&
                                (desc.endsWith("로 대체 가능합니다")
                                        || desc.equals("이 재료는 생략 가능합니다"));

                if (ing.getQuantity() != null && ing.getQuantity() <= 0) {
                    log.error("❌ optional_ingredients quantity <= 0: name={}, quantity={}",
                            ing.getName(), ing.getQuantity());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }

                if (ing.getUnit() != null && !allowedUnits.contains(ing.getUnit())) {
                    log.error("❌ optional_ingredients 허용되지 않은 unit: name={}, unit={}",
                            ing.getName(), ing.getUnit());
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID_FORMAT);
                }
            }
        }

        log.info("✅ AI 응답 검증 통과");
    }

    // youtube_references(실제 유튜브 링크 결과)를 포함한 JSON을 저장
    private void saveAiMessageWithYoutubeReferences(
            AiSession session,
            GeminiRecipeResponseDto response,
            List<YoutubeReferenceDto> youtubeReferences,
            MessageType type
    ) {
        try {
            validateAiResponse(response);

            // Gemini 응답을 JSON으로 변환
            ObjectNode root = objectMapper.valueToTree(response);

            // 레시피 생성 결과 저장
            JsonNode refsNode = objectMapper.valueToTree(youtubeReferences);
            root.set("youtube_references", refsNode);

            String json = objectMapper.writeValueAsString(root);

            AiMessage message = AiMessage.builder()
                    .session(session)
                    .role(Role.AI)
                    .messageType(type)
                    .content(json)
                    .build();

            aiMessageRepository.save(message);

        } catch (Exception e) {
            log.error("AI 메시지 저장 실패 (youtube 포함)", e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        }
    }


}
