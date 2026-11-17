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
 * 状态主色：用于「彩色圆点 + 文字」的专业 OJ 呈现（替代通用 el-tag）。
 * 同族状态同色：AC 绿 / WA·RE·SE 红 / TLE·MLE·CE 橙 / 判题中主题色 / 排队中灰。
 */
const STATUS_COLOR: Record<number, string> = {
  0: '#6b7280',
  1: '#4f46e5',
  2: '#16a34a',
  3: '#dc2626',
  4: '#d97706',
  5: '#d97706',
  6: '#dc2626',
  7: '#d97706',
  8: '#dc2626',
}

export function statusColor(status?: number) {
  return status != null ? STATUS_COLOR[status] || '#6b7280' : '#6b7280'
}

/**
 * 状态浅底色：判题结果横幅等大面积背景用（与 statusColor 同族）。
 */
const STATUS_TINT: Record<number, string> = {
  0: 'rgba(107, 114, 128, 0.08)',
  1: 'rgba(79, 70, 229, 0.07)',
  2: 'rgba(22, 163, 74, 0.08)',
  3: 'rgba(220, 38, 38, 0.06)',
  4: 'rgba(217, 119, 6, 0.08)',
  5: 'rgba(217, 119, 6, 0.08)',
  6: 'rgba(220, 38, 38, 0.06)',
  7: 'rgba(217, 119, 6, 0.08)',
  8: 'rgba(220, 38, 38, 0.06)',
}

export function statusTint(status?: number) {
  return status != null ? STATUS_TINT[status] || STATUS_TINT[0] : STATUS_TINT[0]
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
 */
export const DIFFICULTY_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '简单', color: '#67c23a' },
  2: { label: '中等', color: '#e6a23c' },
  3: { label: '困难', color: '#f56c6c' },
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
