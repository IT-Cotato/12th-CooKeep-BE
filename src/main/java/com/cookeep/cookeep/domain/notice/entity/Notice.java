package com.cookeep.cookeep.domain.notice.entity;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cookeep.cookeep.common.entity.BaseEntity;

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
@Table(name = "notices")
public class Notice extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long noticeId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NoticeType type;

	// 신규 공지 등록용 빌더 - type을 지정하지 않으면 NOTICE_TYPE_REQUIRED 예외 발생
	@Builder
	private Notice(String title, String content, NoticeType type) {
		if (type == null) {
			throw new AppException(ErrorCode.NOTICE_TYPE_REQUIRED);
		}
		this.title = title;
		this.content = content;
		this.type = type;
	}

}
