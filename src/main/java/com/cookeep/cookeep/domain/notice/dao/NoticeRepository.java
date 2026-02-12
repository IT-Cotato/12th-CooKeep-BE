package com.cookeep.cookeep.domain.notice.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.notice.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

	// 전체 공지를 최신순으로 조회함
	List<Notice> findAllByOrderByCreatedAtDesc();
}
