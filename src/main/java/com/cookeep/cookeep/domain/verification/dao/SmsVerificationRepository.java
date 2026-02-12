package com.cookeep.cookeep.domain.verification.dao;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.verification.entity.SmsVerification;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;

public interface SmsVerificationRepository extends JpaRepository<SmsVerification, Long> {

	Optional<SmsVerification> findTopByPhoneAndPurposeOrderByCreatedAtDesc(
		String phone,
		VerificationPurpose purpose
	);
}
