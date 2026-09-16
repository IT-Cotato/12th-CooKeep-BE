package com.cookeep.cookeep.domain.notification.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "notifications",
        indexes = {
                // 알림함 목록 조회(30일 이내/최신순) 복합 인덱스
                @Index(name = "idx_notifications_user_created_at", columnList = "user_id, created_at"),
                // 만료된(30일 경과) 알림 일괄 삭제 스케줄러용 인덱스
                @Index(name = "idx_notifications_created_at", columnList = "created_at")
        }
)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // EXPIRATION / PLANT_WILTING / PLANT_GROWTH_STOP
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    // 클릭 시 이동할 경로 (예: /fridge, /cookeeps)
    @Column(nullable = false)
    private String url;

    // 알림함에서의 읽음 여부 (이미지의 회색 처리된 알림)
    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    // 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }
}
