package com.cookeep.cookeep.domain.plant.application;

import com.cookeep.cookeep.api.dto.response.GrowingPlantResponseDto;
import com.cookeep.cookeep.api.dto.response.MyPlantResponseDto;
import com.cookeep.cookeep.api.dto.response.RegisterPlantResponseDto;
import com.cookeep.cookeep.api.dto.response.WaterResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.dao.PendingCookieRewardRepository;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.cookie.entity.PendingCookieReward;
import com.cookeep.cookeep.domain.plant.dao.PlantRepository;
import com.cookeep.cookeep.domain.plant.dao.UserPlantRepository;
import com.cookeep.cookeep.domain.plant.dao.WateringLogRepository;
import com.cookeep.cookeep.domain.plant.entity.Plant;
import com.cookeep.cookeep.domain.plant.entity.PlantStatus;
import com.cookeep.cookeep.domain.plant.entity.UserPlant;
import com.cookeep.cookeep.domain.plant.entity.WateringLog;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPlantService {
    private final UserPlantRepository userPlantRepository;
    private final WateringLogRepository wateringLogRepository;
    private final UserRepository userRepository;
    private final PlantRepository plantRepository;
    private final CookieService cookieService;
    private final UserReader userReader;
    private final PendingCookieRewardRepository pendingCookieRewardRepository;

    // 미접속 일수 기반 식물 상태 계산 및 성장 정지 처리 (스케줄러에서 호출)
    @Transactional
    public void checkAndUpdatePlantStatusById(Long userId) {
        User user = userReader.readById(userId);
        LocalDateTime lastAccess = user.getLastAccessAt();

        // lastAccessAt이 null이면 (기존 유저 or 최초 적용) NORMAL 처리
        if (lastAccess == null) {
            user.updatePlantStatus(PlantStatus.NORMAL);
            return;
        }

        long inactiveDays = ChronoUnit.DAYS.between(lastAccess, LocalDateTime.now());

        if (inactiveDays >= 14) {
            // 14일 이상 미접속: 키우는 식물 성장 정지 + FROZEN 상태
            userPlantRepository.findByUserAndIsHarvestedFalse(user)
                .filter(plant -> !plant.getIsFrozen())
                .ifPresent(UserPlant::freeze);
            user.updatePlantStatus(PlantStatus.FROZEN);
        } else if (inactiveDays >= 7) {
            // 7~13일 미접속: WILTING 상태 (성장 정지는 하지 않음)
            user.updatePlantStatus(PlantStatus.WILTING);
        } else {
            // 아직 성장 정지된 식물이 남아 있으면 FROZEN 유지 (유저가 살리기 전까지)
            boolean hasFrozenPlant = userPlantRepository.existsByUserAndIsFrozenTrue(user);
            user.updatePlantStatus(hasFrozenPlant ? PlantStatus.FROZEN : PlantStatus.NORMAL);
        }
    }

    // 쿠킵스 화면: 현재 키우는 식물 정보 조회
    @Transactional(readOnly = true)
    public GrowingPlantResponseDto getGrowingPlant(Long userId) {
        User user = userReader.readById(userId);

        return userPlantRepository.findByUserAndIsHarvestedFalse(user)
                .map(plant -> GrowingPlantResponseDto.from(plant, user))
                .orElse(null);
    }

    // 유저 보유 식물 목록 조회
    @Transactional(readOnly = true) // 성능 최적화를 위해 읽기 전용 설정
    public List<MyPlantResponseDto> getMyPlants(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // @EntityGraph 덕분에 Plant 정보까지 한 번에 긁어옴
        List<UserPlant> userPlants = userPlantRepository.findAllByUser(user);

        return userPlants.stream()
                .map(plant -> MyPlantResponseDto.from(plant, user))
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

    // 현재 키우는 식물 등록 (첫 식물 등록 여부 + userPlantId 반환)
    @Transactional
    public RegisterPlantResponseDto registerPlant(Long userId, long plantId) {
        // 1. 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 2. 첫 식물 등록 여부 확인
        boolean isFirstPlant = !userPlantRepository.existsByUser(user);

        // 3. 기본으로 존재하는 식물인지 확인
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND));

        // 4. UserPlant 엔티티 생성 및 저장
        UserPlant newUserPlant = UserPlant.builder()
                .user(user)
                .plant(plant)
                .level(1)         // 초기 레벨 1
                .waterCount(0)    // 초기 물 주기 횟수 0
                .isHarvested(false)
                .isFrozen(false)
                .build();

        userPlantRepository.save(newUserPlant);

        // 5. 자동 모드(isProfileAutoUpdate=true)일 때만 프로필 갱신
        user.setProfilePlantAuto(newUserPlant);

        // 6. 새 식물 등록 시 plantStatus를 NORMAL로 초기화
        // (키우는 식물 없이 14일+ 미접속 후 로그인하면 FROZEN이 설정될 수 있으므로)
        user.updatePlantStatus(PlantStatus.NORMAL);

        String message = isFirstPlant ? "첫 식물 등록이 완료되었습니다." : "식물 등록이 완료되었습니다.";
        return new RegisterPlantResponseDto(newUserPlant.getUserPlantId(), message);
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

        // 3. 상태 체크: 반드시 성장 정지(isFrozen) 상태여야만 포기가 가능하도록 제한
        if (!userPlant.getIsFrozen()) {
            throw new AppException(ErrorCode.PLANT_NOT_FROZEN);
        }

        // 4. 삭제한 식물이 현재 프로필 식물이라면 연관 관계 해제
        User user = userPlant.getUser();
        if (user.getProfilePlant() != null && user.getProfilePlant().getUserPlantId().equals(userPlantId)) {
            user.updateProfilePlant(null);
        }

        // 5. 식물 삭제
        userPlantRepository.delete(userPlant);
    }

    // 식물에게 물 주기
    @Transactional
    public WaterResponseDto giveWater(Long userId, Long userPlantId) {
        // 1. 식물 조회
        UserPlant userPlant = userPlantRepository.findById(userPlantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND)); // 404

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)); // 404

        // 2. 권한 체크
        if (!userPlant.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.NOT_MY_PLANT); // 403
        }

        // 3. 무료 물주기 여부 확인 (유저의 첫 물주기인 경우 쿠키 차감 없음)
        boolean isFirstWatering = !wateringLogRepository.existsByUserUserId(userId);
        if (!isFirstWatering) {
            // 첫 물주기가 아닌 경우에만 쿠키 차감
            cookieService.updateCookie(userId, CookieLog.CookieLogType.WATERING);
        }

        // 4. 물 주기 수행 및 로그 저장
        userPlant.giveWater();

        WateringLog log = WateringLog.builder()
                .userPlantId(userPlant.getUserPlantId())
                .user(user)
                .build();
        wateringLogRepository.save(log);

        // 5. 수확 완료 시 즉시 지급 대신 Pending Reward 생성
        Long pendingRewardId = null;
        if (userPlant.getIsHarvested()) {
            PendingCookieReward pending = PendingCookieReward.builder()
                    .user(user)
                    .rewardType(CookieLog.CookieLogType.BONUS_PLANT_HARVEST_REWARD)
                    .status(PendingCookieReward.PendingRewardStatus.PENDING)
                    .build();
            pendingRewardId = pendingCookieRewardRepository.save(pending).getPendingRewardId();
        }

        return WaterResponseDto.builder()
                .isFreeWatering(isFirstWatering)
                .isJustHarvested(userPlant.getIsHarvested())
                .pendingRewardId(pendingRewardId)
                .build();
    }

    // 식물 살리기
    @Transactional
    public void revivePlant(Long userId, Long userPlantId) {
        // 1. 식물 조회
        UserPlant userPlant = userPlantRepository.findById(userPlantId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_NOT_FOUND)); // 404

        // 2. 권한 체크
        if (!userPlant.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.NOT_MY_PLANT); // 403
        }

        // 3. 상태 체크: 성장 정지 상태가 아니면 살릴 수 없음
        if (!userPlant.getIsFrozen()) {
            throw new AppException(ErrorCode.PLANT_NOT_FROZEN); // 400
        }

        // 4. 쿠키 차감 로직
        cookieService.updateCookie(userId, CookieLog.CookieLogType.REVIVE_PLANT);

        // 5. 식물 살리기 수행 (isFrozen = false)
        userPlant.revive();

        // 6. 유저의 plantStatus를 NORMAL로 변경
        userPlant.getUser().updatePlantStatus(PlantStatus.NORMAL);
    }
}
