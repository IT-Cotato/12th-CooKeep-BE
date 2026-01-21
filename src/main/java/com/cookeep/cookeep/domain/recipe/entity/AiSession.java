package com.cookeep.cookeep.domain.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
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

    /**
     * JSON 컬럼 (MySQL)
     * - 일단은 raw JSON 문자열로 저장 (예: [1,2,3])
     * - columnDefinition="json"로 MySQL JSON 타입 유지
     */
    @Column(name = "user_ingredient_ids", columnDefinition = "json")
    private String userIngredientIds;

    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // -------------------------
    // 도메인 로직 (필수 요구사항)
    // -------------------------

    public void pin() {
        this.isPinned = true;
    }

    public void unpin() {
        this.isPinned = false;
    }

    public void complete() {
        this.isCompleted = true;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void incrementAttemptNumber() {
        if (this.attemptNumber == null) this.attemptNumber = 0;
        this.attemptNumber++;
    }

    public boolean isChangeLimitExceeded(int max) {
        return this.attemptNumber != null && this.attemptNumber >= max;
    }

    public boolean isCompletedSession() {
        return Boolean.TRUE.equals(this.isCompleted);
    }
}
