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
  line-height: 1.85;
  word-break: break-word;
  font-size: 14.5px;
  color: var(--oj-text);
}

/* 标题：左侧强调条 + 字距 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  position: relative;
  padding-left: 12px;
  margin: 22px 0 10px;
  font-weight: 700;
  line-height: 1.4;
}
.markdown-body :deep(h1) { font-size: 19px; }
.markdown-body :deep(h2) {
  font-size: 17px;
  border-bottom: 1px solid var(--oj-border);
  padding-bottom: 6px;
}
.markdown-body :deep(h2)::before,
.markdown-body :deep(h3)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--oj-primary), var(--oj-accent));
}
.markdown-body :deep(h3)::before {
  bottom: 2px;
}
.markdown-body :deep(h3) {
  font-size: 15px;
}

/* 行内代码：浅蓝胶囊 */
.markdown-body :deep(code) {
  background: var(--oj-primary-soft);
  color: var(--oj-primary-deep);
  border-radius: 5px;
  padding: 1.5px 7px;
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, Menlo, monospace;
  font-size: 13px;
}

/* 代码块：深色高对比（代码感） */
.markdown-body :deep(pre) {
  background: #0f172a;
  color: #e2e8f0;
  padding: 14px 16px;
  border-radius: 10px;
  overflow: auto;
  margin: 12px 0;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}
.markdown-body :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  font-size: 13px;
  line-height: 1.7;
}

/* 列表/引用/表格 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}
.markdown-body :deep(li) {
  margin: 4px 0;
}
.markdown-body :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 14px;
  border-left: 4px solid var(--oj-primary);
  background: var(--oj-primary-soft);
  border-radius: 0 8px 8px 0;
  color: var(--oj-text-sub);
}
.markdown-body :deep(blockquote p) {
  margin: 0;
}
.markdown-body :deep(a) {
  color: var(--oj-primary);
  font-weight: 600;
}
.markdown-body :deep(p) {
  margin: 10px 0;
}
.markdown-body :deep(strong) {
  font-weight: 700;
}

/* 嵌入式交互控件：让下拉框/输入框/按钮/折叠块在题面里一眼可识别 */
.markdown-body :deep(select) {
  appearance: auto;
  padding: 6px 30px 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%232563eb' stroke-width='2.5'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E")
    no-repeat right 10px center;
  font-size: 14px;
  color: var(--oj-text);
  cursor: pointer;
}
.markdown-body :deep(select:hover) {
  border-color: var(--oj-primary);
}
.markdown-body :deep(select:focus) {
  outline: none;
  border-color: var(--oj-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}
.markdown-body :deep(input[type='text']),
.markdown-body :deep(input:not([type])) {
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
}
.markdown-body :deep(input:focus) {
  outline: none;
  border-color: var(--oj-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}
.markdown-body :deep(button) {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  background: var(--oj-primary);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.markdown-body :deep(button:hover) {
  filter: brightness(1.1);
}
.markdown-body :deep(details) {
  margin: 10px 0;
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  padding: 8px 14px;
  background: #f8fafc;
}
.markdown-body :deep(summary) {
  color: var(--oj-primary);
  font-weight: 600;
  cursor: pointer;
  user-select: none;
}
</style>
