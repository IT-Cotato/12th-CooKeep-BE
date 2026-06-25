package com.cookeep.cookeep.domain.user.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginPasswordFailureService {

	static final int MAX_PASSWORD_ATTEMPTS = 5;

	private final UserReader userReader;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int increasePasswordFailCount(Long userId) {
		User user = userReader.readById(userId);
		int passwordCnt = Optional.ofNullable(user.getPasswordCnt()).orElse(0) + 1;

		if (passwordCnt >= MAX_PASSWORD_ATTEMPTS) {
			user.updatePasswordCnt(MAX_PASSWORD_ATTEMPTS);
			user.updateUserStatus(UserStatus.LOCK);
			log.warn("User account locked by login password failures. userId={}", userId);
			return MAX_PASSWORD_ATTEMPTS;
		}

		user.updatePasswordCnt(passwordCnt);
		return passwordCnt;
	}
}
