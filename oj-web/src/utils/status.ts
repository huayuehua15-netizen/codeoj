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
 * 难度映射（论文表 4-2：1=简单，2=中等，3=困难）。
 */
export const DIFFICULTY_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '简单', color: '#67c23a' },
  2: { label: '中等', color: '#e6a23c' },
  3: { label: '困难', color: '#f56c6c' },
}

/**
 * 四语言模板代码（论文 5.7：按题目允许的语言切换语法模式并载入对应模板代码）。
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
