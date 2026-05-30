-- V4__add_pending_cookie_rewards.sql
-- 식물 수확 등 서버에서 발생한 쿠키 보상을 프론트 요청 시점에 지급하기 위한 대기 보상 테이블

CREATE TABLE IF NOT EXISTS `pending_cookie_rewards` (
    `pending_reward_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`        DATETIME(6)  DEFAULT NULL,
    `user_id`           BIGINT       NOT NULL,
    `reward_type`       ENUM('BASIC_DAILY_FIRST_CONSUME','BASIC_FOOD_PHOTO_REG','BASIC_LOAD_RECIPE','BONUS_PLANT_HARVEST_REWARD','BONUS_RETENTION_REWARD','BONUS_URGENT_INGREDIENT_USE','BONUS_WEEKLY_GOAL_ACHIEVE','ONBOARDING_INGREDIENT','ONBOARDING_RECIPE','REVIVE_PLANT','WATERING') COLLATE utf8mb4_unicode_ci NOT NULL,
    `status`            ENUM('PENDING','CLAIMED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (`pending_reward_id`),
    KEY `FK_pending_cookie_rewards_user` (`user_id`),
    CONSTRAINT `FK_pending_cookie_rewards_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
