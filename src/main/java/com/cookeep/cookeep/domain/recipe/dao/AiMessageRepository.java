package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiMessage;
import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import com.cookeep.cookeep.domain.recipe.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    Optional<AiMessage> findTopBySessionAndRoleOrderByCreatedAtDesc(AiSession session, Role role);
}
