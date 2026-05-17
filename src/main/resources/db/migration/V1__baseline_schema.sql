-- V1__baseline_schema.sql
-- 현재 배포된 DB의 스키마를 그대로 정의 (baseline용 - 실제로 실행되지 않음)
-- baseline-version: 1 로 지정 시 flyway_schema_history에 "이미 완료됨"으로만 기록됨

-- ============================================================
-- 독립 테이블 (외래키 없음)
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
                                       `user_id`                   BIGINT NOT NULL AUTO_INCREMENT,
                                       `cookie_cnt`                INT NOT NULL,
                                       `is_cookeeps_onboarded`     BIT(1) NOT NULL,
    `is_first_ingredient_reward` BIT(1) NOT NULL,
    `is_first_recipe_reward`    BIT(1) NOT NULL,
    `is_profile_auto_update`    BIT(1) NOT NULL,
    `marketing_consent`         BIT(1) DEFAULT NULL,
    `marketing_push`            BIT(1) DEFAULT NULL,
    `password_cnt`              INT DEFAULT NULL,
    `created_at`                DATETIME(6) DEFAULT NULL,
    `last_access_at`            DATETIME(6) DEFAULT NULL,
    `profile_plant_id`          BIGINT DEFAULT NULL,
    `updated_at`                DATETIME(6) DEFAULT NULL,
    `nickname`                  VARCHAR(10) COLLATE utf8mb4_unicode_ci NOT NULL,
    `phone_number`              VARCHAR(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `disliked_ingredients`      JSON DEFAULT NULL,
    `email`                     VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `password`                  VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `plant_status`              ENUM('FROZEN','NORMAL','WILTING') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_status`               ENUM('ACTIVE','CREATED','LOCK','WITHDRAWN') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname`),
    UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
    UNIQUE KEY `UK5f54qvapcu07iindngrju7rwl` (`profile_plant_id`),
    UNIQUE KEY `UK9q63snka3mdh91as4io72espi` (`phone_number`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `plants` (
                                        `plant_id`          BIGINT NOT NULL AUTO_INCREMENT,
                                        `growth_image_url`  VARCHAR(512) COLLATE utf8mb4_unicode_ci NOT NULL,
    `harvest_image_url` VARCHAR(512) COLLATE utf8mb4_unicode_ci NOT NULL,
    `seed_image_url`    VARCHAR(512) COLLATE utf8mb4_unicode_ci NOT NULL,
    `sprout_image_url`  VARCHAR(512) COLLATE utf8mb4_unicode_ci NOT NULL,
    `plant_name`        ENUM('APPLE','KIDNEY_BEAN','LETTUCE','POTATO','STRAWBERRY','TOMATO') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`plant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `default_ingredients` (
                                                     `default_ingredient_id`     BIGINT NOT NULL AUTO_INCREMENT,
                                                     `default_expiration_days`   INT DEFAULT NULL,
                                                     `default_ingredient`        VARCHAR(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `ai_tip`                    VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `image_url`                 VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `category`                  ENUM('BAKERY','BEAN','BEVERAGE','DAIRY_EGG','ETC','FERMENTED','FRUIT','GRAIN_RICE_NOODLE','MEAT','READY_MEAL','SEAFOOD','SEASONING_SAUCE','SNACK_DESSERT','VEGETABLE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `default_storage`           ENUM('FREEZER','FRIDGE','PANTRY') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `default_unit`              ENUM('BAG','BOTTLE','BUNDLE','CAN','GRAM','MILLILITER','PACK','PIECE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`default_ingredient_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `custom_ingredients` (
                                                    `custom_ingredient_id`      BIGINT NOT NULL AUTO_INCREMENT,
                                                    `custom_expiration_days`    INT NOT NULL,
                                                    `user_id`                   BIGINT NOT NULL,
                                                    `custom_ingredient`         VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `image_url`                 VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `custom_category`           ENUM('BAKERY','BEAN','BEVERAGE','DAIRY_EGG','ETC','FERMENTED','FRUIT','GRAIN_RICE_NOODLE','MEAT','READY_MEAL','SEAFOOD','SEASONING_SAUCE','SNACK_DESSERT','VEGETABLE') COLLATE utf8mb4_unicode_ci NOT NULL,
    `custom_storage`            ENUM('FREEZER','FRIDGE','PANTRY') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`custom_ingredient_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `nickname_actions` (
                                                  `action_id`     BIGINT NOT NULL AUTO_INCREMENT,
                                                  `action_name`   VARCHAR(10) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`action_id`),
    UNIQUE KEY `UKs5jc4m34w0ohbirqi0hk2ffe7` (`action_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `nickname_foods` (
                                                `food_id`   BIGINT NOT NULL AUTO_INCREMENT,
                                                `food_name` VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`food_id`),
    UNIQUE KEY `UK8di2f1mkds2y0npgblv2as5pw` (`food_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notices` (
                                         `notice_id`  BIGINT NOT NULL AUTO_INCREMENT,
                                         `created_at` DATETIME(6) DEFAULT NULL,
    `content`    TEXT COLLATE utf8mb4_unicode_ci NOT NULL,
    `title`      VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`notice_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `email_verifications` (
                                                     `email_verification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                                     `created_at`            DATETIME(6) DEFAULT NULL,
    `code`                  VARCHAR(10) COLLATE utf8mb4_unicode_ci NOT NULL,
    `email`                 VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `expires_at`            DATETIME(6) NOT NULL,
    `fail_count`            INT NOT NULL,
    `purpose`               ENUM('CHANGE_EMAIL','PASSWORD_VERIFICATION','RESET_PASSWORD','SIGNUP') COLLATE utf8mb4_unicode_ci NOT NULL,
    `verified_at`           DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`email_verification_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sms_verifications` (
                                                   `sms_verification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                                   `fail_count`          INT NOT NULL,
                                                   `code`                VARCHAR(6) COLLATE utf8mb4_unicode_ci NOT NULL,
    `created_at`          DATETIME(6) DEFAULT NULL,
    `expires_at`          DATETIME(6) NOT NULL,
    `verified_at`         DATETIME(6) DEFAULT NULL,
    `phone`               VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    `purpose`             ENUM('CHANGE_PHONE','PASSWORD_VERIFICATION','RESET_PASSWORD','SIGNUP') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`sms_verification_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- users 참조 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_auths` (
                                            `auth_id`          BIGINT NOT NULL AUTO_INCREMENT,
                                            `created_at`       DATETIME(6) DEFAULT NULL,
    `updated_at`       DATETIME(6) DEFAULT NULL,
    `user_id`          BIGINT NOT NULL,
    `password`         VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `provider_user_id` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `provider`         ENUM('GOOGLE','KAKAO','LOCAL') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`auth_id`),
    KEY `FKl35o65ovs04mariyxjj10ack1` (`user_id`),
    CONSTRAINT `FKl35o65ovs04mariyxjj10ack1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_sessions` (
                                               `session_id`    BIGINT NOT NULL AUTO_INCREMENT,
                                               `created_at`    DATETIME(6) DEFAULT NULL,
    `expires_at`    DATETIME(6) DEFAULT NULL,
    `user_id`       BIGINT NOT NULL,
    `refresh_token` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`session_id`),
    UNIQUE KEY `UKs53lpnfnkol367c935m8ue3fc` (`user_id`),
    CONSTRAINT `FK8klxsgb8dcjjklmqebqp1twd5` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_ingredients` (
                                                  `ingredients_id` BIGINT NOT NULL AUTO_INCREMENT,
                                                  `expiration_date` DATE NOT NULL,
                                                  `left_days`      INT NOT NULL,
                                                  `quantity`       INT NOT NULL,
                                                  `created_at`     DATETIME(6) DEFAULT NULL,
    `reference_id`   BIGINT NOT NULL,
    `user_id`        BIGINT NOT NULL,
    `batch_id`       VARCHAR(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `memo`           TEXT COLLATE utf8mb4_unicode_ci,
    `storage`        ENUM('FREEZER','FRIDGE','PANTRY') COLLATE utf8mb4_unicode_ci NOT NULL,
    `type`           ENUM('CUSTOM','DEFAULT') COLLATE utf8mb4_unicode_ci NOT NULL,
    `unit`           ENUM('BAG','BOTTLE','BUNDLE','CAN','GRAM','MILLILITER','PACK','PIECE') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`ingredients_id`),
    KEY `FK7m945oapbarut6bbkc3hn5g5h` (`user_id`),
    CONSTRAINT `FK7m945oapbarut6bbkc3hn5g5h` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `recent_ingredient_batch` (
                                                         `id`             BIGINT NOT NULL AUTO_INCREMENT,
                                                         `created_at`     DATETIME(6) DEFAULT NULL,
    `user_id`        BIGINT NOT NULL,
    `batch_id`       VARCHAR(36) COLLATE utf8mb4_unicode_ci NOT NULL,
    `ingredient_ids` JSON DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_recent_batch_user` (`user_id`),
    CONSTRAINT `FKpjc9jva3ehjonudwqvkinr23n` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `cookie_logs` (
                                             `cookie_log_id` BIGINT NOT NULL AUTO_INCREMENT,
                                             `amount`        INT NOT NULL,
                                             `created_at`    DATETIME(6) DEFAULT NULL,
    `user_id`       BIGINT NOT NULL,
    `type`          ENUM('BASIC_DAILY_FIRST_CONSUME','BASIC_FOOD_PHOTO_REG','BASIC_LOAD_RECIPE','BONUS_PLANT_HARVEST_REWARD','BONUS_RETENTION_REWARD','BONUS_URGENT_INGREDIENT_USE','BONUS_WEEKLY_GOAL_ACHIEVE','ONBOARDING_INGREDIENT','ONBOARDING_RECIPE','REVIVE_PLANT','WATERING') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`cookie_log_id`),
    KEY `FKeyk3jsdym25xvdk2rkjis36fv` (`user_id`),
    CONSTRAINT `FKeyk3jsdym25xvdk2rkjis36fv` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `daily_cookie_grants` (
                                                     `id`         BIGINT NOT NULL AUTO_INCREMENT,
                                                     `grant_date` DATE NOT NULL,
                                                     `created_at` DATETIME(6) DEFAULT NULL,
    `user_id`    BIGINT NOT NULL,
    `grant_type` ENUM('BASIC_DAILY_FIRST_CONSUME','BASIC_FOOD_PHOTO_REG','BASIC_LOAD_RECIPE','BONUS_PLANT_HARVEST_REWARD','BONUS_RETENTION_REWARD','BONUS_URGENT_INGREDIENT_USE','BONUS_WEEKLY_GOAL_ACHIEVE','ONBOARDING_INGREDIENT','ONBOARDING_RECIPE','REVIVE_PLANT','WATERING') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_date` (`user_id`,`grant_type`,`grant_date`),
    CONSTRAINT `FK9yip9imy4um52ubbwwjwasw16` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `weekly_goal` (
                                             `goal_id`          BIGINT NOT NULL AUTO_INCREMENT,
                                             `current_count`    INT NOT NULL,
                                             `is_achieved`      BIT(1) NOT NULL,
    `target_count`     INT NOT NULL,
    `week_start_date`  DATE NOT NULL,
    `created_at`       DATETIME(6) DEFAULT NULL,
    `user_id`          BIGINT DEFAULT NULL,
    `goal_action_type` ENUM('COOKING','PHOTO_RECORD','RECIPE_LIKE','USE_EXPIRING_INGREDIENT') COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`goal_id`),
    KEY `FKhecq5bkkykmhvbmjhc3sro9nh` (`user_id`),
    CONSTRAINT `FKhecq5bkkykmhvbmjhc3sro9nh` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `weekly_ingredient_logs` (
                                                        `id`                       BIGINT NOT NULL AUTO_INCREMENT,
                                                        `consumed`                 BIT(1) NOT NULL,
    `ever_near_expiry`         BIT(1) NOT NULL,
    `near_expiry_when_consumed` BIT(1) NOT NULL,
    `week_start_date`          DATE NOT NULL,
    `created_at`               DATETIME(6) DEFAULT NULL,
    `user_id`                  BIGINT NOT NULL,
    `user_ingredient_id`       BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK2m9j34m92rxn3ptvwgs0ly8h` (`user_id`,`week_start_date`,`user_ingredient_id`),
    CONSTRAINT `FKfuy4avn6d6rhiogxr8lq0honi` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `web_push_subscriptions` (
                                                        `id`         BIGINT NOT NULL AUTO_INCREMENT,
                                                        `created_at` DATETIME(6) DEFAULT NULL,
    `user_id`    BIGINT NOT NULL,
    `auth`       VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL,
    `p256dh`     VARCHAR(256) COLLATE utf8mb4_unicode_ci NOT NULL,
    `endpoint`   VARCHAR(512) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKt7scd5od188hu977a8yyp1436` (`endpoint`),
    KEY `FKfd4hay09484ebpqe6jiu5dmd2` (`user_id`),
    CONSTRAINT `FKfd4hay09484ebpqe6jiu5dmd2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- plants 참조 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_plants` (
                                             `user_plant_id` BIGINT NOT NULL AUTO_INCREMENT,
                                             `is_frozen`     BIT(1) NOT NULL,
    `is_harvested`  BIT(1) NOT NULL,
    `level`         INT NOT NULL,
    `water_count`   INT NOT NULL,
    `created_at`    DATETIME(6) DEFAULT NULL,
    `plant_id`      BIGINT NOT NULL,
    `user_id`       BIGINT NOT NULL,
    PRIMARY KEY (`user_plant_id`),
    KEY `FKloohu3h6rw3ecqf66i97r3thv` (`plant_id`),
    KEY `FKnbpr7n32dyrc5od00ljnfan1j` (`user_id`),
    CONSTRAINT `FKloohu3h6rw3ecqf66i97r3thv` FOREIGN KEY (`plant_id`) REFERENCES `plants` (`plant_id`),
    CONSTRAINT `FKnbpr7n32dyrc5od00ljnfan1j` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `watering_logs` (
                                               `watering_log_id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `created_at`      DATETIME(6) DEFAULT NULL,
    `user_id`         BIGINT NOT NULL,
    `user_plant_id`   BIGINT NOT NULL,
    PRIMARY KEY (`watering_log_id`),
    KEY `FKnf4uq0dvl00chrwf19sidhg2v` (`user_id`),
    KEY `FK262q5uhw3kh4fpohcn1w73b6j` (`user_plant_id`),
    CONSTRAINT `FK262q5uhw3kh4fpohcn1w73b6j` FOREIGN KEY (`user_plant_id`) REFERENCES `user_plants` (`user_plant_id`),
    CONSTRAINT `FKnf4uq0dvl00chrwf19sidhg2v` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- users FK 후처리: profile_plant_id -> user_plants
-- (users 테이블 생성 시점에 user_plants가 없으므로 ALTER로 추가)
-- ============================================================

ALTER TABLE `users`
    ADD CONSTRAINT `FK2rjifh48h13w869flwypbuup8`
        FOREIGN KEY IF NOT EXISTS (`profile_plant_id`) REFERENCES `user_plants` (`user_plant_id`);

-- ============================================================
-- AI 관련 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_sessions` (
                                             `ai_sessions_id`       BIGINT NOT NULL AUTO_INCREMENT,
                                             `attempt_number`       INT DEFAULT NULL,
                                             `has_urgent_ingredient` BIT(1) NOT NULL,
    `is_completed`         BIT(1) DEFAULT NULL,
    `is_pinned`            BIT(1) DEFAULT NULL,
    `completed_at`         DATETIME(6) DEFAULT NULL,
    `created_at`           DATETIME(6) DEFAULT NULL,
    `updated_at`           DATETIME(6) DEFAULT NULL,
    `user_id`              BIGINT NOT NULL,
    `ingredient_ids`       JSON DEFAULT NULL,
    `title`                VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_ingredient_ids`  JSON DEFAULT NULL,
    `difficulty`           ENUM('EASY','HARD','NORMAL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`ai_sessions_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_messages` (
                                             `ai_messages_id` BIGINT NOT NULL AUTO_INCREMENT,
                                             `ai_sessions_id` BIGINT NOT NULL,
                                             `created_at`     DATETIME(6) DEFAULT NULL,
    `content`        TEXT COLLATE utf8mb4_unicode_ci,
    `message_type`   ENUM('ADOPT_RECIPE','INITIAL_REQUEST','RETRY_REQUEST') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `role`           ENUM('AI','USER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`ai_messages_id`),
    KEY `FK520gxpbx51sp4330nwv7q5xmh` (`ai_sessions_id`),
    CONSTRAINT `FK520gxpbx51sp4330nwv7q5xmh` FOREIGN KEY (`ai_sessions_id`) REFERENCES `ai_sessions` (`ai_sessions_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_recipes` (
                                            `ai_recipes_id`         BIGINT NOT NULL AUTO_INCREMENT,
                                            `ai_sessions_id`        BIGINT NOT NULL,
                                            `created_at`            DATETIME(6) DEFAULT NULL,
    `user_id`               BIGINT NOT NULL,
    `ai_recipes_ingredients` JSON DEFAULT NULL,
    `ai_recipes_steps`      JSON DEFAULT NULL,
    `ai_recipes_title`      VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `youtube_search_queries` TEXT COLLATE utf8mb4_unicode_ci,
    `youtube_url`           JSON DEFAULT NULL,
    PRIMARY KEY (`ai_recipes_id`),
    KEY `FKnsal7htfu2budn48l6b1akoy9` (`ai_sessions_id`),
    CONSTRAINT `FKnsal7htfu2budn48l6b1akoy9` FOREIGN KEY (`ai_sessions_id`) REFERENCES `ai_sessions` (`ai_sessions_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- daily_recipes (ai_recipes, users 참조)
-- ============================================================

CREATE TABLE IF NOT EXISTS `daily_recipes` (
                                               `daily_recipe_id`          BIGINT NOT NULL AUTO_INCREMENT,
                                               `is_photo_reward_completed` BIT(1) NOT NULL,
    `is_public`                BIT(1) NOT NULL,
    `like_count`               INT NOT NULL,
    `ai_recipes_id`            BIGINT DEFAULT NULL,
    `created_at`               DATETIME(6) DEFAULT NULL,
    `user_id`                  BIGINT NOT NULL,
    `title`                    VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `recipe_image_url`         VARCHAR(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `description`              VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `content`                  TEXT COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`daily_recipe_id`),
    KEY `idx_daily_recipes_public_created` (`is_public`,`created_at` DESC),
    KEY `idx_daily_recipes_public_likes` (`is_public`,`like_count` DESC,`created_at` DESC),
    KEY `FKgesea8m4il1q7rky5apwk0f66` (`ai_recipes_id`),
    KEY `FKkcaj757sdcqsugegbr1gwtckl` (`user_id`),
    CONSTRAINT `FKgesea8m4il1q7rky5apwk0f66` FOREIGN KEY (`ai_recipes_id`) REFERENCES `ai_recipes` (`ai_recipes_id`),
    CONSTRAINT `FKkcaj757sdcqsugegbr1gwtckl` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `recipe_bookmarks` (
                                                  `recipe_bookmark_id` BIGINT NOT NULL AUTO_INCREMENT,
                                                  `created_at`         DATETIME(6) DEFAULT NULL,
    `daily_recipe_id`    BIGINT NOT NULL,
    `user_id`            BIGINT NOT NULL,
    PRIMARY KEY (`recipe_bookmark_id`),
    UNIQUE KEY `UKmxs9i1tkosi9xxsl4avlew9pe` (`daily_recipe_id`,`user_id`),
    KEY `FKfm2l0nfe1112rwrv3ql6ax6ny` (`user_id`),
    CONSTRAINT `FKfm2l0nfe1112rwrv3ql6ax6ny` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `FKndx3gp45nvqtlku176gxq4q22` FOREIGN KEY (`daily_recipe_id`) REFERENCES `daily_recipes` (`daily_recipe_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `recipe_likes` (
                                              `recipe_like_id`  BIGINT NOT NULL AUTO_INCREMENT,
                                              `created_at`      DATETIME(6) DEFAULT NULL,
    `daily_recipe_id` BIGINT NOT NULL,
    `user_id`         BIGINT NOT NULL,
    PRIMARY KEY (`recipe_like_id`),
    UNIQUE KEY `UKp5nixgpk99svxj2a42jabdqis` (`daily_recipe_id`,`user_id`),
    KEY `FK3h43xv6xr4dr42hpl8xym711d` (`user_id`),
    CONSTRAINT `FK3h43xv6xr4dr42hpl8xym711d` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `FKrekn1q1ux4g5674ugm3vo92io` FOREIGN KEY (`daily_recipe_id`) REFERENCES `daily_recipes` (`daily_recipe_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;