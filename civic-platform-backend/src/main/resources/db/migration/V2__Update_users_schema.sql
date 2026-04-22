-- Update users table schema to match requirements
-- Keep migration idempotent and safe across old/new schemas.

-- Add missing columns to users table if they don't exist
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS user_name VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS association_name VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS contact_name VARCHAR(255);
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS points INT DEFAULT 0;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS awarded_date DATE;

-- Normalize known enum-like columns only if they exist.
ALTER TABLE `user` MODIFY COLUMN IF EXISTS badge VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE `user` MODIFY COLUMN IF EXISTS user_type VARCHAR(20) DEFAULT 'CITIZEN';
ALTER TABLE `user` MODIFY COLUMN IF EXISTS role VARCHAR(20) DEFAULT 'USER';

-- Add indexes conditionally (portable approach).
SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND index_name = 'idx_user_user_type'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_user_user_type ON `user`(user_type)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND index_name = 'idx_user_role'
);
SET @col_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND column_name = 'role'
);
SET @sql := IF(@idx_exists = 0 AND @col_exists = 1, 'CREATE INDEX idx_user_role ON `user`(role)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND index_name = 'idx_user_badge'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_user_badge ON `user`(badge)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
