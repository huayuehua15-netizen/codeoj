/**
 * 前端单元测试：服务端时间解析与格式化。
 *
 * 重点验证 docs/API.md §0.1 的三种格式口径不会串：
 * - 服务端返回 `yyyy-MM-dd HH:mm:ss`（空格）
 * - Query 参数必须 `yyyy-MM-ddTHH:mm:ss`（T）
 * - JSON 请求体必须 `yyyy-MM-dd HH:mm:ss`（空格）
 * 串了口径后端会直接 40000，这类回归在生产里很难一眼看出来。
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  parseServerTime,
  formatDateTime,
  serverTimeToPicker,
  toQueryDateTime,
  toBodyDateTime,
  relativeTime,
} from '../src/utils/datetime.ts'

test('parseServerTime：空格与 T 两种分隔都能解析，空值/非法值返回 null', () => {
  const a = parseServerTime('2026-11-18 17:30:00')
  const b = parseServerTime('2026-11-18T17:30:00')
  assert.ok(a instanceof Date)
  assert.ok(b instanceof Date)
  assert.equal(a?.getTime(), b?.getTime())
  assert.equal(a?.getFullYear(), 2026)
  assert.equal(a?.getMonth(), 10) // 0-based
  assert.equal(a?.getDate(), 18)
  assert.equal(a?.getHours(), 17)
  assert.equal(a?.getMinutes(), 30)

  assert.equal(parseServerTime(''), null)
  assert.equal(parseServerTime(undefined), null)
  assert.equal(parseServerTime(null), null)
  assert.equal(parseServerTime('not-a-date'), null)
})

test('formatDateTime：输出恒定 `yyyy-MM-dd HH:mm:ss`，空值回落到原值或占位符', () => {
  assert.equal(formatDateTime('2026-11-18 07:05:09'), '2026-11-18 07:05:09')
  assert.equal(formatDateTime('2026-11-18T07:05:09'), '2026-11-18 07:05:09')
  // 个单位数必须补零，否则表格列会左右跳动
  assert.equal(formatDateTime('2026-01-02 03:04:05'), '2026-01-02 03:04:05')
  assert.equal(formatDateTime(''), '-')
  assert.equal(formatDateTime(undefined), '-')
  assert.equal(formatDateTime(null), '-')
  // 非法值原样返回，便于排查后端问题而不是显示成「Invalid Date」
  assert.equal(formatDateTime('garbage'), 'garbage')
})

test('serverTimeToPicker：回填 el-date-picker 用 T 分隔', () => {
  assert.equal(serverTimeToPicker('2026-11-18 09:00:00'), '2026-11-18T09:00:00')
  assert.equal(serverTimeToPicker(''), '')
  assert.equal(serverTimeToPicker(undefined), '')
})

test('toQueryDateTime：Query 参数必须是 T 分隔（对应 @DateTimeFormat ISO.DATE_TIME）', () => {
  assert.equal(toQueryDateTime('2026-11-18 09:00:00'), '2026-11-18T09:00:00')
  assert.equal(toQueryDateTime('2026-11-18T09:00:00'), '2026-11-18T09:00:00')
  assert.equal(toQueryDateTime(''), undefined)
  assert.equal(toQueryDateTime(undefined), undefined)
  assert.equal(toQueryDateTime('garbage'), undefined)
})

test('toBodyDateTime：JSON 请求体必须是空格分隔（对应 @JsonFormat）', () => {
  assert.equal(toBodyDateTime('2026-11-18T09:00:00'), '2026-11-18 09:00:00')
  assert.equal(toBodyDateTime('2026-11-18 09:00:00'), '2026-11-18 09:00:00')
  assert.equal(toBodyDateTime(''), undefined)
  assert.equal(toBodyDateTime('garbage'), undefined)
})

test('toQueryDateTime 与 toBodyDateTime 口径不同：一个带 T，一个带空格', () => {
  const src = '2026-11-18 09:00:00'
  const q = toQueryDateTime(src)
  const b = toBodyDateTime(src)
  assert.ok(q && b)
  assert.ok(q.includes('T'))
  assert.ok(!q.includes(' '))
  assert.ok(b.includes(' '))
  assert.ok(!b.includes('T'))
})

test('relativeTime：刚刚/分钟/小时/天，超 30 天回落到日期', () => {
  const now = Date.now()
  // 注意：不能用 toISOString()——那是 UTC，而 parseServerTime 按本地时区解释，
  // 在 UTC+8 的机器上会整段偏移 8 小时，用例就得跟着时区改。这里手工拼本地时间。
  const pad = (n: number) => String(n).padStart(2, '0')
  const fmt = (offsetMs: number) => {
    const d = new Date(now - offsetMs)
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
      + `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }

  // 未来时间（服务端时钟略快）也应当收敛到「刚刚」，不能出现「-3 分钟前」
  assert.equal(relativeTime(fmt(-60_000)), '刚刚')
  assert.equal(relativeTime(fmt(10_000)), '刚刚')
  assert.equal(relativeTime(fmt(5 * 60_000)), '5 分钟前')
  assert.equal(relativeTime(fmt(3 * 3_600_000)), '3 小时前')
  assert.equal(relativeTime(fmt(2 * 86_400_000)), '2 天前')

  const old = fmt(40 * 86_400_000)
  assert.match(relativeTime(old), /^\d{4}-\d{2}-\d{2}$/)

  assert.equal(relativeTime(''), '-')
  assert.equal(relativeTime(undefined), '-')
})
