package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserStatus;

@ExtendWith(MockitoExtension.class)
class LoginPasswordFailureServiceTest {

	@Mock
	private UserReader userReader;

	@Test
	@DisplayName("increasePasswordFailCount starts a new transaction")
	void increasePasswordFailCount_hasRequiresNewPropagation() throws NoSuchMethodException {
		Method method = LoginPasswordFailureService.class
			.getDeclaredMethod("increasePasswordFailCount", Long.class);

		Transactional transactional = method.getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
	}

	@Test
	@DisplayName("increasePasswordFailCount stores 1 when password count is null")
	void increasePasswordFailCount_nullPasswordCount_storesOne() {
		LoginPasswordFailureService service = new LoginPasswordFailureService(userReader);
		User user = user(UserStatus.ACTIVE);

		given(userReader.readById(user.getUserId())).willReturn(user);

		int passwordCnt = service.increasePasswordFailCount(user.getUserId());

		assertThat(passwordCnt).isEqualTo(1);
		assertThat(user.getPasswordCnt()).isEqualTo(1);
		assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("increasePasswordFailCount locks user when max attempts is reached")
	void increasePasswordFailCount_maxAttempts_locksUser() {
		LoginPasswordFailureService service = new LoginPasswordFailureService(userReader);
		User user = user(UserStatus.ACTIVE);
		user.updatePasswordCnt(4);

		given(userReader.readById(user.getUserId())).willReturn(user);

		int passwordCnt = service.increasePasswordFailCount(user.getUserId());

		assertThat(passwordCnt).isEqualTo(5);
		assertThat(user.getPasswordCnt()).isEqualTo(5);
		assertThat(user.getUserStatus()).isEqualTo(UserStatus.LOCK);
	}

	@Test
	@DisplayName("increasePasswordFailCount keeps max count and lock state when already over max attempts")
	void increasePasswordFailCount_alreadyOverMax_keepsLocked() {
		LoginPasswordFailureService service = new LoginPasswordFailureService(userReader);
		User user = user(UserStatus.LOCK);
		user.updatePasswordCnt(5);

		given(userReader.readById(user.getUserId())).willReturn(user);

		int passwordCnt = service.increasePasswordFailCount(user.getUserId());

		assertThat(passwordCnt).isEqualTo(5);
		assertThat(user.getPasswordCnt()).isEqualTo(5);
		assertThat(user.getUserStatus()).isEqualTo(UserStatus.LOCK);
	}

	private User user(UserStatus userStatus) {
		return User.builder()
			.userId(1L)
			.email("test@example.com")
			.password("encoded-password")
			.nickname("nickname")
			.userStatus(userStatus)
			.build();
	}
}
