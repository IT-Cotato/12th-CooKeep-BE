package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
}
