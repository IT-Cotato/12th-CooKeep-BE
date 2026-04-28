// package com.cookeep.cookeep.domain.verification.application.sms;
//
// import com.cookeep.cookeep.common.exception.AppException;
// import com.cookeep.cookeep.common.exception.ErrorCode;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import net.nurigo.sdk.message.model.Message;
// import net.nurigo.sdk.message.service.DefaultMessageService;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;
//
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class CoolVerificationSender implements VerificationSender {
//
// 	private final DefaultMessageService messageService;
//
// 	@Value("${sms.sender}")
// 	private String from010; // 발신번호
//
// 	@Override
// 	public void send(String to010, String title, String text) {
// 		try {
// 			log.info("[SMS] send attempt. to={}", mask(to010));
//
// 			Message message = new Message();
// 			message.setFrom(from010);
// 			message.setTo(to010);
// 			message.setText(text);
//
// 			messageService.send(message);
//
// 			log.info("[SMS] send success. to={}", mask(to010));
// 		} catch (Exception e) {
// 			log.error("[SMS] CoolSMS send failed. to={}", mask(to010), e);
// 			throw new AppException(ErrorCode.EMAIL_PROVIDER_ERROR);
// 		}
// 	}
//
// 	private String mask(String phone010) { // 마스킹 후 로그 기록
// 		if (phone010 == null || phone010.length() < 4) return "****";
// 		return phone010.substring(0, phone010.length() - 4) + "****";
// 	}
// }
