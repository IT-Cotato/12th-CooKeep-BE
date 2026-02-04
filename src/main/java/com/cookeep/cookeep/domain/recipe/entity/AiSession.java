package com.cookeep.cookeep.domain.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

}
