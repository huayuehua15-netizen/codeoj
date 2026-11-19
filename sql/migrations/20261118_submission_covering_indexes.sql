-- =====================================================================
-- 增量迁移：submission 表补两个覆盖索引（review P2-20 / §E 索引核对）
--   idx_submission_status_user  (status, user_id, problem_id)
--     → 排行榜对账 acCountByUser：WHERE status=2 GROUP BY user_id 聚合走索引内扫描
--   idx_submission_problem_user (problem_id, user_id)
--     → 课程完成统计 completionStats：COUNT(DISTINCT user_id) 免逐行回表
-- 适用：已按旧版 schema.sql 建库的环境；schema.sql 已同步包含。
-- 用法：mysql -uroot -p123456 codeoj < sql/migrations/20261118_submission_covering_indexes.sql
-- 可重复执行：索引已存在时跳过，不报 1061。
-- =====================================================================
SET NAMES utf8mb4;

SET @add_status_user = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema=DATABASE() AND table_name='submission'
     AND index_name='idx_submission_status_user')=0,
  'ALTER TABLE `submission` ADD KEY `idx_submission_status_user` (`status`, `user_id`, `problem_id`)',
  'SELECT 1');
PREPARE stmt FROM @add_status_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_problem_user = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema=DATABASE() AND table_name='submission'
     AND index_name='idx_submission_problem_user')=0,
  'ALTER TABLE `submission` ADD KEY `idx_submission_problem_user` (`problem_id`, `user_id`)',
  'SELECT 1');
PREPARE stmt FROM @add_problem_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 校验：SHOW INDEX FROM `submission` WHERE Key_name LIKE 'idx_submission_status_user'
--                                     OR Key_name LIKE 'idx_submission_problem_user';
