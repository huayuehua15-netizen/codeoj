-- JS 场景题种子（函数实现题，蓝桥杯风格）：学生实现入口函数，平台传参比对返回值。
-- 依赖：20261122_problem_entry_function.sql（entry_function 列）。
-- Safe to repeat：按标题判重。仅适用于已执行列迁移的库。
SET NAMES utf8mb4;

-- 01 简单：数组去重（函数实现）
INSERT INTO `problem`
  (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`,
   `languages`, `entry_function`, `submit_count`, `accepted_count`, `enabled`)
SELECT
  '数组去重（函数实现）',
  '## 题目描述\n\n实现一个函数 `dedupe(arr)`，去掉数组中的重复项，**保留第一次出现的顺序**，返回去重后的新数组（不修改原数组）。\n\n这是 JavaScript 场景题：你只需要实现函数本身，判题平台会自动传入参数并比对返回值；编辑器右侧的「运行」可以用自定义参数自测。\n\n## 函数签名\n\n```js\n/**\n * @param {number[]} arr 元素为整数的数组\n * @returns {number[]} 按首次出现顺序去重后的数组\n */\nfunction dedupe(arr) {\n  // 在此实现\n}\n```\n\n## 样例\n\n| 调用 | 返回 |\n|---|---|\n| `dedupe([1,1,2,2,3])` | `[1,2,3]` |\n| `dedupe([])` | `[]` |\n\n## 数据范围\n\n0 ≤ arr.length ≤ 10^4，元素为 -10^5 ~ 10^5 的整数。',
  '[{"input": "[1,1,2,2,3]", "output": "[1,2,3]"}, {"input": "[]", "output": "[]"}]',
  1, 1000, 64,
  '["js"]', 'dedupe', 0, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = '数组去重（函数实现）');

INSERT INTO `test_case` (`problem_id`, `input`, `expected_output`, `score`, `is_sample`)
SELECT p.`id`, c.`input`, c.`expected_output`, 1, c.`is_sample`
FROM `problem` p
JOIN (
  SELECT '[1,1,2,2,3]' AS `input`, '[1,2,3]' AS `expected_output`, 1 AS `is_sample`
  UNION ALL SELECT '[]', '[]', 1
  UNION ALL SELECT '[5,5,5,5]', '[5]', 0
  UNION ALL SELECT '[3,1,3,2,1,4]', '[3,1,2,4]', 0
  UNION ALL SELECT '[-1,-1,0,1,0]', '[-1,0,1]', 0
) c ON p.`title` = '数组去重（函数实现）'
WHERE NOT EXISTS (
  SELECT 1 FROM `test_case` t
  WHERE t.`problem_id` = p.`id` AND t.`input` = c.`input`
);

-- 02 中等：命名风格转换（函数实现）
INSERT INTO `problem`
  (`title`, `description`, `samples`, `difficulty`, `time_limit_ms`, `memory_limit_mb`,
   `languages`, `entry_function`, `submit_count`, `accepted_count`, `enabled`)
SELECT
  '命名风格转换（函数实现）',
  '## 题目描述\n\n前端工程里变量命名风格五花八门。实现函数 `toCamelCase(name)`，把下划线（snake_case）或连字符（kebab-case）风格的命名统一转换为小驼峰（camelCase）。\n\n这是 JavaScript 场景题：实现函数本身即可，判题平台会传入参数并比对返回值。\n\n## 函数签名\n\n```js\n/**\n * @param {string} name 由小写字母和 - / _ 分隔符组成的标识符\n * @returns {string} 小驼峰形式：首个单词小写，其后每个单词首字母大写\n */\nfunction toCamelCase(name) {\n  // 在此实现\n}\n```\n\n## 转换规则\n\n1. 单词以连续的 `-` 或 `_` 分隔（两种分隔符可混用）；\n2. 首个单词保持全小写；\n3. 其后每个单词**首字母大写**，其余小写；\n4. 输入保证由小写字母与分隔符组成（不含数字、大写与空格）。\n\n## 样例\n\n| 调用 | 返回 |\n|---|---|\n| `toCamelCase("hello_world")` | `"helloWorld"` |\n| `toCamelCase("user-name-field")` | `"userNameField"` |\n\n## 数据范围\n\n1 ≤ name.length ≤ 10^4。注意用例规模较大，避免 O(n²) 的反复字符串拼接（可以，但要写对）。',
  '[{"input": "\"hello_world\"", "output": "helloWorld"}, {"input": "\"user-name-field\"", "output": "userNameField"}]',
  2, 1000, 64,
  '["js"]', 'toCamelCase', 0, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM `problem` WHERE `title` = '命名风格转换（函数实现）');

INSERT INTO `test_case` (`problem_id`, `input`, `expected_output`, `score`, `is_sample`)
SELECT p.`id`, c.`input`, c.`expected_output`, 1, c.`is_sample`
FROM `problem` p
JOIN (
  SELECT '"hello_world"' AS `input`, 'helloWorld' AS `expected_output`, 1 AS `is_sample`
  UNION ALL SELECT '"user-name-field"', 'userNameField', 1
  UNION ALL SELECT '"a"', 'a', 0
  UNION ALL SELECT '"already_camel"', 'alreadyCamel', 0
  UNION ALL SELECT '"mix-ed_se-parators"', 'mixEdSeParators', 0
  UNION ALL SELECT '"a_b_c_d_e"', 'aBCDE', 0
) c ON p.`title` = '命名风格转换（函数实现）'
WHERE NOT EXISTS (
  SELECT 1 FROM `test_case` t
  WHERE t.`problem_id` = p.`id` AND t.`input` = c.`input`
);
