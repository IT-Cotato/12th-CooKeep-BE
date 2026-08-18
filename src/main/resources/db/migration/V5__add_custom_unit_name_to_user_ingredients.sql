-- V5__add_custom_unit_name_to_user_ingredient.sql
-- 식재료 등록 시 "직접입력" 단위 선택을 지원하기 위한 스키마 변경

ALTER TABLE `user_ingredients`
    MODIFY COLUMN `unit` ENUM('PIECE','PACK','BAG','BOTTLE','BUNDLE','CAN','GRAM','MILLILITER','CUSTOM')
    COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE `user_ingredients`
    ADD COLUMN `custom_unit_name` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `unit`;