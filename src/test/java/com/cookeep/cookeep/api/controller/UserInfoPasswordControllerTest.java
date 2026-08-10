package com.cookeep.cookeep.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.cookeep.cookeep.api.dto.request.UpdatePasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyPasswordRequestDTO;
import com.cookeep.cookeep.api.dto.response.ReauthenticationResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.user.application.UserInfoService;
import com.cookeep.cookeep.security.RefreshTokenCookieProvider;
import com.cookeep.cookeep.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class UserInfoPasswordControllerTest {

	@Mock
	private UserInfoService userInfoService;

	private UserInfoController controller;

	@BeforeEach
	void setUp() {
		controller = new UserInfoController(
			userInfoService,
			new RefreshTokenCookieProvider(true, "Lax")
		);
	}

	@Test
	void verifyPasswordReturnsReauthenticationTokenAndExpiry() {
		VerifyPasswordRequestDTO request = new VerifyPasswordRequestDTO("old-password1");
		given(userInfoService.verifyMyPassword(1L, request))
			.willReturn(new ReauthenticationResponseDTO("reauth-token", 300));

		ResponseEntity<DataResponse<ReauthenticationResponseDTO>> response =
			controller.verifyMyPassword(new UserPrincipal(1L), request);

		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData().reauthToken()).isEqualTo("reauth-token");
		assertThat(response.getBody().getData().expiresInSeconds()).isEqualTo(300);
	}

	@Test
	void updatePasswordPassesHeaderAndExpiresRefreshCookie() {
		UpdatePasswordRequestDTO request =
			new UpdatePasswordRequestDTO("new-password1", "new-password1");

		ResponseEntity<DataResponse<Void>> response = controller.updateMyPassword(
			new UserPrincipal(1L),
			"reauth-token",
			request
		);

		verify(userInfoService).updateMyPassword(1L, "reauth-token", request);
		assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
			.contains("refreshToken=")
			.contains("Path=/api/auth/refresh")
			.contains("Max-Age=0")
			.contains("Secure")
			.contains("HttpOnly")
			.contains("SameSite=Lax");
	}
}
