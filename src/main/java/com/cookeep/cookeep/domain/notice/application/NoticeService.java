package com.cookeep.cookeep.domain.notice.application;

import java.util.List;

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

	// 공지사항 조회
	public List<GetNoticeResponseDTO> getNotices() {
		return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
			.map(n -> new GetNoticeResponseDTO(n.getNoticeId(), n.getTitle(), n.getContent()))
			.toList();
	}
}
