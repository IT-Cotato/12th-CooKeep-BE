package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.recipe.dao.AiMessageRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiSessionRepository;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeRequestDto;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.IngredientDetailDto;
import com.cookeep.cookeep.domain.recipe.entity.AiMessage;
import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import com.cookeep.cookeep.domain.recipe.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AiRecipeService {

    private static final int MAX_CHANGE_COUNT = 5;

    private final GeminiService geminiService;
    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final ObjectMapper objectMapper;

    public AiRecipeResponseDto generateRecipe(
            Long userId,
            AiRecipeRequestDto request
    ) {

        // 1. 세션 조회 or 생성
        AiSession session = getOrCreateSession(userId, request);

        // 2. 변경 횟수 제한 체크
        if (session.isChangeLimitExceeded(MAX_CHANGE_COUNT)) {
            throw new AppException(ErrorCode.AI_RECIPE_CHANGE_LIMIT_EXCEEDED);
        }

        // 3. 시도 횟수 증가
        session.incrementAttemptNumber();

        // 4. USER 프리셋 메시지 저장
        saveUserMessage(session);

        // 5. Gemini 호출
        GeminiRecipeResponseDto geminiResponse =
                geminiService.generateRecipe(
                        toIngredientDetailDtos(request.getIngredients()),
                        request.getDifficulty()
                );

        // 6. AI 메시지 저장
        saveAiMessage(session, geminiResponse);

        // 7. 세션 제목 갱신 (마지막 AI 레시피 제목)
        session.updateTitle(geminiResponse.getTitle());

        return AiRecipeResponseDto.builder()
                .sessionId(session.getId())
                .changeCount(session.getAttemptNumber())
                .recipe(geminiResponse)
                .build();
    }

    // 내부 메서드
    private AiSession getOrCreateSession(Long userId, AiRecipeRequestDto request) {

        if (request.getSessionId() == null) {
            return aiSessionRepository.save(
                    AiSession.builder()
                            .userId(userId)
                            .difficulty(request.getDifficulty())
                            .attemptNumber(0)
                            .isPinned(false)
                            .isCompleted(false)
                            .build()
            );
        }

        return aiSessionRepository.findByIdAndUserId(
                        request.getSessionId(), userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.AI_SESSION_NOT_FOUND));
    }

    private void saveUserMessage(AiSession session) {
        aiMessageRepository.save(
                AiMessage.builder()
                        .session(session)
                        .role(Role.USER)
                        .content("다른 레시피를 받을래요")
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
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        }
    }

    private List<IngredientDetailDto> toIngredientDetailDtos(
            List<IngredientDetailDto> ingredients
    ) {
        return ingredients;
    }
}
