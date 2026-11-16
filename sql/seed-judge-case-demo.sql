-- =====================================================================
-- judge_case 演示数据补造（2026-11-17，成员 B）
-- =====================================================================
-- 背景：演示提交 1053 条由脚本直插（§12.4），未生成 judge_case 明细，
--       导致判题详情页「用例级结果」卡片（论文 5.7 验收点）无数据可展示。
-- 规则（贴近真实 OJ 语义）：
--   - AC(2)  ：该题全部用例逐条通过；
--   - WA(3)/TLE(4)：短路——按用例顺序执行到首个失败用例为止，其后不再判；
--   - 每题用例数取 test_case 实际行数（2~5），与题库自洽；
--   - 失败用例序号 = 1 + (submission_id % 用例数)，确定性可复现
--     （不用 RAND()：MySQL 对含 RAND() 的派生表可能不物化，逐行重算会
--      导致 CASE 各列条件求值不一致——首版踩过的坑）；
--   - 失败行耗时 600~1500ms，通过行 8~80ms，内存 1024~4095KB，均由 id 派生。
-- 可重复执行：脚本自带 DELETE，幂等。
-- =====================================================================

DELETE FROM judge_case;

INSERT INTO judge_case (submission_id, case_index, status, time_used_ms, memory_used_kb)
SELECT
  s.id,
  t.rn,
  CASE WHEN s.status = 2 OR t.rn < f.fail_at THEN 2 ELSE s.status END,
  CASE WHEN s.status = 2 OR t.rn < f.fail_at
       THEN 8 + ((s.id * 7 + t.rn * 13) % 73)
       ELSE 600 + ((s.id * 3) % 901) END,
  1024 + ((s.id * 11 + t.rn * 17) % 3072)
FROM submission s
JOIN (SELECT problem_id, COUNT(*) AS total FROM test_case GROUP BY problem_id) tc
     ON tc.problem_id = s.problem_id
-- fail_at：AC 视为「全部通过」（total+1 永不触发失败分支）；WA/TLE 取 1 + (id % 用例数)，必 ≤ 用例数
JOIN (SELECT id,
             CASE WHEN status = 2 THEN 999 ELSE 1 + (id % tc2.total) END AS fail_at
      FROM submission s2
      JOIN (SELECT problem_id, COUNT(*) AS total FROM test_case GROUP BY problem_id) tc2
           ON tc2.problem_id = s2.problem_id
      WHERE status IN (2, 3, 4)) f
     ON f.id = s.id
JOIN (SELECT problem_id, ROW_NUMBER() OVER (PARTITION BY problem_id ORDER BY id) AS rn
      FROM test_case) t
     ON t.problem_id = s.problem_id
WHERE t.rn <= f.fail_at;

-- 核对：总行数 / 按状态分布 / 带明细的提交数
SELECT COUNT(*) AS total_case_rows FROM judge_case;
SELECT status, COUNT(*) AS rows_by_status FROM judge_case GROUP BY status;
SELECT COUNT(DISTINCT submission_id) AS submissions_with_cases FROM judge_case;

-- 核对一致性：WA/TLE 提交必须恰有一条与其状态相同的失败用例行
SELECT COUNT(*) AS subs_missing_fail_row
FROM submission s
LEFT JOIN judge_case jc ON jc.submission_id = s.id AND jc.status = s.status
WHERE s.status IN (3, 4) AND jc.id IS NULL;

-- 核对失败行耗时区间（应全部 ≥ 600）
SELECT COUNT(*) AS fail_rows_below_600 FROM judge_case WHERE status IN (3, 4) AND time_used_ms < 600;
