-- =====================================================================
-- 增量迁移：user 表补个人资料列（API.md v1.8：头像 / 签名）
--   avatar    VARCHAR(255)  头像相对路径 /uploads/avatars/xxx
--   signature VARCHAR(200)  个性签名
-- 适用：已按旧版 schema.sql 建库的环境；schema.sql 已同步包含。
-- 用法：mysql -uroot -p123456 codeoj < sql/migrations/20261119_user_profile.sql
-- 可重复执行：列已存在时跳过，不报 1060。
-- =====================================================================
SET NAMES utf8mb4;

SET @add_avatar = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='user' AND column_name='avatar')=0,
  'ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT ''头像相对路径 /uploads/avatars/xxx'' AFTER `status`',
  'SELECT 1');
PREPARE stmt FROM @add_avatar;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_signature = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='user' AND column_name='signature')=0,
  'ALTER TABLE `user` ADD COLUMN `signature` VARCHAR(200) DEFAULT NULL COMMENT ''个性签名'' AFTER `avatar`',
  'SELECT 1');
PREPARE stmt FROM @add_signature;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 校验：SHOW COLUMNS FROM `user` LIKE 'avatar';
