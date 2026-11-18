-- Upgrade the early seven-table schema without deleting existing records.
-- Back up the selected database first; run before starting current server/judge.
-- Safe to repeat, and safe on a fresh current schema. Select the target database in mysql CLI.
SET NAMES utf8mb4;

SET @upgrade_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='user' AND column_name='status')=0,
  'ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT ''0=正常，1=封禁'' AFTER `role`',
  'SELECT 1');
PREPARE upgrade_stmt FROM @upgrade_sql;
EXECUTE upgrade_stmt;
DEALLOCATE PREPARE upgrade_stmt;

SET @upgrade_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='problem' AND column_name='tags')=0,
  'ALTER TABLE `problem` ADD COLUMN `tags` JSON NULL COMMENT ''标签数组'' AFTER `description`',
  'SELECT 1');
PREPARE upgrade_stmt FROM @upgrade_sql;
EXECUTE upgrade_stmt;
DEALLOCATE PREPARE upgrade_stmt;

-- The current entity no longer writes nickname; retain legacy values and allow new registrations.
SET @upgrade_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='user' AND column_name='nickname' AND is_nullable='NO')>0,
  'ALTER TABLE `user` ALTER COLUMN `nickname` SET DEFAULT ''''',
  'SELECT 1');
PREPARE upgrade_stmt FROM @upgrade_sql;
EXECUTE upgrade_stmt;
DEALLOCATE PREPARE upgrade_stmt;

CREATE TABLE IF NOT EXISTS `contest` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_contest_time` (`start_time`, `end_time`)
) ENGINE=InnoDB COMMENT='竞赛表';

CREATE TABLE IF NOT EXISTS `contest_problem` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contest_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_contest_problem` (`contest_id`)
) ENGINE=InnoDB COMMENT='竞赛题目关联表';

CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `content` VARCHAR(2000) NOT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='公告表';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_problem` (`user_id`, `problem_id`),
  KEY `idx_favorite_user` (`user_id`)
) ENGINE=InnoDB COMMENT='题目收藏表';
