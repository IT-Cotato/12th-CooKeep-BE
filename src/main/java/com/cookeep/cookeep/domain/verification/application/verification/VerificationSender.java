package com.cookeep.cookeep.domain.verification.application.verification;

public interface VerificationSender {
	void send(String to, String title, String content);
}
