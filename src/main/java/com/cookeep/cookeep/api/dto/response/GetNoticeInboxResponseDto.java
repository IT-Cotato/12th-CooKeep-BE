package com.cookeep.cookeep.api.dto.response;


import com.cookeep.cookeep.domain.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "GetNoticeInboxResponse",
        description = "알림함 - 공지사항 목록 조회 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class GetNoticeInboxResponseDto {

    @Schema(description = "공지사항 목록 (최근 30일 이내, 최신순)")
    private List<NoticeItem> notices;

    @Schema(
            name = "NoticeItem",
            description = "공지사항 항목"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class NoticeItem {

        @Schema(description = "공지사항 ID", example = "3")
        private Long noticeId;

        @Schema(description = "공지사항 제목", example = "버전 업데이트 안내")
        private String title;

        @Schema(description = "공지사항 내용", example = "v.2.1.0 업데이트가 되었어요~")
        private String content;

        @Schema(description = "공지사항 등록 시각", example = "2026-09-08T09:26:00")
        private LocalDateTime createdAt;

        public static NoticeItem from(Notice notice) {
            return NoticeItem.builder()
                    .noticeId(notice.getNoticeId())
                    .title(notice.getTitle())
                    .content(notice.getContent())
                    .createdAt(notice.getCreatedAt())
                    .build();
        }
    }
}
