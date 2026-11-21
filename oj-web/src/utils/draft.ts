import { CODE_TEMPLATES } from './status.ts'

/**
 * 代码草稿持久化（localStorage，按「题目 + 语言」维度）。
 *
 * 为什么需要它：题目页代码只存在于组件内存——切走页面、误刷新、浏览器崩溃
 * 都会把未提交的代码丢光。编辑器是本产品的核心场景，丢一次代码比任何 UI 瑕疵都伤。
 *
 * 约定：
 * - key = `oj_draft:{problemId}:{language}`，语言分开存（各语言模板不同）；
 * - 提交成功后清除草稿（代码已落库，草稿失去意义）；
 * - 空串与语言模板不保存——那不是用户写的内容，保存反而会让"恢复草稿"失真。
 *
 * 存储不可用（隐私模式/被禁用）时静默降级为无草稿，绝不阻断编辑器本身。
 */

/** 最小存储接口：与 localStorage 兼容，测试用 Map 假实现注入 */
export interface DraftStorage {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
}

export function draftStorageKey(problemId: number | string, language: string): string {
  return `oj_draft:${problemId}:${language}`
}

/** 该语言当前内容是否仍是模板（未动笔） */
export function isTemplateCode(language: string, code: string): boolean {
  const template = CODE_TEMPLATES[language]
  return template !== undefined && code === template
}

/** 保存草稿；空代码 / 模板原文 / 存储异常都不落盘 */
export function saveDraft(
  problemId: number | string,
  language: string,
  code: string,
  storage?: DraftStorage,
): void {
  const store = storage ?? (typeof localStorage === 'undefined' ? undefined : localStorage)
  if (!store) return
  const trimmed = code ?? ''
  if (!trimmed.trim() || isTemplateCode(language, trimmed)) {
    clearDraft(problemId, language, store)
    return
  }
  try {
    store.setItem(draftStorageKey(problemId, language), trimmed)
  } catch {
    // 配额满 / 隐私模式：保持内存态，不打断答题
  }
}

/** 读取草稿；无草稿 / 存储异常返回 undefined */
export function loadDraft(
  problemId: number | string,
  language: string,
  storage?: DraftStorage,
): string | undefined {
  const store = storage ?? (typeof localStorage === 'undefined' ? undefined : localStorage)
  if (!store) return undefined
  try {
    const value = store.getItem(draftStorageKey(problemId, language))
    return typeof value === 'string' && value.length > 0 ? value : undefined
  } catch {
    return undefined
  }
}

/** 清除草稿（提交成功后调用；存储异常静默） */
export function clearDraft(
  problemId: number | string,
  language: string,
  storage?: DraftStorage,
): void {
  const store = storage ?? (typeof localStorage === 'undefined' ? undefined : localStorage)
  if (!store) return
  try {
    store.removeItem(draftStorageKey(problemId, language))
  } catch {
    // 静默：清不掉的草稿不影响功能，最多多提示一次恢复
  }
}
