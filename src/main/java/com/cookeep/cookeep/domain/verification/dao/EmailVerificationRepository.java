package com.cookeep.cookeep.domain.verification.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.verification.entity.EmailVerification;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(
		String email,
		VerificationPurpose purpose
	);
}
