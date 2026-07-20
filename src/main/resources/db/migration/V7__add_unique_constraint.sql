-- src/main/resources/db/migration/V7__add_unique_constraint.sql

ALTER TABLE user_auths
    ADD CONSTRAINT uq_user_provider UNIQUE (user_id, provider);