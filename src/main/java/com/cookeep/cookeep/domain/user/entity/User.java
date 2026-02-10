package com.cookeep.cookeep.domain.user.entity;

import java.time.LocalDateTime;

import com.cookeep.cookeep.domain.plant.entity.PlantStatus;
import com.cookeep.cookeep.domain.plant.entity.UserPlant;
import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cookeep.cookeep.common.entity.BaseEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "Users")
public class User extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@Column(length = 10, nullable = false, unique = true)
	private String nickname;

	// 소셜 로그인은 전화번호를 수집하지 않으므로 nullable
	// 전화번호는 010으로 시작하는 11자리 숫자여야 함
	@Pattern(regexp = "^010\\d{8}$")
	@Column(length = 15, unique = true)
	private String phoneNumber;

	@Email
	@Column(length = 255, nullable = false, unique = true)
	private String email;

	@Column(length = 255)
	private String password;

	/**
	 소셜 로그인 회원의 경우 회원가입 및 로그인 완료 후에 약관 동의 페이지로 넘어가므로
	 marketingConsent와 marketingPush 모두 nullable로 둠
	 null: 아직 선택하지 않음 (소셜 로그인 직후)
	 true: 동의, false: 미동의
	 */
	private Boolean marketingConsent;

	// 소셜 로그인은 가입 완료 이후에 설정하므로 setter 필요
	public void setMarketingConsent(Boolean marketingConsent) {
		this.marketingConsent = marketingConsent;
	}

	// 온보딩 과정에서 알림 켜기를 선택한 경우로 true로 업데이트되므로 알림 켜기에 동의하지 않은 회원은 marketingPush 값이 null로 남게 됨
	// 따라서 디폴트값 false로 설정, 알림 켜기 선택시에 true로 변경하도록 함
	@Builder.Default
	private Boolean marketingPush = false;

	public void setMarketingPush(Boolean marketingPush) {
		this.marketingPush = marketingPush;
	}

	// 비밀번호 오류 횟수, 5회 오류시 LOCKED 상태됨
	// 소셜로그인 회원은 별도로 카운트하지 않으므로 nullable
	@Column(nullable = true)
	private Integer passwordCnt;

	// @Builder 어노테이션 사용중이므로 기본값이 있는 필드에 @Builder.Default 추가
	@Column(nullable = false)
	@Builder.Default
	private int cookieCnt = 0;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus userStatus = UserStatus.CREATED;

	@LastModifiedDate
	private LocalDateTime updatedAt;

	private LocalDateTime lastAccessAt;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	private PlantStatus plantStatus = PlantStatus.NORMAL;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profile_plant_id")
	private UserPlant profilePlant;

	@Builder.Default
	@Column(nullable = false)
	private boolean isProfileAutoUpdate = true; // 프로필 식물 변경을 자동 or 수동 모드 관리

	// 유저가 API를 통해 직접 프로필을 변경할 때 호출
	public void updateProfilePlant(UserPlant nesUserPlant) {
		this.profilePlant = nesUserPlant;
		this.isProfileAutoUpdate = false; // 수동으로 변경하면 자동 갱신 모드 해제
	}

	// 새로운 식물 등록 시 시스템에 의해 호출
	public void setProfilePlantAuto(UserPlant userPlant) {
		if (this.isProfileAutoUpdate) {
			this.profilePlant = userPlant;
		}
	}

	public void updateCookieCnt(int amount) {
		this.cookieCnt += amount;
	}

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	public void updateLastAccessAt(LocalDateTime lastAccessAt) {
		this.lastAccessAt = lastAccessAt;
	}

	public void updatePlantStatus(PlantStatus plantStatus) {
		this.plantStatus = plantStatus;
	}

	public void activate() {
		this.userStatus = UserStatus.ACTIVE;
	}

}