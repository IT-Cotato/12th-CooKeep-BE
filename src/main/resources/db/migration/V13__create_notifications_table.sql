-- src/main/resources/db/migration/V13__create_notifications_table.sql

-- 알림함(서비스 알림 탭)에 노출되는 개별 알림 발송 이력을 저장
-- 30일 이내의 알림 보관

CREATE TABLE IF NOT EXISTS `notifications` (
                                               `notification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `created_at`       DATETIME(6) DEFAULT NULL,
    `user_id`          BIGINT NOT NULL,
    `type`             VARCHAR(30) COLLATE utf8mb4_unicode_ci NOT NULL,
    `title`            VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `body`             TEXT COLLATE utf8mb4_unicode_ci NOT NULL,
    `url`              VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `is_read`          BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`notification_id`),
    KEY `idx_notifications_user_created_at` (`user_id`, `created_at`),
    KEY `idx_notifications_created_at` (`created_at`),
    CONSTRAINT `FK_notifications_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
