<template>
  <div
    ref="editorEl"
    class="code-editor"
    role="textbox"
    aria-multiline="true"
    :aria-label="`${languageLabel} 代码编辑器`"
    aria-roledescription="代码编辑器"
  ></div>
</template>

<script setup lang="ts">
// 在线编辑器组件（论文 5.7）：封装 Monaco Editor，按语言切换语法模式并载入模板代码
// 先注册 worker 环境，再引入 monaco，避免 defaultWorkerFactory 抛错后回落到主线程
import '@/monacoWorker'
import * as monaco from 'monaco-editor'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { LANGUAGE_OPTIONS, MONACO_LANGUAGE } from '@/utils/status'

const props = defineProps<{
  modelValue: string
  language: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const languageLabel = computed(
  () => LANGUAGE_OPTIONS.find((l) => l.value === props.language)?.label ?? props.language,
)

const editorEl = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null
let changeDisposable: monaco.IDisposable | null = null

onMounted(() => {
  editor = monaco.editor.create(editorEl.value!, {
    value: props.modelValue ?? '',
    language: MONACO_LANGUAGE[props.language] ?? 'cpp',
    theme: 'vs-dark',
    automaticLayout: true, // 随容器自适应（论文 5.7）
    fontSize: 14,
    fontFamily:
      "'JetBrains Mono', 'Cascadia Code', Consolas, Menlo, 'Courier New', monospace",
    lineHeight: 22,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    // 与 oj-web 的整体设计保持一致：轻圆角 + 适度留白 + 平滑滚动
    padding: { top: 12, bottom: 12 },
    roundedSelection: true,
    smoothScrolling: true,
    cursorBlinking: 'smooth',
    renderLineHighlight: 'line',
    scrollbar: { verticalScrollbarSize: 10, horizontalScrollbarSize: 10 },
    tabSize: 4,
    renderFinalNewline: 'on',
    // 移动端/触屏不弹软键盘时也保证可读（论文 5.7 的"在线编辑器"体验）
    wordWrap: 'off',
    accessibilitySupport: 'auto',
  })
  changeDisposable = editor.onDidChangeModelContent(() =>
    emit('update:modelValue', editor!.getValue()),
  )
})

watch(
  () => props.language,
  (lang) => {
    if (editor) {
      monaco.editor.setModelLanguage(editor.getModel()!, MONACO_LANGUAGE[lang] ?? 'cpp')
    }
  },
)

watch(
  () => props.modelValue,
  (value) => {
    if (editor && value !== editor.getValue()) {
      editor.setValue(value ?? '')
    }
  },
)

onUnmounted(() => {
  changeDisposable?.dispose()
  changeDisposable = null
  editor?.dispose() // standalone editor 会连同内部 model 一并释放
  editor = null
})
</script>

<style scoped>
.code-editor {
  height: 440px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--oj-code-bg);
}
</style>
