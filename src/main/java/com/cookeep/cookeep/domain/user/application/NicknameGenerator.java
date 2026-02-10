package com.cookeep.cookeep.domain.user.application;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.nickname.dao.NicknameActionRepository;
import com.cookeep.cookeep.domain.nickname.dao.NicknameFoodRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameGenerator {
	private final NicknameActionRepository nicknameActionRepository;
	private final NicknameFoodRepository nicknameFoodRepository;

	private List<String> actions;
	private List<String> foods;

	@PostConstruct // 서버 시작 시 NicknameGenerator 빈이 초기화된 직후에 한 번 실행
		// 회원가입 시점에 seed 누락으로 인한 오류를 방지하기 위해
		// 서버 시작 단계에서 닉네임 seed 데이터를 검증 및 로딩하도록 하였음
	void loadNicknameList() {
		actions = nicknameActionRepository.findAllActionNames();
		foods = nicknameFoodRepository.findAllFoodNames();
		if (actions.isEmpty() || foods.isEmpty()) {
			log.debug("[ERROR] Nickname seed empty: actions=" + actions.size() + ", foods=" + foods.size());
			throw new AppException(ErrorCode.NICKNAME_GENERATION_UNAVAILABLE);
		}
	}

	String generateRandomNickname() {
		// 웹 서버는 멀티 스레드 환경이므로
		// 멀티 스레드 환경에서 Random의 동기화 비용을 피하기 위해 ThreadLocalRandom을 사용함
		int actionIndex = ThreadLocalRandom.current().nextInt(actions.size());
		int foodIndex   = ThreadLocalRandom.current().nextInt(foods.size());

		String action = actions.get(actionIndex);
		String food   = foods.get(foodIndex);

		return action + " " + food;
	}
}
