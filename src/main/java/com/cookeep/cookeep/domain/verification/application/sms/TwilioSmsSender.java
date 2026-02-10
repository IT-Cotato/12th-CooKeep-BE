package com.cookeep.cookeep.domain.verification.application.sms;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TwilioSmsSender implements SmsSender {

	@Value("${sms.sender}")
	private String from; // 발신 번호

	@Override
	public void send(String toE164, String text) { // 전화번호, 메세지
		try {
			log.info("[SMS] send attempt. toTail={}, fromTail={}",
				maskTail(toE164), maskTail(from));

			Message message = Message.creator(
				new PhoneNumber(toE164),
				new PhoneNumber(from),
				text
			).create(); // 문자 발송

			log.info("[SMS] send success. sid={}", message.getSid());

		} catch (ApiException e) {
			Integer code = e.getCode(); // Twilio 에러 코드를 가져옴
			log.warn("[SMS] Twilio error. code={}, moreInfo={}, toTail={}, fromTail={}, msg={}",
				e.getCode(),
				e.getMoreInfo(),
				maskTail(toE164),
				maskTail(from),
				e.getMessage());


			// Twilio 에러 코드가 21211인 경우, 번호 형식 오류
			if (code != null && code == 21211) {
				throw new AppException(ErrorCode.INVALID_PHONE_NUMBER);
			}

			// Twilio 계정/Trial/발신번호 문제 등 외부 시스템 오류
			throw new AppException(ErrorCode.SMS_PROVIDER_ERROR);

		} catch (Exception e) { // 이외 일반적인 예외 처리
			log.error("[SMS] Unexpected error to={}", maskTail(toE164), e);
			throw new AppException(ErrorCode.SMS_SEND_FAILED);
		}
	}

	// 로그 기록용, 전화번호 마스킹
	private String maskTail(String phone) {
		if (phone == null || phone.length() < 4) return "****";
		return "****" + phone.substring(phone.length() - 4);
	}

}
