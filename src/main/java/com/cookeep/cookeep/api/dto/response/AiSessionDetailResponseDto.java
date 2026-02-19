package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.recipe.entity.AiMessage;
import com.cookeep.cookeep.domain.recipe.entity.MessageType;
import com.cookeep.cookeep.domain.recipe.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "AiSessionDetailResponse",
        description = "AI 레시피 대화 상세 리스트 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class AiSessionDetailResponseDto {

    @Schema(
            description = "세션 ID",
            example = "21",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long sessionId;

    @Schema(description = "세션 완료 여부", example = "true")
    private boolean isCompleted;

    @Schema(
            description = "개별 메시지 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<MessageItem> messages;

    @Getter
    @Builder
    public static class MessageItem {
        private Role role;
        private MessageType messageType;
        private String content;
        private LocalDateTime createdAt;

        public static MessageItem from(AiMessage m) {
            return MessageItem.builder()
                    .role(m.getRole())
                    .messageType(m.getMessageType())
                    .content(m.getContent())
                    .createdAt(m.getCreatedAt())
                    .build();
        }
    }
}
