package com.cookeep.cookeep.domain.recipe.entity;

import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_messages")
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_messages_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_sessions_id", nullable = false)
    private AiSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static AiMessage from(AiSession session, GeminiRecipeResponseDto response, MessageType type) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(response);

            return AiMessage.builder()
                    .session(session)
                    .role(Role.AI)
                    .messageType(type)
                    .content(json)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("AI 메시지 생성 실패", e);
        }
    }

    public static AiMessage userMessage(AiSession session, MessageType type, String content) {
        return AiMessage.builder()
                .session(session)
                .role(Role.USER)
                .messageType(type)
                .content(content)
                .build();
    }
}
