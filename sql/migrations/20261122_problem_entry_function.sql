-- JS 场景题（函数实现题）：problem 表新增 entry_function 列。
-- 约定：NULL = 标准 stdin/stdout 题（现有题目全部不受影响）；
--       非空 = JS 场景题，值为入口函数名，oj-server 在入库判题代码前包装"用户代码 + 调用器"，
--       judge 链路拿到的仍是可直接执行的 stdin/stdout 代码（对判题端透明）。
-- Safe to repeat（重复执行输出 SELECT 1，不加列）。Back up first per repo convention.
SET NAMES utf8mb4;

SET @upgrade_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='problem' AND column_name='entry_function')=0,
  'ALTER TABLE `problem` ADD COLUMN `entry_function` VARCHAR(64) NULL COMMENT ''JS 场景题入口函数名；NULL=标准 stdin/stdout 题'' AFTER `languages`',
  'SELECT 1');
PREPARE upgrade_stmt FROM @upgrade_sql;
EXECUTE upgrade_stmt;
DEALLOCATE PREPARE upgrade_stmt;
