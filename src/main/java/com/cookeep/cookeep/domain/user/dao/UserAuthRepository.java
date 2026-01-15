package com.cookeep.cookeep.domain.user.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.UserAuth;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
	Optional<UserAuth> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
