package com.cookeep.cookeep.domain.plant.dao;

import com.cookeep.cookeep.domain.plant.entity.UserPlant;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPlantRepository extends JpaRepository<UserPlant, Long>{

    // N+1 문제를 방지하기 위해 plant 엔티티를 fetch join으로 가져옵니다.
    @EntityGraph(attributePaths = {"plant"})
    List<UserPlant> findAllByUser(User user);

    // 현재 키우는 식물 조회 (성장 정지 여부 무관하게, 아직 수확 완료되지 않은 식물)
    @EntityGraph(attributePaths = {"plant"})
    Optional<UserPlant> findByUserAndIsHarvestedFalse(User user);

    // 성장 정지된 식물이 존재하는지 확인
    boolean existsByUserAndIsFrozenTrue(User user);

    // 유저가 보유한 식물이 존재하는지 확인
    boolean existsByUser(User user);
}
