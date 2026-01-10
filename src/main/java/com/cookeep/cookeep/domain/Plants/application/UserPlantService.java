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
}
