package com.cookeep.cookeep.api.dto.user;

public record UserContext(
    Long userId,
    String nickname,
    int cookieCount
) {}
