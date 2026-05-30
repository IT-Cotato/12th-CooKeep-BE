package com.cookeep.cookeep.domain.cookie.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pending_cookie_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingCookieReward extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pendingRewardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CookieLog.CookieLogType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingRewardStatus status;

    // status는 생성 시 항상 PENDING으로 고정
    public PendingCookieReward(User user, CookieLog.CookieLogType rewardType) {
        this.user = user;
        this.rewardType = rewardType;
        this.status = PendingRewardStatus.PENDING;
    }

    public void claim() {
        if (status == PendingRewardStatus.CLAIMED) {
            throw new AppException(ErrorCode.PENDING_REWARD_ALREADY_CLAIMED);
        }
        status = PendingRewardStatus.CLAIMED;
    }

    public enum PendingRewardStatus {
        PENDING, CLAIMED
    }
}
