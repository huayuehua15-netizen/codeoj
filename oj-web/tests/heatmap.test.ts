/**
 * 热力图栅格构建的单测：周对齐、补零、连续天数三个 off-by-one 高发点。
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { buildHeatmapGrid } from '../src/utils/heatmap.ts'

function d(date: string): string {
  return date
}

test('26 周 + 当前不完整周 = 27 列，首列对齐到周日、末列含 end', () => {
  // 2026-11-19 是周四；起点 = end-181 天（2026-05-22 周五）回退到周日 2026-05-17
  // 周日对齐 + end 在周四 → 最后一列是不完整的周（与 GitHub 行为一致）
  const grid = buildHeatmapGrid(new Date(2026, 10, 19), [])
  assert.equal(grid.columns.length, 27)
  assert.equal(grid.columns[0].length, 7)
  assert.equal(grid.columns[0][0].date, '2026-05-17')
  assert.equal(grid.columns[0][0].count, 0)
  // 最后一格正好是 end 当天（周四 = 第 4 行），末列周六格是"未来"，恒为 0
  const last = grid.columns[grid.columns.length - 1]
  assert.equal(last[4].date, '2026-11-19')
  assert.equal(last[6].date, '2026-11-21')
  assert.equal(last[6].count, 0)
})

test('稀疏数据补零且超出范围的数据不越界', () => {
  const grid = buildHeatmapGrid(new Date(2026, 10, 19), [
    { date: '2026-11-18', total: 3, accepted: 2 },
    { date: '2026-01-01', total: 99, accepted: 1 }, // 范围外，必须被忽略
    { date: '2026-11-19', total: 1, accepted: 1 },
  ])
  const last = grid.columns[grid.columns.length - 1]
  assert.equal(last[3].count, 3) // 周三 11-18
  assert.equal(last[4].count, 1) // 周四 11-19
  assert.equal(last[4].level, 1)
  assert.equal(last[3].level, 2)
  assert.equal(grid.totalInRange, 4) // 99 不计入
  assert.equal(grid.activeDays, 2)
})

test('streak：end 当天没提交时回退昨天起算', () => {
  // end=11-19 无提交；11-18、11-17 连续 → streak=2
  const grid = buildHeatmapGrid(new Date(2026, 10, 19), [
    { date: '2026-11-17', total: 2, accepted: 1 },
    { date: '2026-11-18', total: 1, accepted: 1 },
  ])
  assert.equal(grid.streak, 2)
})

test('streak：end 当天有提交则连续累加', () => {
  const grid = buildHeatmapGrid(new Date(2026, 10, 19), [
    { date: '2026-11-19', total: 1, accepted: 0 },
    { date: '2026-11-18', total: 1, accepted: 0 },
    { date: '2026-11-16', total: 1, accepted: 0 }, // 17 号断了
  ])
  assert.equal(grid.streak, 2)
})

test('色阶阈值：1 / 3 / 6 / 7+ 四档', () => {
  const grid = buildHeatmapGrid(new Date(2026, 10, 19), [
    { date: '2026-11-13', total: 1, accepted: 0 }, // 周五
    { date: '2026-11-14', total: 3, accepted: 0 },
    { date: '2026-11-15', total: 6, accepted: 0 },
    { date: '2026-11-16', total: 7, accepted: 0 },
  ])
  const col = (date: string) => {
    for (const col of grid.columns) for (const c of col) if (c.date === date) return c
    throw new Error(date)
  }
  assert.equal(col('2026-11-13').level, 1)
  assert.equal(col('2026-11-14').level, 2)
  assert.equal(col('2026-11-15').level, 3)
  assert.equal(col('2026-11-16').level, 4)
})

test('月份标签只在每月第一列出现', () => {
  const grid = buildHeatmapGrid(new Date(2026, 10, 19), [])
  const labels = grid.monthLabels.map((m) => m.label)
  // 26 周 ≈ 6 个月，11 月的"11月"必须在
  assert.ok(labels.includes('11月'))
  // 不重复
  assert.equal(new Set(labels).size, labels.length)
})

test('空数据：全部 0 级、streak 为 0', () => {
  const grid = buildHeatmapGrid(new Date(2026, 10, 19), [])
  assert.equal(grid.totalInRange, 0)
  assert.equal(grid.streak, 0)
  assert.ok(grid.columns.every((c) => c.every((cell) => cell.level === 0)))
})
