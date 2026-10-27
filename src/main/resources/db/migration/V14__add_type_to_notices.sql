-- src/main/resources/db/migrationV14__add_type_to_notices.sql

-- notices 테이블에 공지 종류(type) 컬럼 추가
-- type이 없는 기존 데이터는 DEFUALT = GENERAL로 설정
ALTER TABLE notices
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'GENERAL';

-- null 방지
UPDATE notices
SET type = 'GENERAL'
WHERE type IS NULL OR type = '';

-- 알림함 조회 쿼리
CREATE INDEX idx_notices_type_created_at ON notices (type, created_at);