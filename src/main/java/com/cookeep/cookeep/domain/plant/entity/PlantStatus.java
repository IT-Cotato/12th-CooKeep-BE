package com.cookeep.cookeep.domain.plant.entity;

public enum PlantStatus {
	NORMAL,   // 정상 (미접속 0~6일)
	WILTING,  // 시들어 가는 중 (미접속 7~13일)
	FROZEN    // 시든 상태 = 성장 정지 (미접속 14일 이상)
}
