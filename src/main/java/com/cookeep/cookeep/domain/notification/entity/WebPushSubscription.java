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
        name = "web_push_subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "endpoint")
        }
)
public class WebPushSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Push 전송 주소 (기기/브라우저 마다 다름)
    @Column(nullable = false, unique = true, length = 512)
    private String endpoint;

    // VAPID 공개키
    @Column(nullable = false, length = 256)
    private String p256dh;

    // VAPID 인증키
    @Column(nullable = false, length = 64)
    private String auth;
}
