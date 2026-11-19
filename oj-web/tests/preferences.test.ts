import test from 'node:test'
import assert from 'node:assert/strict'

import {
  DEFAULT_PREFERENCES,
  FONT_SIZE_MAX,
  FONT_SIZE_MIN,
  normalizeEditorPreferences,
} from '../src/utils/preferences.ts'

test('normalizeEditorPreferences：空值/非对象回落默认', () => {
  assert.deepEqual(normalizeEditorPreferences(null), DEFAULT_PREFERENCES)
  assert.deepEqual(normalizeEditorPreferences('broken'), DEFAULT_PREFERENCES)
})

test('normalizeEditorPreferences：字号越界与小数回落默认', () => {
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, fontSize: FONT_SIZE_MAX }).fontSize, FONT_SIZE_MAX)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, fontSize: FONT_SIZE_MIN }).fontSize, FONT_SIZE_MIN)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, fontSize: 99 }).fontSize, DEFAULT_PREFERENCES.fontSize)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, fontSize: 14.5 }).fontSize, DEFAULT_PREFERENCES.fontSize)
})

test('normalizeEditorPreferences：缩进只接受 2/4，语言必须在语言表内', () => {
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, tabSize: 2 }).tabSize, 2)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, tabSize: 4 }).tabSize, 4)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, tabSize: 8 }).tabSize, DEFAULT_PREFERENCES.tabSize)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, defaultLanguage: 'python3' }).defaultLanguage, 'python3')
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, defaultLanguage: 'go' }).defaultLanguage, DEFAULT_PREFERENCES.defaultLanguage)
})

test('normalizeEditorPreferences：布尔偏好只接受 boolean', () => {
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, wordWrap: true }).wordWrap, true)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, wordWrap: 'yes' }).wordWrap, DEFAULT_PREFERENCES.wordWrap)
  assert.equal(normalizeEditorPreferences({ ...DEFAULT_PREFERENCES, minimap: false }).minimap, false)
})
