package com.cookeep.cookeep.domain.user.application;

import com.cookeep.cookeep.api.dto.request.NicknameUpdateRequestDto;
import com.cookeep.cookeep.api.dto.response.MyProfileResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.onboarding.dao.WeeklyGoalRepository;
import com.cookeep.cookeep.domain.onboarding.entity.WeeklyGoal;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInfoService {

    private final UserRepository userRepository;
    private final WeeklyGoalRepository weeklyGoalRepository;

    public void updateNickname(Long userId, NicknameUpdateRequestDto request) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String newNickname = request.getNickname();

        // 현재 닉네임과 동일하면 변경할 필요 없음
        if (newNickname.equals(user.getNickname())) {
            return;
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(newNickname)) {
            throw new AppException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(newNickname);
    }

    @Transactional(readOnly = true)
    public MyProfileResponseDto getProfile(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 이번 주 월요일 계산
        LocalDate currentWeekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 이번 주 목표 조회 (없으면 null)
        WeeklyGoal weeklyGoal = weeklyGoalRepository
                .findByUserAndWeekStartDate(user, currentWeekStart)
                .orElse(null);

        return MyProfileResponseDto.of(user, weeklyGoal);
    }
}
