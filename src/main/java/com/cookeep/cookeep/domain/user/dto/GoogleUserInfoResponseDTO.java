package com.cookeep.cookeep.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfoResponseDTO(
	@JsonProperty("id") String id,
	@JsonProperty("email") String email
) {
}
