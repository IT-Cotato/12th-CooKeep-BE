package com.cookeep.cookeep.domain.Plant.dao;

import com.cookeep.cookeep.domain.Plant.entity.UserPlant;
import com.cookeep.cookeep.domain.Users.entity.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlantRepository extends JpaRepository<UserPlant, Long>{

    // N+1 문제를 방지하기 위해 plant 엔티티를 fetch join으로 가져옵니다.
    @EntityGraph(attributePaths = {"plant"})
    List<UserPlant> findAllByUser(Users user);
}
