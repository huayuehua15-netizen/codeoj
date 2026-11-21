/**
 * 判题状态映射（论文表 4-8）。
 */
export const STATUS_MAP: Record<number, { label: string; type: 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
  0: { label: '排队中', type: 'info' },
  1: { label: '判题中', type: 'primary' },
  2: { label: '通过', type: 'success' },
  3: { label: '答案错误', type: 'danger' },
  4: { label: '超时', type: 'warning' },
  5: { label: '超内存', type: 'warning' },
  6: { label: '运行错误', type: 'danger' },
  7: { label: '编译错误', type: 'warning' },
  8: { label: '系统错误', type: 'danger' },
}

export function statusLabel(status?: number) {
  return status != null ? STATUS_MAP[status]?.label || '未知' : '-'
}

export function statusType(status?: number) {
  return status != null ? STATUS_MAP[status]?.type || 'info' : 'info'
}

/**
 * 读取设计 token 的实际色值。
 *
 * 为什么需要它：SFC 样式里可以直接写 `var(--oj-x)`，但 **ECharts 画在 canvas 上**
 * （以及任何需要真实色值而非 CSS 引用的场景），拿不到变量解析结果。
 * 这里统一从 :root 上取一次，色板仍然只有 styles/index.css 一份。
 */
export function cssVar(name: string, fallback = '#8a8f98'): string {
  if (typeof document === 'undefined') return fallback
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

/**
 * 状态主色 token：同族状态同色（AC 绿 / WA·RE·SE 红 / TLE·MLE·CE 橙 / 判题中主题色 / 排队中灰）。
 * 只存 token 名；需要真实色值（ECharts canvas 等）的地方统一走 statusColor() 解析，
 * 列表、图表、判定卡从此不会各写一份映射漂移。
 */
const STATUS_TOKEN: Record<number, string> = {
  0: '--oj-gray-500',
  1: '--oj-primary',
  2: '--oj-success',
  3: '--oj-danger',
  4: '--oj-warning',
  5: '--oj-warning',
  6: '--oj-danger',
  7: '--oj-warning',
  8: '--oj-danger',
}

const GRAY_TOKEN = '--oj-gray-500'

/**
 * 状态主色：浏览器里解析为真实色值（ECharts canvas 等拿不到 CSS 变量的场景用）；
 * 无 DOM 环境（node 单测）退回 `var(--oj-x)` 引用串，保持与 CSS 同源。
 */
export function statusColor(status?: number) {
  const token = status != null ? STATUS_TOKEN[status] ?? GRAY_TOKEN : GRAY_TOKEN
  if (typeof document === 'undefined') return `var(${token})`
  return cssVar(token)
}

/**
 * 状态浅底色：判题结果横幅等大面积背景用（与 statusColor 同族）。
 */
const STATUS_TINT: Record<number, string> = {
  0: 'var(--oj-tint-gray)',
  1: 'var(--oj-tint-primary)',
  2: 'var(--oj-tint-success)',
  3: 'var(--oj-tint-danger)',
  4: 'var(--oj-tint-warning)',
  5: 'var(--oj-tint-warning)',
  6: 'var(--oj-tint-danger)',
  7: 'var(--oj-tint-warning)',
  8: 'var(--oj-tint-danger)',
}

export function statusTint(status?: number) {
  return status != null ? STATUS_TINT[status] || STATUS_TINT[0] : STATUS_TINT[0]
}

/**
 * 判定卡图标：通过 ✓ / 超限类（TLE·MLE·CE）！/ 失败 ✕。
 * 与 statusColor 同族配套：CE·TLE·MLE 是"值得警惕"而不是"彻底错误"，不该一律打红叉。
 */
export function statusGlyph(status?: number): string {
  if (status === 2) return '✓'
  if (status === 4 || status === 5 || status === 7) return '!'
  return '✕'
}

/**
 * 耗时呈现：`12 ms`，≥1s 转秒（`1.50 s`）；空值显示占位符。
 */
export function formatTimeUsed(ms?: number | null): string {
  if (ms == null) return '—'
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}

/**
 * 内存呈现：`3840 KB`，≥1MB 转兆（`3.8 MB`）；空值显示占位符。
 */
export function formatMemory(kb?: number | null): string {
  if (kb == null) return '—'
  if (kb < 1024) return `${kb} KB`
  return `${(kb / 1024).toFixed(1)} MB`
}

/**
 * 难度映射（论文表 4-2：1=简单，2=中等，3=困难）。
 * color 供 el-tag / el-progress 使用，可直接是 CSS 变量引用。
 */
export const DIFFICULTY_MAP: Record<number, { label: string; color: string; token: string }> = {
  1: { label: '简单', color: 'var(--oj-success)', token: '--oj-success' },
  2: { label: '中等', color: 'var(--oj-warning)', token: '--oj-warning' },
  3: { label: '困难', color: 'var(--oj-danger)', token: '--oj-danger' },
}

/**
 * 五语言模板代码（论文 5.7：按题目允许的语言切换语法模式并载入对应模板代码）。
 */
export const CODE_TEMPLATES: Record<string, string> = {
  js: `// 在此编写你的代码
`,

  c: `#include <stdio.h>

int main(void) {
    // 在此编写你的代码
    return 0;
}
`,
  cpp: `#include <iostream>

int main() {
    // 在此编写你的代码
    return 0;
}
`,
  java: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // 在此编写你的代码
    }
}
`,
  python3: `# 在此编写你的代码
`,
}

export const LANGUAGE_OPTIONS = [
  { value: 'c', label: 'C' },
  { value: 'cpp', label: 'C++' },
  { value: 'java', label: 'Java' },
  { value: 'python3', label: 'Python3' },
  { value: 'js', label: 'JavaScript' },
]

/** monaco 语法模式映射（论文 5.7：c → cpp → java → python） */
export const MONACO_LANGUAGE: Record<string, string> = {
  c: 'c',
  cpp: 'cpp',
  java: 'java',
  python3: 'python',
  js: 'javascript',
}
