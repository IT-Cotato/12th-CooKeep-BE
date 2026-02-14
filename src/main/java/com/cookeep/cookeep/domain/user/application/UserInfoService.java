package com.cookeep.cookeep.domain.user.application;

import java.util.Map;
import java.util.Optional;

import com.cookeep.cookeep.api.dto.request.NicknameUpdateRequestDto;
import com.cookeep.cookeep.api.dto.request.SendCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.UpdateEmailRequestDTO;
import com.cookeep.cookeep.api.dto.request.UpdateMarketingPushDTO;
import com.cookeep.cookeep.api.dto.request.UpdatePasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyPasswordRequestDTO;
import com.cookeep.cookeep.api.dto.response.UserProfileResponseDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.cookeep.cookeep.domain.verification.application.SmsVerificationService;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.web.error.Error;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserInfoService {

    private final UserRepository userRepository;
    private final UserReader userReader;
    private final SmsVerificationService smsVerificationService;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    // 비밀번호 입력 최대 시도 횟수
    private static final int MAX_ATTEMPTS = 5;

    // 회원 정보 조회
    public UserProfileResponseDTO getMyProfile(Long userId) {
        User user = userReader.readById(userId);

        String nickname = user.getNickname();
        String email = user.getEmail();
        Provider provider = userAuthRepository.findProviderByUserId(userId);
        Boolean marketingPush = user.getMarketingPush();

        // 로컬 유저일 경우에만 존재, 소셜은 전화번호 없음
        String phoneNumber = (provider == Provider.LOCAL)
            ? user.getPhoneNumber()
            : null;


        return new UserProfileResponseDTO(
            nickname, phoneNumber, email,
            provider, marketingPush
        );
    }

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

    // 전화번호 변경 시 전화번호 인증 요청
    public void sendChangePhoneCode(Long userId, SendCodeRequestDTO sendCodeRequestDTO) {
        User user = userReader.readById(userId);
        String newPhoneNumber = sendCodeRequestDTO.phoneNumber();

        String currentPhoneNumber = user.getPhoneNumber();

        // 현재 등록된 전화번호와 동일한 경우
        if (newPhoneNumber.equals(currentPhoneNumber)) {
            throw new AppException(ErrorCode.SAME_AS_CURRENT_PHONE_NUMBER);
        }

        // 이미 등록된 번호인지 확인
        if (userRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
        }

        smsVerificationService.sendCode(newPhoneNumber, VerificationPurpose.CHANGE_PHONE);
    }

    // 전화번호 변경 시 전화번호 인증 확인
    @Transactional
    public void verifyChangePhoneCode(Long userId, VerifyCodeRequestDTO verifyCodeRequestDTO) {
        User user = userReader.readById(userId);
        String newPhoneNumber = verifyCodeRequestDTO.phoneNumber();
        String code = verifyCodeRequestDTO.code();

        // 이미 등록된 번호인지 재확인
        if (userRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
        }

        smsVerificationService.verifyCode(newPhoneNumber, VerificationPurpose.CHANGE_PHONE, code);

        user.updatePhoneNumber(newPhoneNumber);
    }

    // 이메일 변경
    @Transactional
    public void updateMyEmail(Long userId, UpdateEmailRequestDTO updateEmailRequestDTO) {

        Provider provider = userAuthRepository.findProviderByUserId(userId);

        // 소셜 로그인은 이메일을 변경할 수 없음
        if (provider != Provider.LOCAL) {
            throw new AppException(ErrorCode.SOCIAL_USER_EMAIL_CHANGE_NOT_ALLOWED);
        }

        User user = userReader.readById(userId);
        String newEmail = updateEmailRequestDTO.email();

        String currentEmail = user.getEmail();

        // 현재 등록된 이메일과 동일한 경우
        if (newEmail.equals(currentEmail)) {
            throw new AppException(ErrorCode.SAME_AS_CURRENT_EMAIL);
        }

        // 이미 등록된 이메일인지 확인
        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        user.updateEmail(newEmail);
    }

    // 비밀번호 확인
    // 비밀번호 검증 실패 횟수를 예외 발생 시에도 누적시키기 위해 rollback 제외하였음
    @Transactional(noRollbackFor = AppException.class)
    public void verifyMyPassword(Long userId, VerifyPasswordRequestDTO verifyPasswordRequestDTO) {
        User user = userReader.readById(userId);

        if (passwordEncoder.matches(verifyPasswordRequestDTO.password(), user.getPassword())) {
            // 기존 비밀번호와 일치할 경우 passwordCnt 초기화
            user.updatePasswordCnt(0);
            return;
        }

        // null일 경우 0으로 세팅 후 +1, 값이 있을 경우 해당 값을 가져오고 + 1
        int passwordCnt = Optional.ofNullable(user.getPasswordCnt()).orElse(0) + 1;

        // 5회 틀릴 경우 LOCK
        // 동시/중복 요청 고려하여 >=로 판별
        if (passwordCnt >= MAX_ATTEMPTS) {
            user.updatePasswordCnt(MAX_ATTEMPTS);
            user.updateUserStatus(UserStatus.LOCK);
            log.warn("Password verification locked. userId={}", userId);
            throw new AppException(
                ErrorCode.PASSWORD_VERIFICATION_LOCKED,
                Map.of(
                    "failedCount", String.valueOf(MAX_ATTEMPTS),
                    "maxCount", String.valueOf(MAX_ATTEMPTS)
                )
            );
        } else {
            user.updatePasswordCnt(passwordCnt);
            throw new AppException(
                ErrorCode.PASSWORD_MISMATCH,
                Map.of(
                    "failedCount", String.valueOf(passwordCnt),
                    "maxCount", String.valueOf(MAX_ATTEMPTS)
                )
            );
        }
    }

    // 비밀번호 변경
    @Transactional
    public void updateMyPassword(Long userId, UpdatePasswordRequestDTO updatePasswordRequestDTO) {
        User user = userReader.readById(userId);

        String encodedPassword = passwordEncoder.encode(updatePasswordRequestDTO.password());

        // 기존에 등록되어 있던 비밀번호와 새로 들어온 비밀번호가 동일할 경우
        if (passwordEncoder.matches(updatePasswordRequestDTO.password(), user.getPassword())) {
            throw new AppException(ErrorCode.SAME_AS_PREVIOUS_PASSWORD);
        }

        user.updatePassword(encodedPassword);
    }

    // 알림설정 변경
    // 멱등성을 보장하기 위해 최종 상태를 명시적으로 가져옴
    // 기존 DB에 저장된 상태와 동일할 경우 에러 발생 없이 그대로 업데이트되도록 처리하였음
    @Transactional
    public void updateMarketingPush(Long userId, UpdateMarketingPushDTO updateMarketingPushDTO) {
        User user = userReader.readById(userId);

        Boolean newMarketingPush = updateMarketingPushDTO.marketingPush();

        user.updateMarketingPush(newMarketingPush);
    }
}
