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
  // 白名单净化：剔除 script/on* 属性等注入面，仅保留安全标签
  return DOMPurify.sanitize(html)
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
</style>
