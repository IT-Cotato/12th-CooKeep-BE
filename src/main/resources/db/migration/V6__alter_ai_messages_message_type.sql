-- src/main/resources/db/migration/V2__alter_ai_messages_message_type.sql
ALTER TABLE `ai_messages`
    MODIFY COLUMN `message_type`
    VARCHAR(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL;