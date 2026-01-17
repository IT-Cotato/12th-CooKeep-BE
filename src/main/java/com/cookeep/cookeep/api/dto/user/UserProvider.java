package com.cookeep.cookeep.api.dto.user;

import org.springframework.stereotype.Component;

@Component
public class UserProvider {

    // 현재 로그인한 유저의 정보를 가져오는 메서드
    public UserContext getUserContext() {
        // 지금은 임시 데이터를 반환
        // 나중에 시큐리티 담당자가 이 부분을 실제 로그인 유저 정보로 채우면 됩니다.
        return new UserContext(1L, "임시유저", 10);
    }

    public Long getCurrentUserId() {
        return getUserContext().userId();
    }
}
