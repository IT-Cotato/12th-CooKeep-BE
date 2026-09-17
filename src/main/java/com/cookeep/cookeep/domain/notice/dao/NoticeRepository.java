package com.cookeep.cookeep.domain.notice.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

	// 전체 공지를 최신순으로 조회함
	List<Notice> findAllByOrderByCreatedAtDesc();

	// 최근 30일 이내 공지를 최신순으로 조회
	List<Notice> findAllByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

	// 기준 시각 이전에 등록된 공지 일괄 삭제
	@Modifying
	@Query("DELETE FROM Notice n WHERE n.createdAt < :before")
	int deleteAllByCreatedAtBefore(@Param("before") LocalDateTime before);


}
