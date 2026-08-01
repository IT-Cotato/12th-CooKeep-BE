DROP PROCEDURE IF EXISTS seed_benchmark_users;

DELIMITER $$
CREATE PROCEDURE seed_benchmark_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE benchmark_user_id BIGINT;

    WHILE i <= 100 DO
        INSERT INTO users (
            cookie_cnt,
            is_cookeeps_onboarded,
            is_first_ingredient_reward,
            is_first_recipe_reward,
            is_profile_auto_update,
            marketing_consent,
            marketing_push,
            password_cnt,
            created_at,
            last_access_at,
            updated_at,
            nickname,
            email,
            password,
            user_status
        ) VALUES (
            0,
            b'0',
            b'0',
            b'0',
            b'0',
            b'0',
            b'0',
            0,
            NOW(6),
            NOW(6),
            NOW(6),
            CONCAT('bench', LPAD(i, 4, '0')),
            CONCAT('benchmark', i, '@example.com'),
            '$2y$10$KD5LVtmXVUC.PzTehgGX9ODuIt6YndhwcE1VVOfPJuKK8DAmMbj0S',
            'ACTIVE'
        )
        ON DUPLICATE KEY UPDATE
            password = VALUES(password),
            user_status = 'ACTIVE',
            last_access_at = NOW(6);

        SELECT user_id
        INTO benchmark_user_id
        FROM users
        WHERE email = CONCAT('benchmark', i, '@example.com');

        INSERT INTO user_auths (
            created_at,
            updated_at,
            user_id,
            provider
        )
        SELECT NOW(6), NOW(6), benchmark_user_id, 'LOCAL'
        WHERE NOT EXISTS (
            SELECT 1
            FROM user_auths
            WHERE user_id = benchmark_user_id
              AND provider = 'LOCAL'
        );

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_benchmark_users();
DROP PROCEDURE seed_benchmark_users;
