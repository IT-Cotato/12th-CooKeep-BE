package com.cookeep.cookeep.common.util;

public final class InboxPolicy {

    // 알림함에는 최근 30일 이내 공지만 노출됨
    public static final int RETENTION_DAYS = 30;

    private InboxPolicy() {
    }
}
