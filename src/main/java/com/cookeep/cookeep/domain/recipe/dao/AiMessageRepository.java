package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiMessage;
import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import com.cookeep.cookeep.domain.recipe.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    Optional<AiMessage> findTopBySessionAndRoleOrderByCreatedAtDesc(AiSession session, Role role);

    // 기존 세션 레시피 조회
    @Query("SELECT m FROM AiMessage m " +
            "WHERE m.session.id = :sessionId AND m.role = 'AI' " +
            "ORDER BY m.createdAt ASC")
    List<AiMessage> findAllBySessionIdAndRoleAi(@Param("sessionId") Long sessionId);

    // 세션 내 모든 메시지 조회
    @Query("SELECT m FROM AiMessage m " +
            "WHERE m.session.id = :sessionId " +
            "ORDER BY m.createdAt ASC")
    List<AiMessage> findAllBySessionIdOrderByCreatedAt(@Param("sessionId") Long sessionId);

    // 세션 삭제 시 메시지 삭제
    void deleteBySessionId(Long sessionId);
}
