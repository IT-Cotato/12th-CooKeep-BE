package com.cookeep.cookeep.domain.user.entity;

import java.time.LocalDateTime;

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
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long userId;

	// 닉네임 처리는 추후 값 전달받은 후에 업데이트할 예정
	//@Column(length = 20, nullable = false, unique = true)
	@Column(length = 20, unique = true)
	private String nickname;

	// 소셜 로그인은 전화번호를 수집하지 않으므로 nullable
	// 전화번호는 010으로 시작하는 11자리 숫자여야 함
	@Pattern(regexp = "^010\\d{8}$")
	@Column(length = 15, unique = true)
	private String phoneNumber;

	@Email
	@Column(length = 255, nullable = false, unique = true)
	private String email;

	/**
	소셜 로그인 회원의 경우 회원가입 및 로그인 완료 후에 약관 동의 페이지로 넘어가므로
	marketingConsent와 marketingPush 모두 nullable로 둠
	null: 아직 선택하지 않음 (소셜 로그인 직후)
	true: 동의, false: 미동의
	 */
	private Boolean marketingConsent;

	private Boolean marketingPush;

	// 비밀번호 오류 횟수, 5회 오류시 LOCKED 상태됨
	// 소셜로그인 회원은 별도로 카운트하지 않으므로 nullable
	private Integer passwordCnt;

	// @Builder 어노테이션 사용중이므로 기본값이 있는 필드에 @Builder.Default 추가
	@Column(nullable = false)
	private int cookieCnt = 0;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus userStatus = UserStatus.CREATED;

	@LastModifiedDate
	private LocalDateTime updatedAt;

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
}