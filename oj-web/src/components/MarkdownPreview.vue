<template>
  <div class="markdown-body" v-html="rendered"></div>
</template>

<script setup lang="ts">
// 题面 Markdown 渲染（论文 4.7：富文本渲染转义防 XSS）
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps<{ content?: string }>()

const rendered = computed(() => {
  if (!props.content) return ''
  marked.setOptions({ async: false })
  const html = marked.parse(props.content, { async: false }) as string
  // 白名单净化：剔除 script/on* 属性等注入面，仅保留安全标签。
  // 额外禁用 <style> 与 style 属性（DOMPurify 默认白名单会放行，
  // 题面可借此注入 CSS 覆盖页面元素做视觉伪造）；排版由本组件 scoped CSS 提供。
  return DOMPurify.sanitize(html, {
    FORBID_TAGS: ['style'],
    FORBID_ATTR: ['style'],
  })
})
</script>

<style scoped>
.markdown-body {
  line-height: 1.9;
  word-break: break-word;
  font-size: 14px;
  color: var(--oj-gray-800);
}

/* 标题：无衬线、紧凑字距，跟界面同一套语气 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 24px 0 10px;
  font-family: var(--oj-font-sans);
  font-weight: 700;
  line-height: 1.45;
  letter-spacing: -0.02em;
  color: var(--oj-text);
}
.markdown-body :deep(h1) {
  font-size: 20px;
}
.markdown-body :deep(h2) {
  font-size: 17.5px;
  border-bottom: 1px solid var(--oj-border);
  padding-bottom: 6px;
}
.markdown-body :deep(h3) {
  font-size: 15px;
  color: var(--oj-gray-800);
}
.markdown-body :deep(h4) {
  margin: 16px 0 8px;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

/* 行内代码：浅灰底的小标签 */
.markdown-body :deep(code) {
  background: var(--oj-gray-100);
  color: var(--oj-gray-800);
  border: 1px solid var(--oj-border);
  border-radius: 4px;
  padding: 1px 6px;
  font-family: var(--oj-font-mono);
  font-size: 12.5px;
}

/* 代码块：界面里唯一的深色块 */
.markdown-body :deep(pre) {
  background: var(--oj-code-bg);
  color: var(--oj-code-text);
  padding: 15px 18px;
  border-radius: 8px;
  overflow: auto;
  margin: 14px 0;
  border: 1px solid var(--oj-code-border);
}
.markdown-body :deep(pre code) {
  background: transparent;
  color: inherit;
  border: 0;
  padding: 0;
  font-size: 12.5px;
  line-height: 1.75;
}
/* 代码块里的强调不能落回正文墨色，否则在深底上几乎不可读 */
.markdown-body :deep(pre strong),
.markdown-body :deep(pre code strong) {
  color: inherit;
  font-weight: 700;
}

/* 列表/引用/表格 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}
.markdown-body :deep(li) {
  margin: 5px 0;
}
.markdown-body :deep(blockquote) {
  margin: 12px 0;
  padding: 6px 0 6px 14px;
  border-left: 2px solid var(--oj-seal);
  background: transparent;
  color: var(--oj-text-sub);
  font-style: normal;
}
.markdown-body :deep(blockquote p) {
  margin: 0;
}
.markdown-body :deep(a) {
  color: var(--oj-primary);
  border-bottom: 1px solid rgba(35, 68, 92, 0.25);
}
.markdown-body :deep(a:hover) {
  border-bottom-color: var(--oj-primary);
}
.markdown-body :deep(p) {
  margin: 11px 0;
}
.markdown-body :deep(strong) {
  font-weight: 700;
  color: var(--oj-text);
}
.markdown-body :deep(hr) {
  border: 0;
  border-top: 1px solid var(--oj-border);
  margin: 20px 0;
}
.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 14px 0;
  font-size: 13px;
  width: 100%;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--oj-border);
  padding: 7px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: var(--oj-gray-50);
  font-family: var(--oj-font-mono);
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: var(--oj-text-sub);
}

/* 嵌入式交互控件：比正文略深一档，但不喧宾夺主 */
.markdown-body :deep(select) {
  appearance: auto;
  padding: 6px 30px 6px 10px;
  border: 1px solid var(--oj-border);
  border-radius: 1px;
  background: var(--oj-card)
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2323445c' stroke-width='2.5'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E")
    no-repeat right 10px center;
  font-size: 14px;
  color: var(--oj-text);
  cursor: pointer;
}
.markdown-body :deep(select:hover) {
  border-color: var(--oj-border-strong);
}
.markdown-body :deep(select:focus) {
  outline: none;
  border-color: var(--oj-primary);
}
.markdown-body :deep(input[type='text']),
.markdown-body :deep(input:not([type])) {
  padding: 6px 10px;
  border: 1px solid var(--oj-border);
  border-radius: 1px;
  font-size: 14px;
  font-family: inherit;
  background: var(--oj-card);
  color: var(--oj-text);
}
.markdown-body :deep(input:focus) {
  outline: none;
  border-color: var(--oj-primary);
}
.markdown-body :deep(button) {
  padding: 5px 14px;
  border: 1px solid var(--oj-primary);
  border-radius: 1px;
  background: var(--oj-primary);
  color: var(--oj-white);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: background var(--oj-duration) var(--oj-ease);
}
.markdown-body :deep(button:hover) {
  background: var(--oj-primary-hover);
  border-color: var(--oj-primary-hover);
}
.markdown-body :deep(details) {
  margin: 12px 0;
  border: 1px solid var(--oj-border);
  border-radius: 1px;
  padding: 8px 14px;
  background: var(--oj-gray-50);
}
.markdown-body :deep(summary) {
  color: var(--oj-primary);
  font-weight: 600;
  cursor: pointer;
  user-select: none;
}
</style>
