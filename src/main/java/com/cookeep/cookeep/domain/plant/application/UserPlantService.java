package com.cookeep.cookeep.domain.plant.application;

import com.cookeep.cookeep.api.dto.response.MyPlantResponse;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.plant.dao.PlantRepository;
import com.cookeep.cookeep.domain.plant.dao.UserPlantRepository;
import com.cookeep.cookeep.domain.plant.entity.Plant;
import com.cookeep.cookeep.domain.plant.entity.UserPlant;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
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
    private final PlantRepository plantRepository;

    // 유저 보유 식물 목록 조회
    @Transactional(readOnly = true) // 성능 최적화를 위해 읽기 전용 설정
    public List<MyPlantResponse> getMyPlants(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // @EntityGraph 덕분에 Plant 정보까지 한 번에 긁어옴
        List<UserPlant> userPlants = userPlantRepository.findAllByUser(user);

        return userPlants.stream()
                .map(plant -> MyPlantResponse.from(plant, user))
                .collect(Collectors.toList());
    }

    // 프로필 식물 지정
    @Transactional
    public void updateProfilePlant(Long userId, Long userPlantId) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 2. 변경할 식물 조회
        UserPlant userPlant = userPlantRepository.findById(userPlantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND));

        // 3. 보안 체크: 이 식물의 주인이 현재 로그인한 유저가 맞는지 검증
        if (!userPlant.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.NOT_MY_PLANT); // 본인 식물이 아닐 경우 예외 발생
        }

        // 4. 프로필 식물 업데이트 (더티 체킹에 의해 자동 반영)
        user.updateProfilePlant(userPlant);
    }

    // 현재 키우는 식물 등록
    @Transactional
    public void registerPlant(Long userId, long plantId) {
        // 1. 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 2. 기본으로 존재하는 식물인지 확인
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND));

        // 3. UserPlant 엔티티 생성 및 저장
        UserPlant newUserPlant = UserPlant.builder()
                .user(user)
                .plant(plant)
                .level(1)         // 초기 레벨 1
                .waterCount(0)    // 초기 물 주기 횟수 0
                .isHarvested(false)
                .isFrozen(false)
                .build();

        userPlantRepository.save(newUserPlant);
        userPlantRepository.flush(); // 이 줄을 추가하여 DB와 동기화 (createdAt 채워짐)

        // 4. 자동 모드(isProfileAutoUpdate=true)일 때만 프로필 갱신
        user.setProfilePlantAuto(newUserPlant);
    }

    // 식물 포기하기
    @Transactional
    public void abandonPlant(Long userId, Long userPlantId) {
        // 1. 식물 조회
        UserPlant userPlant = userPlantRepository.findById(userPlantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND));

        // 2. 권한 체크 (내 식물인지)
        if (!userPlant.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.NOT_MY_PLANT);
        }

        // 3. 상태 체크: 반드시 얼어있는(isFrozen) 상태여야만 포기가 가능하도록 제한
        if (!userPlant.getIsFrozen()) {
            throw new AppException(ErrorCode.PLANT_NOT_FROZEN);
        }

        // 4. 식물 삭제
        userPlantRepository.delete(userPlant);

        // 5. 삭제한 식물이 현재 프로필 식물이라면 연관 관계 해제
        User user = userPlant.getUser();
        if (user.getProfilePlant() != null && user.getProfilePlant().getUserPlantId().equals(userPlantId)) {
            user.updateProfilePlant(null);
        }
    }

    // 식물에게 물 주기
    @Transactional
    public void giveWater(Long userId, Long userPlantId) {
        // 1. 식물 조회
        UserPlant userPlant = userPlantRepository.findById(userPlantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND)); // 404

        // 2. 권한 체크
        if (!userPlant.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.NOT_MY_PLANT); // 403
        }

        // 3. 상태 체크 (이미 수확했거나 얼어있는 경우)
        if (userPlant.getIsHarvested()) {
            throw new AppException(ErrorCode.ALREADY_HARVESTED); // 400
        }
        if (userPlant.getIsFrozen()) {
            throw new AppException(ErrorCode.PLANT_IS_FROZEN); // 400
        }

        // 4. 쿠키 차감 로직 (Cookie 브랜치에서 구현 예정)
        // TODO: userService.useCookie(userId, 1);

        // 5. 물 주기 수행
        userPlant.giveWater();
    }
}
