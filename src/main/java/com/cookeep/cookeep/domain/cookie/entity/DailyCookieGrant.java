package com.cookeep.cookeep.domain.cookie.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "Daily_Cookie_Grants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_type_date",
                        columnNames = {"user_id", "grant_type", "grant_date"}
                )
        }
)
public class DailyCookieGrant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", nullable = false)
    private CookieLog.CookieLogType grantType;

    @Column(name = "grant_date", nullable = false)
    private LocalDate grantDate;

}
