package com.cookeep.cookeep.domain.nickname.dao;

import com.cookeep.cookeep.domain.nickname.entity.NicknameAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NicknameActionRepository extends JpaRepository<NicknameAction, Long> {
}