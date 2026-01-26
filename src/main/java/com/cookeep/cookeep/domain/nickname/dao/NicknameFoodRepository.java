package com.cookeep.cookeep.domain.nickname.dao;

import com.cookeep.cookeep.domain.nickname.entity.NicknameFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NicknameFoodRepository extends JpaRepository<NicknameFood, Long> {
}
