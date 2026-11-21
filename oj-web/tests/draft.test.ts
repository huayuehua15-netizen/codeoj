import test from 'node:test'
import assert from 'node:assert/strict'

import {
  clearDraft,
  draftStorageKey,
  isTemplateCode,
  loadDraft,
  saveDraft,
  type DraftStorage,
} from '../src/utils/draft.ts'
import { CODE_TEMPLATES } from '../src/utils/status.ts'

/** 测试用内存存储：与 localStorage 同接口 */
function fakeStorage(): DraftStorage & { data: Map<string, string> } {
  const data = new Map<string, string>()
  return {
    data,
    getItem: (k) => (data.has(k) ? data.get(k)! : null),
    setItem: (k, v) => void data.set(k, v),
    removeItem: (k) => void data.delete(k),
  }
}

test('draftStorageKey：按题目+语言分维度', () => {
  assert.equal(draftStorageKey(7, 'cpp'), 'oj_draft:7:cpp')
  assert.equal(draftStorageKey('7', 'python3'), 'oj_draft:7:python3')
})

test('isTemplateCode：仅与模板原文精确匹配', () => {
  assert.equal(isTemplateCode('cpp', CODE_TEMPLATES.cpp!), true)
  // 空串不是模板——"空代码不算草稿"由 saveDraft 的 trim 检查兜住，模板比对只管模板原文
  assert.equal(isTemplateCode('cpp', ''), false)
  assert.equal(isTemplateCode('cpp', '   '), false)
  assert.equal(isTemplateCode('cpp', 'int main(){return 0;}'), false)
})

test('saveDraft/loadDraft：保存后能读回', () => {
  const store = fakeStorage()
  saveDraft(7, 'cpp', 'int main(){return 0;}', store)
  assert.equal(loadDraft(7, 'cpp', store), 'int main(){return 0;}')
})

test('saveDraft：空代码与模板原文不落盘', () => {
  const store = fakeStorage()
  saveDraft(7, 'cpp', '', store)
  assert.equal(store.data.size, 0)
  saveDraft(7, 'cpp', CODE_TEMPLATES.cpp!, store)
  assert.equal(store.data.size, 0)
  // 保存空代码还会顺带清掉旧草稿
  saveDraft(7, 'cpp', 'draft text', store)
  assert.equal(store.data.size, 1)
  saveDraft(7, 'cpp', '  ', store)
  assert.equal(store.data.size, 0)
})

test('saveDraft：不同语言互不覆盖', () => {
  const store = fakeStorage()
  saveDraft(7, 'cpp', 'cpp draft', store)
  saveDraft(7, 'python3', 'py draft', store)
  assert.equal(loadDraft(7, 'cpp', store), 'cpp draft')
  assert.equal(loadDraft(7, 'python3', store), 'py draft')
  assert.equal(loadDraft(8, 'cpp', store), undefined)
})

test('clearDraft：提交后清理对应维度', () => {
  const store = fakeStorage()
  saveDraft(7, 'cpp', 'cpp draft', store)
  saveDraft(7, 'python3', 'py draft', store)
  clearDraft(7, 'cpp', store)
  assert.equal(loadDraft(7, 'cpp', store), undefined)
  assert.equal(loadDraft(7, 'python3', store), 'py draft')
  // 重复清理幂等
  clearDraft(7, 'cpp', store)
  assert.equal(loadDraft(7, 'python3', store), 'py draft')
})

test('loadDraft：非字符串值视为无草稿', () => {
  const store = fakeStorage()
  store.setItem(draftStorageKey(7, 'cpp'), '')
  assert.equal(loadDraft(7, 'cpp', store), undefined)
})
