package com.cookeep.cookeep.domain.notice.entity;

// 공지사항 type 결정
public enum NoticeType {

    GENERAL, // 일반 공지사항 - 상시 노출, 자동 삭제X
    INBOX; //알림함 공지사항 - 최근 30일만 노출 (30일 경과 데이터는 사용자에게 노출X)
}
