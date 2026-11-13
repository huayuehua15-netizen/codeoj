<template>
  <div class="markdown-body" v-html="rendered"></div>
</template>

<script setup lang="ts">
// 题面 Markdown 渲染（论文 4.7：富文本渲染转义防 XSS）
import { computed } from 'vue'
import { marked } from 'marked'

const props = defineProps<{ content?: string }>()

const rendered = computed(() => {
  if (!props.content) return ''
  // 同步渲染并转义原始 HTML，防止 XSS
  marked.setOptions({ async: false })
  return marked.parse(props.content, { async: false }) as string
})
</script>

<style scoped>
.markdown-body {
  line-height: 1.7;
  word-break: break-word;
}
.markdown-body :deep(pre) {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
}
.markdown-body :deep(code) {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}
</style>
