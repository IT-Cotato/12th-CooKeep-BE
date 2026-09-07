-- V{NEXT}__ai_session_difficulty_to_feature.sql
-- ai_sessions 테이블의 difficulty 컬럼을 feature 컬럼으로 교체

-- 1. feature 컬럼 추가
ALTER TABLE `ai_sessions`
    ADD COLUMN `feature`
        ENUM('SOUP_STEW','RICE_BOWL','NOODLE','STIR_FRY_GRILL','SALAD_HEALTHY','SNACK_DESSERT','ANY')
        COLLATE utf8mb4_unicode_ci DEFAULT NULL
        AFTER `updated_at`;

-- 2. 기존 difficulty 값을 feature로 이관
--    (EASY/NORMAL/HARD 는 요리 종류가 아니므로 ANY로 매핑)
UPDATE `ai_sessions`
SET `feature` = 'ANY'
WHERE `difficulty` IS NOT NULL;

-- 3. 기존 difficulty 컬럼 삭제
ALTER TABLE `ai_sessions`
DROP COLUMN `difficulty`;