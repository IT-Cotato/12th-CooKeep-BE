package com.cookeep.cookeep.domain.verification.application;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.verification.application.sms.VerificationSender;
import com.cookeep.cookeep.domain.verification.dao.EmailVerificationRepository;
import com.cookeep.cookeep.domain.verification.entity.EmailVerification;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private final EmailVerificationRepository emailVerificationRepository;

	@Qualifier("emailVerificationSender")
	private final VerificationSender verificationSender;
	private final SecureRandom random = new SecureRandom();

	@Value("${otp.ttl-seconds}")
	private long ttlSeconds;

	@Value("${otp.send-cooldown-seconds}")
	private long cooldownSeconds;

	@Value("${otp.max-verify-failures}")
	private int maxFailures;

	// 인증번호를 랜덤으로 생성
	private String generate6() {
		return String.valueOf(random.nextInt(900_000) + 100_000);
	}

	@Transactional
	public void sendCode(String email, VerificationPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();

		// 재전송 쿨다운 체크
		emailVerificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
			.ifPresent(latest -> {
				if (latest.getCreatedAt().isAfter(now.minusSeconds(cooldownSeconds))) {
					throw new AppException(ErrorCode.EMAIL_RESEND_TOO_FAST);
				}
			});

		String code = generate6();

		// 실제 이메일은 대소문자 구분 X
		// 사용자가 대소문자를 잘못 입력하는 경우를 방지해 소문자로 처리
		String normalizedEmail = email.trim().toLowerCase();

		EmailVerification verification = EmailVerification.builder()
			.email(normalizedEmail)
			.purpose(purpose)
			.code(code)
			.failCount(0)
			.expiresAt(now.plusSeconds(ttlSeconds))
			.build();

		emailVerificationRepository.save(verification);

		EmailMessage message = buildVerificationMessage(purpose, code);

		verificationSender.send(normalizedEmail, message.title(), message.content());
	}


	// EmailMessage 객체
	private record EmailMessage(String title, String content) {}

	// 목적에 따라 이메일 내용 생성
	private EmailMessage buildVerificationMessage(VerificationPurpose purpose, String code) {
		String title = "[COOKEEP] 인증번호";
		String firstLine;

		switch (purpose) {
			case SIGNUP -> {
				firstLine = "[COOKEEP] 회원가입을 위한 인증 번호입니다.";
			}
			case RESET_PASSWORD -> {
				firstLine = "[COOKEEP] 비밀번호 재설정을 위한 인증 번호입니다.";
			}
			case CHANGE_EMAIL -> {
				firstLine = "[COOKEEP] 이메일 변경을 위한 인증입니다.";
			}
			case PASSWORD_VERIFICATION -> {
				firstLine = "[COOKEEP] 본인 확인을 위한 이메일 인증입니다.";
			}
			default -> throw new IllegalArgumentException("지원하지 않는 인증 목적입니다.");
		}

		String content = """
            %s<br><br>
            인증번호는 <b>%s</b> 입니다.<br>
            화면에 인증번호를 입력해주세요.
            """.formatted(firstLine, code);

		return new EmailMessage(title, content);
	}

	@Transactional
	public void verifyCode(String email, VerificationPurpose purpose, String inputCode) {
		LocalDateTime now = LocalDateTime.now();

		EmailVerification verification = emailVerificationRepository
			.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
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
			throw new AppException(ErrorCode.EMAIL_TOO_MANY_ATTEMPTS);
		}

		if (!verification.getCode().equals(inputCode)) {
			verification.increaseFailCount();

			if (verification.getFailCount() >= maxFailures) {
				throw new AppException(ErrorCode.EMAIL_TOO_MANY_ATTEMPTS);
			}
			throw new AppException(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		verification.markVerified(now);
	}

	@Transactional(readOnly = true)
	public void assertVerified(String email, VerificationPurpose purpose) {

		// 인증 요청 내역이 없는 경우
		EmailVerification verification = emailVerificationRepository
			.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
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
