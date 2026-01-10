package com.cookeep.cookeep.domain.Plants.application;

import com.cookeep.cookeep.api.dto.response.MyPlantResponse;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.Plants.dao.UserPlantRepository;
import com.cookeep.cookeep.domain.Plants.entity.UserPlants;
import com.cookeep.cookeep.domain.Users.entity.Users;
import com.cookeep.cookeep.domain.Users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPlantService {
    private final UserPlantRepository userPlantRepository;
    private final UserRepository userRepository;

    // 유저 보유 식물 목록 조회
    @Transactional(readOnly = true) // 성능 최적화를 위해 읽기 전용 설정
    public List<MyPlantResponse> getMyPlants(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // @EntityGraph 덕분에 Plant 정보까지 한 번에 긁어옴
        List<UserPlants> userPlants = userPlantRepository.findAllByUser(user);

        return userPlants.stream()
                .map(plant -> MyPlantResponse.from(plant, user))
                .collect(Collectors.toList());
    }

    // 프로필 식물 지정
    @Transactional
    public void updateProfilePlant(Long userId, Long userPlantId) {
        // 1. 유저 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 2. 변경할 식물 조회
        UserPlants userPlant = userPlantRepository.findById(userPlantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND));

        // 3. 보안 체크: 이 식물의 주인이 현재 로그인한 유저가 맞는지 검증
        if (!userPlant.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.NOT_MY_PLANT); // 본인 식물이 아닐 경우 예외 발생
        }

        // 4. 프로필 식물 업데이트 (더티 체킹에 의해 자동 반영)
        user.updateProfilePlant(userPlant);
    }
}
