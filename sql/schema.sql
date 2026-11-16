-- =====================================================================
-- 在线代码评测与编程练习平台 —— 数据库初始化脚本（论文 4.4 数据库设计）
-- MySQL 8.0 / utf8mb4 / InnoDB
-- 设计原则（论文 4.4）：
--   1. 代码、测试用例等大文本使用 LONGTEXT 单表存储，避免行迁移；
--   2. 高频统计字段（提交数、通过数）在题目表中冗余，避免实时聚合查询；
--   3. 业务 DATETIME 按 Asia/Shanghai；内部租约和补发时间使用 UTC 数据库时钟。
-- =====================================================================
-- 强制本会话连接字符集为 utf8mb4：docker-entrypoint 导入时客户端默认
-- 字符集可能为 latin1，缺此句会导致中文被双重编码（mojibake）
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS codeoj
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE codeoj;

-- 显式声明连接字符集：容器初始化时 mysql 客户端默认字符集可能是 latin1，
-- 不加会把 UTF-8 种子数据按 latin1 读入（双重编码乱码）
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 表 4-1  user 用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username`   VARCHAR(50)  NOT NULL COMMENT '用户名（支持中文，全局唯一）',
  `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `email`      VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=用户，1=管理员',
  `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=正常，1=封禁',
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
  `tags`            JSON         COMMENT '标签数组（动态规划/贪心/模拟…）',
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
  `judged_at`      DATETIME    DEFAULT NULL COMMENT '判题完成时间 Asia/Shanghai',
  `judge_token`    VARCHAR(64) DEFAULT NULL COMMENT '当前评测租约所有者',
  `judge_lease_until` DATETIME(6) DEFAULT NULL COMMENT '租约到期时间 UTC',
  `judge_attempts` INT NOT NULL DEFAULT 0 COMMENT '已领取评测次数',
  `next_dispatch_at` DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)) COMMENT '持久待发送记录下次重试时间 UTC',
  `created_at`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间 Asia/Shanghai',
  PRIMARY KEY (`id`),
  KEY `idx_submission_user` (`user_id`),
  KEY `idx_submission_problem` (`problem_id`),
  KEY `idx_submission_status` (`status`),
  KEY `idx_submission_dispatch` (`status`, `next_dispatch_at`),
  KEY `idx_submission_lease` (`status`, `judge_lease_until`),
  KEY `idx_submission_judged_at` (`judged_at`),
  -- 提交记录页：WHERE user_id=? [AND problem_id=?] [AND status=?] ORDER BY id DESC
  -- 复合索引让排序也走索引，避免"过滤+filesort"；单列 user_id 索引只能覆盖前半段
  KEY `idx_submission_user_id` (`user_id`, `id`),
  -- 竞赛排名：题目集 IN (...) + 时间窗 BETWEEN，按时间升序扫描
  KEY `idx_submission_problem_created` (`problem_id`, `created_at`),
  -- 个人统计/趋势：user_id + created_at 范围聚合
  KEY `idx_submission_user_created` (`user_id`, `created_at`)
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
-- 竞赛模式（论文 7.2 展望落地）：比赛 + 比赛题目集
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `contest` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '比赛 ID',
  `title`       VARCHAR(200) NOT NULL COMMENT '比赛名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '比赛说明',
  `start_time`  DATETIME     NOT NULL COMMENT '开始时间',
  `end_time`    DATETIME     NOT NULL COMMENT '结束时间',
  `created_by`  BIGINT       DEFAULT NULL COMMENT '创建教师',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_contest_time` (`start_time`, `end_time`)
) ENGINE = InnoDB COMMENT = '竞赛表';

CREATE TABLE IF NOT EXISTS `contest_problem` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contest_id` BIGINT NOT NULL COMMENT '比赛 ID',
  `problem_id` BIGINT NOT NULL COMMENT '题目 ID',
  `sort_order` INT    NOT NULL DEFAULT 0 COMMENT '题目顺序（字母题号 A/B/C...）',
  PRIMARY KEY (`id`),
  KEY `idx_contest_problem` (`contest_id`)
) ENGINE = InnoDB COMMENT = '竞赛题目关联表';

CREATE TABLE IF NOT EXISTS `announcement` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告 ID',
  `title`      VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content`    VARCHAR(2000) NOT NULL COMMENT '公告正文',
  `created_by` BIGINT       DEFAULT NULL COMMENT '发布教师',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '公告表';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '用户',
  `problem_id` BIGINT   NOT NULL COMMENT '题目',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_problem` (`user_id`, `problem_id`),
  KEY `idx_favorite_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '题目收藏表';

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
