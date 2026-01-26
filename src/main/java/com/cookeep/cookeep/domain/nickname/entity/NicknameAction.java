package com.cookeep.cookeep.domain.nickname.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "nickname_actions")
public class NicknameAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long id;

    @Column(name = "action_name", length = 10, nullable = false, unique = true)
    private String actionName;
}