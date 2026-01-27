package com.cookeep.cookeep.domain.onboarding.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyGoal extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long goalId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GoalActionType goalActionType;
	// COOKING, PHOTO_RECORD, USE_EXPIRING_INGREDIENT, RECIPE_LIKE

	@Column(nullable = false)
	private int targetCount;   // 목표 횟수

	@Builder.Default
	private int currentCount = 0;  // 현재 달성 횟수

	@Column(nullable = false)
	private LocalDate weekStartDate; // 해당 주차 시작일

	@Builder.Default
	private boolean isAchieved = false; // 달성 여부 (보상 중복 지급 방지)

	public void incrementCount() {
		this.currentCount++;
		if (this.currentCount >= this.targetCount) {
			this.isAchieved = true;
		}
	}

	public void initWeekStartDate() {
		// 현재 날짜를 받아와서 해당 주차 시작일(월요일)로 값을 설정함
		this.weekStartDate = LocalDate.now()
			.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}
}