package com.cookeep.cookeep.domain.user.application;

import com.cookeep.cookeep.api.dto.request.NicknameUpdateRequestDto;
import com.cookeep.cookeep.api.dto.request.SendCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.UpdateEmailRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyCodeRequestDTO;
import com.cookeep.cookeep.api.dto.response.UserProfileResponseDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.verification.application.SmsVerificationService;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.error.Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInfoService {

    private final UserRepository userRepository;
    private final UserReader userReader;
    private final SmsVerificationService smsVerificationService;
    private final UserAuthRepository userAuthRepository;

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
}
