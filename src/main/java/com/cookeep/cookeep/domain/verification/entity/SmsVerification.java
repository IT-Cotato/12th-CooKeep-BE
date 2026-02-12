package com.cookeep.cookeep.domain.verification.entity;

import java.time.LocalDateTime;

import com.cookeep.cookeep.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "sms_verifications")
public class SmsVerification extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long smsVerificationId;

	@Column(nullable = false, length = 20)
	// private String phoneE164;
	private String phone;

	// SIGNUP / RESET_PASSWORD / CHANGE_PHONE
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private VerificationPurpose purpose;

	@Column(nullable = false, length = 6)
	private String code;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private LocalDateTime verifiedAt;

	// 인증 실패 횟수, 5회 초과 시 잠금
	@Column(nullable = false)
	private int failCount;

	public boolean isExpired(LocalDateTime now) {
		return now.isAfter(expiresAt);
	}

	public boolean isVerified() {
		return verifiedAt != null;
	}

	public void markVerified(LocalDateTime now) {
		this.verifiedAt = now;
	}

	public void increaseFailCount() {
		this.failCount++;
	}
}
