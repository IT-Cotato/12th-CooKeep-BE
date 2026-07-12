-- src/main/resources/db/migration/V9__add_profile_image_id_to_users.sql

ALTER TABLE users
    ADD COLUMN profile_image_id INT NULL DEFAULT 1;