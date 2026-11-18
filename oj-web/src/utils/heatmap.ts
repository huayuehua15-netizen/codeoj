/**
 * 提交热力图（个人主页）的日期栅格构建。
 *
 * 纯函数、无 DOM 依赖，方便用 node:test 钉住边界：
 * 周对齐、数据补零、连续天数（streak）这三处最容易出 off-by-one。
 */

/** trend 接口（API.md §4.4a）返回的一行 */
export interface HeatmapDay {
  date: string
  total: number
  accepted: number
}

/** 热力图单格 */
export interface HeatmapCell {
  date: string
  count: number
  /** 0=无提交，1-4 越深越密集 */
  level: 0 | 1 | 2 | 3 | 4
}

export interface HeatmapGrid {
  /** 周为列、星期为行：cells[列][行0=周日 … 行6=周六]，与 GitHub 布局一致 */
  columns: HeatmapCell[][]
  /** 月份标签：从某列开始显示月份名 */
  monthLabels: { column: number; label: string }[]
  /** 范围内总提交次数 */
  totalInRange: number
  /** 连续刷题天数：以 end 结束（end 当天没提交则回退一天起算） */
  streak: number
  /** 范围内的活跃天数 */
  activeDays: number
}

const MONTH_LABELS = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
const DAY_MS = 24 * 60 * 60 * 1000

function keyOf(d: Date): string {
  // 只用本地年月日拼 key，避免 toISOString 把时区带偏一天
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/** count → 色阶。阈值取"一道题从看题到 AC"的真实节奏，而不是线性切分 */
function levelOf(count: number): HeatmapCell['level'] {
  if (count <= 0) return 0
  if (count === 1) return 1
  if (count <= 3) return 2
  if (count <= 6) return 3
  return 4
}

/**
 * 构建以 end 结束、共 weeks 周的热力图栅格。
 * @param data trend 返回的稀疏数据（只有有提交的日子），函数内部补零
 */
export function buildHeatmapGrid(end: Date, data: HeatmapDay[], weeks = 26): HeatmapGrid {
  const endMidnight = new Date(end.getFullYear(), end.getMonth(), end.getDate())
  // 起点回退到该列的周日（getDay(): 0=周日），保证第一列是完整的一周
  const start = new Date(endMidnight.getTime() - (weeks * 7 - 1) * DAY_MS)
  start.setDate(start.getDate() - start.getDay())

  const counts = new Map<string, number>()
  let totalInRange = 0
  let activeDays = 0
  for (const d of data) {
    // 只统计范围内，防止超长 days 参数带来的重复日期越界
    const dt = new Date(`${d.date}T00:00:00`)
    if (dt < start || dt > endMidnight) continue
    counts.set(d.date, d.total)
    totalInRange += d.total
    if (d.total > 0) activeDays++
  }

  const columns: HeatmapCell[][] = []
  const monthLabels: { column: number; label: string }[] = []
  const cursor = new Date(start)
  for (let c = 0; cursor <= endMidnight; c++) {
    if (cursor.getDate() <= 7 && cursor.getDay() === 0) {
      // 每列首格是这个月的第 1 周 → 标一次月份
      monthLabels.push({ column: c, label: MONTH_LABELS[cursor.getMonth()] })
    }
    const col: HeatmapCell[] = []
    for (let r = 0; r < 7; r++) {
      const key = keyOf(cursor)
      const count = cursor > endMidnight ? 0 : counts.get(key) ?? 0
      col.push({ date: key, count, level: levelOf(count) })
      cursor.setDate(cursor.getDate() + 1)
    }
    columns.push(col)
  }

  // 连续天数：从 end 往前数；end 当天没提交则从昨天开始（夜里刷题白天看的人不该是 0）
  let streak = 0
  const probe = new Date(endMidnight)
  if ((counts.get(keyOf(probe)) ?? 0) === 0) probe.setDate(probe.getDate() - 1)
  while ((counts.get(keyOf(probe)) ?? 0) > 0) {
    streak++
    probe.setDate(probe.getDate() - 1)
  }

  return { columns, monthLabels, totalInRange, streak, activeDays }
}
