package com.cookeep.cookeep.domain.Users.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.Plant.entity.UserPlant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Users") // 매핑할 테이블 이름
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자
@AllArgsConstructor
@Builder
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AI(Auto Increment) 전략
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 20) // 닉네임 (최대 20자, Not Null)
    private String nickname;

    @Column(name = "cookie_cnt", nullable = false) // 현재 보유 쿠키 개수
    @Builder.Default
    private Integer cookieCnt = 0; // 초기값 0

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_plant_id")
    private UserPlant profilePlant;

    // 프로필 식물 변경
    public void updateProfilePlant(UserPlant newUserPlant) {
        this.profilePlant = newUserPlant;
    }
}
