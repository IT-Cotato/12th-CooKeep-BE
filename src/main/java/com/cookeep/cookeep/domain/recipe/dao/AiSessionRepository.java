package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiSessionRepository extends JpaRepository<AiSession, Long> {
    Optional<AiSession> findByIdAndUserId(Long id, Long userId);
    Optional<AiSession> findById(Long sessionId);
}
