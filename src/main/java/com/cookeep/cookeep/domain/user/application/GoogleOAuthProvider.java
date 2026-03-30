package com.cookeep.cookeep.domain.user.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.cookeep.cookeep.domain.user.dto.GoogleUserInfoResponseDTO;
import com.cookeep.cookeep.domain.user.dto.OAuthUserInfoDTO;
import com.cookeep.cookeep.domain.user.dto.SocialTokenResponseDTO;
import com.cookeep.cookeep.domain.user.entity.Provider;

import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthProvider implements OAuthProvider {

	@Value("${google.auth.client}")
	private String clientId;
	@Value("${google.auth.secret}")
	private String clientSecret;
	@Value("${google.auth.access-token-url}")
	private String GoogleAccessTokenURL;
	@Value("${google.auth.user-info-url}")
	private String GoogleUserInfoURL;
	@Value("${google.auth.redirect}")
	private String defaultRedirectUri;

	@Override
	public Provider provider() {
		return Provider.GOOGLE;
	}

	// 인가코드를 사용해 로그인할 유저의 구글 액세스 토큰값을 받아옴
	public String getAccessToken(String code, String redirectUri) {
		String actualRedirectUri = (redirectUri != null) ? redirectUri : defaultRedirectUri;

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("redirect_uri", actualRedirectUri);
		form.add("code", code);

		SocialTokenResponseDTO socialTokenResponseDTO = WebClient.create()
			.post()
			.uri(GoogleAccessTokenURL)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(BodyInserters.fromFormData(form))
			.retrieve()
			.onStatus(HttpStatusCode::isError, res ->
				res.bodyToMono(String.class).defaultIfEmpty("")
					.doOnNext(
						body -> log.error("[GOOGLE] /oauth/token error status={}, body={}", res.statusCode(), body))
					.then(Mono.error(new RuntimeException("구글 로그인 요청에 실패하였습니다.")))
			)
			.bodyToMono(SocialTokenResponseDTO.class)
			.block();

		return socialTokenResponseDTO.accessToken();
	}

	// 구글 액세스 토큰을 사용해 유저의 정보값을 받아옴
	public OAuthUserInfoDTO getUserInfo(String accessToken) {
		GoogleUserInfoResponseDTO googleUserInfo = WebClient.create(GoogleUserInfoURL)
			.get()
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // access token 인가
			.header(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
			.retrieve()
			.bodyToMono(GoogleUserInfoResponseDTO.class)
			.block();

		return new OAuthUserInfoDTO(
			googleUserInfo.id(), googleUserInfo.email()
		);
	}
}
