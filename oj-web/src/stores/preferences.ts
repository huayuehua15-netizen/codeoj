import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  DEFAULT_PREFERENCES,
  normalizeEditorPreferences,
  type EditorPreferences,
} from '@/utils/preferences'

const STORAGE_KEY = 'oj_editor_prefs'

function read(): EditorPreferences {
  if (typeof localStorage === 'undefined') return { ...DEFAULT_PREFERENCES }
  try {
    return normalizeEditorPreferences(JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null'))
  } catch {
    return { ...DEFAULT_PREFERENCES }
  }
}

/**
 * 编辑器偏好：字号、缩进、换行、代码地图、默认语言。
 * 只落 localStorage；不随账号同步，换设备/清缓存后回到默认。
 */
export const usePreferencesStore = defineStore('preferences', () => {
  const initial = read()
  const fontSize = ref(initial.fontSize)
  const tabSize = ref(initial.tabSize)
  const wordWrap = ref(initial.wordWrap)
  const minimap = ref(initial.minimap)
  const defaultLanguage = ref(initial.defaultLanguage)

  function snapshot(): EditorPreferences {
    return {
      fontSize: fontSize.value,
      tabSize: tabSize.value,
      wordWrap: wordWrap.value,
      minimap: minimap.value,
      defaultLanguage: defaultLanguage.value,
    }
  }

  function persist() {
    if (typeof localStorage === 'undefined') return
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizeEditorPreferences(snapshot())))
    } catch {
      // 浏览器禁用存储时保持内存态，不阻断编辑器使用
    }
  }

  function reset() {
    fontSize.value = DEFAULT_PREFERENCES.fontSize
    tabSize.value = DEFAULT_PREFERENCES.tabSize
    wordWrap.value = DEFAULT_PREFERENCES.wordWrap
    minimap.value = DEFAULT_PREFERENCES.minimap
    defaultLanguage.value = DEFAULT_PREFERENCES.defaultLanguage
  }

  watch([fontSize, tabSize, wordWrap, minimap, defaultLanguage], persist)

  return { fontSize, tabSize, wordWrap, minimap, defaultLanguage, reset }
})
