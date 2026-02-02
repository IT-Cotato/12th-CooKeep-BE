package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "AiSessionListResponseDto",
        description = "AI 레시피 대화 세션 목록 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class AiSessionListResponseDto {

    @Schema(
            description = "즐겨찾기된 세션 목록",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<SessionSummary> pinned;

    @Schema(
            description = "일반 세션 목록 (최신순)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<SessionSummary> sessions;

    @Schema(
            name = "SessionSummary",
            description = "세션 요약 정보"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SessionSummary {

        @Schema(
                description = "세션 ID",
                example = "21",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long sessionId;

        @Schema(
                description = "세션 제목 (첫 번째 레시피 제목)",
                example = "양파 고구마순 볶음",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        private String title;

        @Schema(
                description = "세션 생성 시간",
                example = "2025-02-01T14:32:10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private LocalDateTime createdAt;

        @Schema(
                description = "즐겨찾기 여부",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Boolean isPinned;

        public static SessionSummary from(AiSession session) {
            return SessionSummary.builder()
                    .sessionId(session.getId())
                    .title(session.getTitle())
                    .createdAt(session.getCreatedAt())
                    .isPinned(session.getIsPinned() != null ? session.getIsPinned() : false)
                    .build();
        }
    }
}
