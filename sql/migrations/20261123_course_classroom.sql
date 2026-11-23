-- 课程班级升级为"班级 + 作业"：邀请码、截止时间、班级成员。
-- B 侧（oj-server / oj-web）使用；不改动判题链路。Safe to repeat.
SET NAMES utf8mb4;

-- 1. course.join_code：学生凭码加入班级。NULL 表示暂未开放加入。
SET @add_join_code = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'course' AND column_name = 'join_code') = 0,
  'ALTER TABLE `course` ADD COLUMN `join_code` VARCHAR(12) NULL COMMENT ''学生加入班级的邀请码（NULL=未开放）'' AFTER `teacher_id`',
  'SELECT 1');
PREPARE stmt FROM @add_join_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. course.deadline：作业截止时间；NULL 表示不限时，进度仍按 AC 统计。
SET @add_deadline = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'course' AND column_name = 'deadline') = 0,
  'ALTER TABLE `course` ADD COLUMN `deadline` DATETIME NULL COMMENT ''课程作业截止时间（NULL=不限）'' AFTER `join_code`',
  'SELECT 1');
PREPARE stmt FROM @add_deadline;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 历史课程补一个不可预测的 8 位邀请码（去掉 I/O/0/1），避免升级后
--    出现 OJ000001 这类可按 id 猜到的默认码。教师可在管理台随时轮换。
UPDATE `course`
   SET `join_code` = CONCAT(
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1),
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1),
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1),
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1),
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1),
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1),
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1),
       SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + FLOOR(RAND() * 32), 1)
   )
 WHERE `join_code` IS NULL;

-- 4. 邀请码唯一索引（MySQL 唯一索引允许多个 NULL）。
SET @add_join_code_index = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'course' AND index_name = 'uk_course_join_code') = 0,
  'ALTER TABLE `course` ADD UNIQUE KEY `uk_course_join_code` (`join_code`)',
  'SELECT 1');
PREPARE stmt FROM @add_join_code_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 班级成员表。
CREATE TABLE IF NOT EXISTS `course_member` (
  `id`        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT   NOT NULL COMMENT '课程 ID',
  `user_id`   BIGINT   NOT NULL COMMENT '学生 ID',
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_member` (`course_id`, `user_id`),
  KEY `idx_course_member_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '课程班级成员表';
