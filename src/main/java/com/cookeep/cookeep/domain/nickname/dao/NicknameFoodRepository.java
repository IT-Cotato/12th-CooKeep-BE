package com.cookeep.cookeep.domain.nickname.dao;

import java.util.List;

import com.cookeep.cookeep.domain.nickname.entity.NicknameFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NicknameFoodRepository extends JpaRepository<NicknameFood, Long> {
	@Query("select f.foodName from NicknameFood f")
	List<String> findAllFoodNames();
}
