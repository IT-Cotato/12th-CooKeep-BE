package com.cookeep.cookeep.domain.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_recipes")
public class AiRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_recipes_id", nullable = false)
    private Long id;

    @Column(name = "ai_recipes_title", length = 255)
    private String title;

    // JSON 컬럼들은 raw JSON 문자열로 저장 (추후 DTO/서비스에서 파싱)
    @Column(name = "ai_recipes_ingredients", columnDefinition = "json")
    private String ingredientsJson;

    @Column(name = "ai_recipes_steps", columnDefinition = "json")
    private String stepsJson;

    @Column(name = "youtube_url", columnDefinition = "json")
    private String youtubeUrlJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_sessions_id", nullable = false)
    private AiSession session;
}
