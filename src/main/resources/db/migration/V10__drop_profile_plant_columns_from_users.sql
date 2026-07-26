-- src/main/resources/db/migration/V10__drop_profile_plant_columns_from_users.sql

-- FK 제약조건 먼저 제거 (컬럼/인덱스보다 선행되어야 함)
ALTER TABLE users
DROP FOREIGN KEY FK2rjifh48h13w869flwypbuup8;

-- UNIQUE 인덱스 제거
ALTER TABLE users
DROP INDEX UK5f54qvapcu07iindngrju7rwl;

-- 컬럼 제거
ALTER TABLE users
DROP COLUMN profile_plant_id,
    DROP COLUMN is_profile_auto_update;