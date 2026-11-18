/**
 * 前端单元测试：状态/难度映射与格式化工具。
 *
 * 运行方式（无需任何新增依赖，Node 22 内置 `--experimental-strip-types` 直接剥类型）：
 *   npm test
 * 等价于 `node --experimental-strip-types tests/index.ts`。
 *
 * 为什么不放进 `src/`：tsconfig 的 include 是 `src/**`，而项目尚未装 `@types/node`
 * （本地 npm registry 不可达，见 docs/PROGRESS.md §12.7），
 * 把 `node:test` 的导入放进 src 会让 `vue-tsc --noEmit` 报找不到模块。
 * 放在 tests/ 下既不影响类型检查，又能被 Node 直接执行。
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  STATUS_MAP,
  statusLabel,
  statusType,
  statusColor,
  statusTint,
  formatTimeUsed,
  formatMemory,
  DIFFICULTY_MAP,
  CODE_TEMPLATES,
  LANGUAGE_OPTIONS,
  MONACO_LANGUAGE,
  cssVar,
} from '../src/utils/status.ts'

test('STATUS_MAP 覆盖 0-8 九种判题状态（论文表 4-8）', () => {
  for (let code = 0; code <= 8; code++) {
    assert.ok(STATUS_MAP[code], `缺少状态码 ${code}`)
    assert.equal(typeof STATUS_MAP[code].label, 'string')
    assert.ok(STATUS_MAP[code].label.length > 0)
  }
})

test('statusLabel：已知码返回中文，未知码回退「未知」，空值回退「-」', () => {
  assert.equal(statusLabel(0), '排队中')
  assert.equal(statusLabel(2), '通过')
  assert.equal(statusLabel(7), '编译错误')
  assert.equal(statusLabel(99), '未知')
  assert.equal(statusLabel(undefined), '-')
  assert.equal(statusLabel(null as unknown as undefined), '-')
})

test('statusType：已知码返回 Element Plus 语义类型，未知/空值回退 info', () => {
  assert.equal(statusType(2), 'success')
  assert.equal(statusType(3), 'danger')
  assert.equal(statusType(4), 'warning')
  assert.equal(statusType(1), 'primary')
  assert.equal(statusType(0), 'info')
  assert.equal(statusType(99), 'info')
  assert.equal(statusType(undefined), 'info')
})

test('statusColor：同族状态同色，且全部走 CSS 变量（不再硬编码 hex）', () => {
  assert.equal(statusColor(2), 'var(--oj-success)')
  assert.equal(statusColor(3), 'var(--oj-danger)')
  assert.equal(statusColor(6), 'var(--oj-danger)')
  assert.equal(statusColor(8), 'var(--oj-danger)')
  assert.equal(statusColor(4), 'var(--oj-warning)')
  assert.equal(statusColor(7), 'var(--oj-warning)')
  assert.equal(statusColor(1), 'var(--oj-primary)')
  assert.equal(statusColor(0), 'var(--oj-gray-500)')
  // 未知码与空值都必须有兜底，不能返回 undefined（否则会被当成透明色）
  assert.equal(statusColor(99), 'var(--oj-gray-500)')
  assert.equal(statusColor(undefined), 'var(--oj-gray-500)')

  for (let code = 0; code <= 8; code++) {
    assert.match(statusColor(code), /^var\(--oj-/, `状态 ${code} 仍在用裸色值`)
  }
})

test('statusTint：浅底色同样走 CSS 变量且有兜底', () => {
  assert.equal(statusTint(2), 'var(--oj-tint-success)')
  assert.equal(statusTint(3), 'var(--oj-tint-danger)')
  assert.equal(statusTint(4), 'var(--oj-tint-warning)')
  assert.equal(statusTint(99), 'var(--oj-tint-gray)')
  assert.equal(statusTint(undefined), 'var(--oj-tint-gray)')
})

test('formatTimeUsed：<1s 显示毫秒，≥1s 转秒保留两位，空值占位符', () => {
  assert.equal(formatTimeUsed(0), '0 ms')
  assert.equal(formatTimeUsed(1), '1 ms')
  assert.equal(formatTimeUsed(999), '999 ms')
  assert.equal(formatTimeUsed(1000), '1.00 s')
  assert.equal(formatTimeUsed(1500), '1.50 s')
  assert.equal(formatTimeUsed(123456), '123.46 s')
  assert.equal(formatTimeUsed(null), '—')
  assert.equal(formatTimeUsed(undefined), '—')
})

test('formatMemory：<1024KB 显示 KB，≥1MB 转兆保留一位，空值占位符', () => {
  assert.equal(formatMemory(0), '0 KB')
  assert.equal(formatMemory(1023), '1023 KB')
  assert.equal(formatMemory(1024), '1.0 MB')
  assert.equal(formatMemory(3840), '3.8 MB')
  assert.equal(formatMemory(36864), '36.0 MB')
  assert.equal(formatMemory(null), '—')
  assert.equal(formatMemory(undefined), '—')
})

test('DIFFICULTY_MAP：三档难度齐全，color 可直接给 el-tag / el-progress', () => {
  assert.deepEqual(
    [1, 2, 3].map((d) => DIFFICULTY_MAP[d].label),
    ['简单', '中等', '困难'],
  )
  for (const d of [1, 2, 3]) {
    assert.match(DIFFICULTY_MAP[d].color, /^var\(--oj-/)
    assert.match(DIFFICULTY_MAP[d].token, /^--oj-/)
  }
  // 未定义的难度不能崩，调用方用 `?.label ?? '未知'` 兜
  assert.equal(DIFFICULTY_MAP[9], undefined)
})

test('CODE_TEMPLATES：五种语言都有可直接提交的模板', () => {
  const languages = LANGUAGE_OPTIONS.map((l) => l.value)
  assert.deepEqual(languages, ['c', 'cpp', 'java', 'python3', 'js'])
  for (const lang of languages) {
    assert.ok(CODE_TEMPLATES[lang], `${lang} 缺少模板`)
    assert.ok(CODE_TEMPLATES[lang].trim().length > 0, `${lang} 模板为空`)
  }
  // C/C++/Java 模板必须自带入口函数，否则新手直接提交就是 CE
  assert.match(CODE_TEMPLATES.c, /int main/)
  assert.match(CODE_TEMPLATES.cpp, /int main/)
  assert.match(CODE_TEMPLATES.java, /class Main/)
})

test('MONACO_LANGUAGE：五种语言全部映射到 Monaco 语法模式', () => {
  assert.equal(MONACO_LANGUAGE.c, 'c')
  assert.equal(MONACO_LANGUAGE.cpp, 'cpp')
  assert.equal(MONACO_LANGUAGE.java, 'java')
  assert.equal(MONACO_LANGUAGE.python3, 'python')
  assert.equal(MONACO_LANGUAGE.js, 'javascript')
  for (const lang of LANGUAGE_OPTIONS.map((l) => l.value)) {
    assert.ok(MONACO_LANGUAGE[lang], `${lang} 缺少 Monaco 语言映射`)
  }
})

test('cssVar：无 DOM 环境时返回兜底值，不抛异常', () => {
  // 测试跑在 Node 里，没有 document；实现必须自己判掉，否则整个模块无法在服务端/测试中导入
  assert.equal(cssVar('--oj-primary', '#4f46e5'), '#4f46e5')
  assert.equal(typeof cssVar('--oj-primary'), 'string')
})
