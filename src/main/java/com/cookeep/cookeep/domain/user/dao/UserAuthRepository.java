package com.cookeep.cookeep.domain.user.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserAuth;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
	Optional<UserAuth> findByProviderAndProviderUserId(Provider provider, String providerUserId);

	List<UserAuth> findAllByUser(User user);

	@Query("select ua.provider from UserAuth ua where ua.user.userId = :userId")
	Provider findProviderByUserId(@Param("userId") Long userId);
}
