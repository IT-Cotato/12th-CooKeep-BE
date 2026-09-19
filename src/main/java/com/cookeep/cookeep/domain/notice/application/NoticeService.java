package com.cookeep.cookeep.domain.notice.application;

import java.time.LocalDateTime;
import java.util.List;

import com.cookeep.cookeep.api.dto.response.GetNoticeInboxResponseDto;
import org.springframework.stereotype.Service;

import com.cookeep.cookeep.api.dto.response.GetNoticeResponseDTO;
import com.cookeep.cookeep.domain.notice.dao.NoticeRepository;
import com.cookeep.cookeep.domain.notice.entity.Notice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {
	private final NoticeRepository noticeRepository;

	// 알림함에는 최근 30일 이내 공지만 노출됨
	private static final int RETENTION_DAYS = 30;

	// 공지사항 조회
	public List<GetNoticeResponseDTO> getNotices() {
		return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
			.map(n -> new GetNoticeResponseDTO(n.getNoticeId(), n.getTitle(), n.getContent()))
			.toList();
	}

	// 알림함 - 공지사항 목록 조회 (최근 30일, 최신순)
	public GetNoticeInboxResponseDto getNoticeInbox() {
		LocalDateTime after = LocalDateTime.now().minusDays(RETENTION_DAYS);

		List<GetNoticeInboxResponseDto.NoticeItem> notices = noticeRepository.findAllByCreatedAtAfterOrderByCreatedAtDesc(after).stream()
				.map(GetNoticeInboxResponseDto.NoticeItem::from)
				.toList();

		return GetNoticeInboxResponseDto.builder()
				.notices(notices)
				.build();
	}
}
