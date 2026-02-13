package com.cookeep.cookeep.domain.verification.application;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.verification.application.sms.SmsSender;
import com.cookeep.cookeep.domain.verification.dao.SmsVerificationRepository;
import com.cookeep.cookeep.domain.verification.entity.SmsVerification;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmsVerificationService {

	private final SmsVerificationRepository smsVerificationRepository;
	private final SmsSender smsSender;
	private final SecureRandom random = new SecureRandom();

	@Value("${otp.ttl-seconds}")
	private long ttlSeconds;

	@Value("${otp.send-cooldown-seconds}")
	private long cooldownSeconds;

	@Value("${otp.max-verify-failures}")
	private int maxFailures;

	@Transactional
	public void sendCode(String phoneNumber, VerificationPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();
		// String phoneE164 = toE164KR(phoneNumber);

		// 재전송 쿨다운 체크
		smsVerificationRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phoneNumber, purpose)
			.ifPresent(latest -> {
				if (latest.getCreatedAt().isAfter(now.minusSeconds(cooldownSeconds))) {
					throw new AppException(ErrorCode.SMS_RESEND_TOO_FAST);
				}
			});

		String code = generate6();

		SmsVerification verification = SmsVerification.builder()
			// .phoneE164(phoneE164)
			.phone(phoneNumber)
			.purpose(purpose)
			.code(code)
			.failCount(0)
			.expiresAt(now.plusSeconds(ttlSeconds))
			.build();

		smsVerificationRepository.save(verification);

		smsSender.send(phoneNumber, "[COOKEEP] 인증번호는 " + code + " 입니다.");
	}

	@Transactional
	public void verifyCode(String phoneNumber, VerificationPurpose purpose, String inputCode) {
		LocalDateTime now = LocalDateTime.now();
		// String phoneE164 = toE164KR(rawPhoneNumber);

		SmsVerification verification = smsVerificationRepository
			.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phoneNumber, purpose)
			.orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_NOT_FOUND));

		// 이미 인증이 완료된 경우
		if (verification.isVerified()) {
			return;
		}

		// 인증번호가 만료된 경우
		if (verification.isExpired(now)) {
			throw new AppException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}

		// 인증 시도 횟수를 초과한 경우
		if (verification.getFailCount() >= maxFailures) {
			throw new AppException(ErrorCode.SMS_TOO_MANY_ATTEMPTS);
		}

		if (!verification.getCode().equals(inputCode)) {
			verification.increaseFailCount();

			if (verification.getFailCount() >= maxFailures) {
				throw new AppException(ErrorCode.SMS_TOO_MANY_ATTEMPTS);
			}
			throw new AppException(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		verification.markVerified(now);
	}

	// // 입력받은 전화번호를 E164 형식으로 변환
	// // 01012345678 / 010-1234-5678 -> +821012345678
	// private String toE164KR(String raw) {
	// 	String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
	// 	if (digits.length() != 11 || !digits.startsWith("010")) {
	// 		throw new AppException(ErrorCode.INVALID_PHONE_NUMBER);
	// 	}
	// 	return "+82" + digits.substring(1);
	// }

	// 인증번호를 랜덤으로 생성
	private String generate6() {
		return String.valueOf(random.nextInt(900_000) + 100_000);
	}

	@Transactional(readOnly = true)
	public void assertVerified(String phoneNumber, VerificationPurpose purpose) {

		// 인증 요청 내역이 없는 경우
		SmsVerification verification = smsVerificationRepository
			.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phoneNumber, purpose)
			.orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_NOT_FOUND));

		// 아직 인증이 완료되지 않은 경우
		if (!verification.isVerified()) {
			throw new AppException(ErrorCode.VERIFICATION_NOT_VERIFIED);
		}

		// 인증이 만료된 경우 (5분 초과 시 만료)
		if (verification.isExpired(LocalDateTime.now())) {
			throw new AppException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}
	}

}
