package com.cookeep.cookeep.domain.nickname.dao;

import java.util.List;

import com.cookeep.cookeep.domain.nickname.entity.NicknameAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NicknameActionRepository extends JpaRepository<NicknameAction, Long> {
	@Query("select a.actionName from NicknameAction a")
	List<String> findAllActionNames();
}