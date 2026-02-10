package com.cookeep.cookeep.domain.recipe.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_sessions")
public class AiSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_sessions_id", nullable = false)
    private Long id;

    @Column(name = "title", length = 255)
    private String title;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private Difficulty difficulty;

    @Column(name = "is_pinned")
    private Boolean isPinned;

    @Column(name = "user_ingredient_ids", columnDefinition = "json")
    private String userIngredientIds;

    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ingredient_ids", columnDefinition = "JSON")
    private String ingredientIdsJson;

    // 유저 요청 식재료에 D-0 식재료 포함 여부 (true -> 쿠키 3개 지급/1일1회)
    @Column(name = "has_urgent_ingredient", nullable = false)
    @Builder.Default
    private Boolean hasUrgentIngredient = false;

    // Domain Logic
    public void pin() {
        this.isPinned = true;
    }

    public void unpin() {
        this.isPinned = false;
    }

    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }

    public void increaseAttempt() {
        this.attemptNumber++;
    }

    // JSON 직렬화/역직렬화 메서드
    @Transient
    private ObjectMapper objectMapper = new ObjectMapper();

    public List<Long> getIngredientIds() {
        if (ingredientIdsJson == null || ingredientIdsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(ingredientIdsJson,
                    new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize ingredient IDs", e);
            return Collections.emptyList();
        }
    }

    public void setIngredientIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            this.ingredientIdsJson = null;
            return;
        }
        try {
            this.ingredientIdsJson = objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            log.error("Failed to serialize ingredient IDs", e);
        }
    }

}
