-- src/main/resources/db/migration/V12__drop_add_custom_unit_name_to_user_ingredient.sql

-- Unit ENUM에 CUSTOM 추가
ALTER TABLE user_ingredients
    MODIFY COLUMN unit ENUM('PIECE','PACK','BAG','BOTTLE','BUNDLE','CAN','GRAM','MILLILITER','CUSTOM') NOT NULL;


-- user_ingredients에 custom_unit_name 컬럼 추가
ALTER TABLE user_ingredients ADD COLUMN custom_unit_name VARCHAR(255) NULL;