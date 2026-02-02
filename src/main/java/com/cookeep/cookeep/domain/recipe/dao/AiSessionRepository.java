package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiSessionRepository extends JpaRepository<AiSession, Long> {
    Optional<AiSession> findByIdAndUserId(Long id, Long userId);
    Optional<AiSession> findById(Long sessionId);

    // 사용자의 모든 세션 조회
    List<AiSession> findAllByUserIdOrderByIsPinnedDescUpdatedAtDesc(Long userId);
}
