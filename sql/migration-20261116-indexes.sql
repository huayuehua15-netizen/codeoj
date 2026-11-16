-- =====================================================================
-- 增量迁移：提交记录表复合索引补齐
-- 适用：已经执行过旧版 sql/schema.sql（表已存在，CREATE TABLE IF NOT EXISTS 不会重建）
-- 用法：mysql -uroot -p123456 codeoj < sql/migration-20261116-indexes.sql
-- 说明：schema.sql 已同步包含这些索引；本脚本仅供已建库的环境平滑升级，
--       索引已存在时会报 1061 Duplicate key name，可忽略或用下方注释核对后跳过。
-- =====================================================================
SET NAMES utf8mb4;
USE codeoj;

-- 提交记录页：WHERE user_id=? [AND problem_id=?] [AND status=?] ORDER BY id DESC
ALTER TABLE `submission` ADD KEY `idx_submission_user_id` (`user_id`, `id`);

-- 竞赛排名：题目集 IN (...) + 时间窗 BETWEEN
ALTER TABLE `submission` ADD KEY `idx_submission_problem_created` (`problem_id`, `created_at`);

-- 个人统计/趋势聚合：user_id + created_at 范围
ALTER TABLE `submission` ADD KEY `idx_submission_user_created` (`user_id`, `created_at`);

-- 校验：确认三个索引已建立
-- SHOW INDEX FROM `submission`;
