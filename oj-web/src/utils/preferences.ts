import { LANGUAGE_OPTIONS } from './status.ts'

/** 在线编辑器偏好（只保存在当前浏览器，不写账号数据库）。 */
export interface EditorPreferences {
  fontSize: number
  tabSize: number
  wordWrap: boolean
  minimap: boolean
  defaultLanguage: string
}

export const FONT_SIZE_MIN = 12
export const FONT_SIZE_MAX = 20
export const TAB_SIZE_OPTIONS = [2, 4] as const

export const DEFAULT_PREFERENCES: EditorPreferences = {
  fontSize: 14,
  tabSize: 4,
  wordWrap: false,
  minimap: false,
  defaultLanguage: 'cpp',
}

/**
 * 把 localStorage/旧版本留下的任意值收敛为合法偏好。
 * 不信任外部输入：字号越界回落默认、缩进只接受 2/4、语言必须在当前语言表内。
 */
export function normalizeEditorPreferences(input: unknown): EditorPreferences {
  const raw = typeof input === 'object' && input !== null ? (input as Record<string, unknown>) : {}
  const fontSize = Number(raw.fontSize)
  const tabSize = Number(raw.tabSize)
  const defaultLanguage = typeof raw.defaultLanguage === 'string' ? raw.defaultLanguage : ''

  return {
    fontSize:
      Number.isInteger(fontSize) && fontSize >= FONT_SIZE_MIN && fontSize <= FONT_SIZE_MAX
        ? fontSize
        : DEFAULT_PREFERENCES.fontSize,
    tabSize: TAB_SIZE_OPTIONS.includes(tabSize as (typeof TAB_SIZE_OPTIONS)[number])
      ? tabSize
      : DEFAULT_PREFERENCES.tabSize,
    wordWrap: typeof raw.wordWrap === 'boolean' ? raw.wordWrap : DEFAULT_PREFERENCES.wordWrap,
    minimap: typeof raw.minimap === 'boolean' ? raw.minimap : DEFAULT_PREFERENCES.minimap,
    defaultLanguage: LANGUAGE_OPTIONS.some((item) => item.value === defaultLanguage)
      ? defaultLanguage
      : DEFAULT_PREFERENCES.defaultLanguage,
  }
}
