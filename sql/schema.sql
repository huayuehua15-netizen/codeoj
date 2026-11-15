-- =====================================================================
-- 在线代码评测与编程练习平台 —— 数据库初始化脚本（论文 4.4 数据库设计）
-- MySQL 8.0 / utf8mb4 / InnoDB
-- 设计原则（论文 4.4）：
--   1. 代码、测试用例等大文本使用 LONGTEXT 单表存储，避免行迁移；
--   2. 高频统计字段（提交数、通过数）在题目表中冗余，避免实时聚合查询；
--   3. 所有时间字段统一 UTC 存储、展示层转换。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS codeoj
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE codeoj;

-- ---------------------------------------------------------------------
-- 表 4-1  user 用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username`   VARCHAR(50)  NOT NULL COMMENT '用户名（支持中文，全局唯一）',
  `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `email`      VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=用户，1=管理员',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ---------------------------------------------------------------------
-- 表 4-2  problem 题目表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `problem` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
  `title`           VARCHAR(200) NOT NULL COMMENT '题目标题',
  `description`     LONGTEXT     COMMENT '题目描述（Markdown）',
  `samples`         JSON         COMMENT '样例输入输出数组',
  `difficulty`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=简单，2=中等，3=困难',
  `time_limit_ms`   INT          NOT NULL DEFAULT 1000 COMMENT '时间限制（毫秒）',
  `memory_limit_mb` INT          NOT NULL DEFAULT 256 COMMENT '内存限制（MB）',
  `languages`       JSON         COMMENT '允许提交的语言数组',
  `submit_count`    INT          NOT NULL DEFAULT 0 COMMENT '提交总数（冗余统计）',
  `accepted_count`  INT          NOT NULL DEFAULT 0 COMMENT '通过总数（冗余统计）',
  `enabled`         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否上架',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_problem_difficulty` (`difficulty`),
  KEY `idx_problem_enabled` (`enabled`)
) ENGINE = InnoDB COMMENT = '题目表';

-- ---------------------------------------------------------------------
-- 表 4-3  test_case 测试用例表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `test_case` (
  `id`              BIGINT    NOT NULL AUTO_INCREMENT COMMENT '用例 ID',
  `problem_id`      BIGINT    NOT NULL COMMENT '所属题目',
  `input`           LONGTEXT  COMMENT '标准输入',
  `expected_output` LONGTEXT  COMMENT '期望输出',
  `score`           INT       NOT NULL DEFAULT 0 COMMENT '分值（计分制）',
  `is_sample`       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否样例（样例可在题面展示）',
  PRIMARY KEY (`id`),
  KEY `idx_test_case_problem` (`problem_id`)
) ENGINE = InnoDB COMMENT = '测试用例表';

-- ---------------------------------------------------------------------
-- 表 4-4  submission 提交记录表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `submission` (
  `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '提交 ID',
  `user_id`        BIGINT      NOT NULL COMMENT '提交用户',
  `problem_id`     BIGINT      NOT NULL COMMENT '题目',
  `language`       VARCHAR(20) NOT NULL COMMENT 'c/cpp/java/python3',
  `code`           LONGTEXT    NOT NULL COMMENT '提交代码（≤64KB）',
  `status`         TINYINT     NOT NULL DEFAULT 0 COMMENT '0排队 1判题中 2AC 3WA 4TLE 5MLE 6RE 7CE 8SE',
  `time_used_ms`   INT         DEFAULT NULL COMMENT '最大用例耗时（毫秒）',
  `memory_used_kb` INT         DEFAULT NULL COMMENT '最大用例内存（KB）',
  `error_info`     TEXT        COMMENT '编译/运行错误摘要',
  `judged_at`      DATETIME    DEFAULT NULL COMMENT '判题完成时间',
  `created_at`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_submission_user` (`user_id`),
  KEY `idx_submission_problem` (`problem_id`),
  KEY `idx_submission_status` (`status`),
  KEY `idx_submission_judged_at` (`judged_at`)
) ENGINE = InnoDB COMMENT = '提交记录表';

-- ---------------------------------------------------------------------
-- 表 4-5  judge_case 判题明细表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `judge_case` (
  `id`             BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `submission_id`  BIGINT  NOT NULL COMMENT '提交 ID',
  `case_index`     INT     NOT NULL COMMENT '用例序号',
  `status`         TINYINT DEFAULT NULL COMMENT '该用例判题状态',
  `time_used_ms`   INT     DEFAULT NULL COMMENT '该用例耗时（毫秒）',
  `memory_used_kb` INT     DEFAULT NULL COMMENT '该用例内存（KB）',
  PRIMARY KEY (`id`),
  KEY `idx_judge_case_submission` (`submission_id`)
) ENGINE = InnoDB COMMENT = '判题明细表';

-- ---------------------------------------------------------------------
-- 课程实验（论文 3.3 / 4.4：course + course_problem 多对多）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `course` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课程 ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '课程名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '课程描述',
  `teacher_id`  BIGINT       DEFAULT NULL COMMENT '创建教师',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '课程表';

CREATE TABLE IF NOT EXISTS `course_problem` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`  BIGINT NOT NULL COMMENT '课程 ID',
  `problem_id` BIGINT NOT NULL COMMENT '题目 ID',
  `sort_order` INT    NOT NULL DEFAULT 0 COMMENT '题目在课程内的顺序',
  PRIMARY KEY (`id`),
  KEY `idx_course_problem_course` (`course_id`)
) ENGINE = InnoDB COMMENT = '课程题目关联表';

-- ---------------------------------------------------------------------
-- 种子数据：一道示例题目（A+B Problem）与两组样例用例
-- 管理员账号：通过 /api/auth/register 注册后执行
--   UPDATE user SET role = 1 WHERE username = '你的用户名';
-- ---------------------------------------------------------------------
INSERT INTO `problem`
  (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`,
   `languages`, `submit_count`, `accepted_count`, `enabled`)
VALUES
  ('A+B Problem',
   '## 题目描述\n\n输入两个整数 a 和 b，输出它们的和。\n\n## 输入格式\n\n一行两个整数，以空格分隔。\n\n## 输出格式\n\n一行一个整数，即 a+b 的值。\n\n## 数据范围\n\n0 ≤ a, b ≤ 10^9',
   '[{"input": "1 2", "output": "3"}, {"input": "10 20", "output": "30"}]',
   1, 1000, 256,
   '["c", "cpp", "java", "python3"]',
   0, 0, 1);

INSERT INTO `test_case` (`problem_id`, `input`, `expected_output`, `score`, `is_sample`)
SELECT `id`, '1 2', '3', 1, 1 FROM `problem` WHERE `title` = 'A+B Problem'
UNION ALL
SELECT `id`, '10 20', '30', 1, 1 FROM `problem` WHERE `title` = 'A+B Problem';
