/**
 * 服务端时间工具。
 *
 * 后端约定（docs/API.md §0.1 + JacksonConfig）：
 * - 存储与传输均为 `LocalDateTime`，序列化格式固定 `yyyy-MM-dd HH:mm:ss`（无 T、无时区后缀）；
 * - 容器/服务端时区为 Asia/Shanghai，与目标用户同一时区，
 *   故前端**不做时区换算**，原样解析、原样展示即可。
 *
 * 两类入参格式不同，务必区分：
 * - Query 参数（如 /api/submissions 的 begin/end）走 Spring 的
 *   `@DateTimeFormat(iso = ISO.DATE_TIME)` → 必须是 `yyyy-MM-ddTHH:mm:ss`（T 分隔）；
 * - JSON 请求体（如竞赛的 startTime/endTime）走 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`
 *   → 必须是 `yyyy-MM-dd HH:mm:ss`（空格分隔）。
 *
 * 所有函数对空值/非法值都返回安全结果，不抛异常。
 */

const pad = (n: number) => String(n).padStart(2, '0')

/** 解析服务端时间串：兼容空格与 T 两种分隔，按本地时区解释 */
export function parseServerTime(value?: string | null): Date | null {
  if (!value) return null
  // "YYYY-MM-DD HH:mm:ss" → 补 T 才能被各浏览器一致解析（本地时区）
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

/** 服务端时间 → 本地可读 `yyyy-MM-dd HH:mm:ss` */
export function formatDateTime(value?: string | null): string {
  const d = parseServerTime(value)
  if (!d) return value || '-'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
    + `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 服务端时间 → el-date-picker 回填值（`YYYY-MM-DDTHH:mm:ss`） */
export function serverTimeToPicker(value?: string | null): string {
  const d = parseServerTime(value)
  if (!d) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
    + `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/**
 * el-date-picker 值 → Query 参数格式 `yyyy-MM-ddTHH:mm:ss`。
 * 对应后端 @DateTimeFormat(iso = ISO.DATE_TIME)。
 */
export function toQueryDateTime(value?: string | null): string | undefined {
  const d = parseServerTime(value)
  if (!d) return undefined
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
    + `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/**
 * el-date-picker 值 → JSON 请求体格式 `yyyy-MM-dd HH:mm:ss`。
 * 对应后端 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")。
 */
export function toBodyDateTime(value?: string | null): string | undefined {
  const d = parseServerTime(value)
  if (!d) return undefined
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
    + `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
