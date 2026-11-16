<template>
  <div ref="editorEl" class="code-editor"></div>
</template>

<script setup lang="ts">
// 在线编辑器组件（论文 5.7）：封装 Monaco Editor，按语言切换语法模式并载入模板代码
// 先注册 worker 环境，再引入 monaco，避免 defaultWorkerFactory 抛错后回落到主线程
import '@/monacoWorker'
import * as monaco from 'monaco-editor'
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { MONACO_LANGUAGE } from '@/utils/status'

const props = defineProps<{
  modelValue: string
  language: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

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
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
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
  height: 420px;
  border-radius: 6px;
  overflow: hidden;
}
</style>
