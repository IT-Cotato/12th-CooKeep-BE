package com.cookeep.cookeep.domain.verification.application.sms;

public interface VerificationSender {
	void send(String to, String title, String content);
}
