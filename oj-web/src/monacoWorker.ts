/**
 * Monaco Worker 环境配置（论文 5.7）。
 *
 * monaco-editor 的 ESM 构建在需要语言服务（补全/诊断/折叠）时会调用
 * MonacoEnvironment.getWorker()；未定义时 defaultWorkerFactory 会直接抛错，
 * 并回落到主线程同步执行（控制台出现 "Could not create web worker(s)" 警告 + UI 卡顿）。
 * 这里用 Vite 的 `?worker` 导入，由打包器生成独立 worker chunk。
 *
 * 注意：本项目只用到 c/cpp/java/python/js 的纯语法高亮（Monarch，跑在主线程），
 * 仍注册 editorWorker 以兜住编辑器通用能力。
 */
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import CssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import HtmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

const monacoEnvironment = {
  getWorker(_workerId: string, label: string) {
    if (label === 'json') return new JsonWorker()
    if (label === 'css' || label === 'scss' || label === 'less') return new CssWorker()
    if (label === 'html' || label === 'handlebars' || label === 'razor') return new HtmlWorker()
    if (label === 'typescript' || label === 'javascript') return new TsWorker()
    return new EditorWorker()
  },
}

;(globalThis as unknown as { MonacoEnvironment: typeof monacoEnvironment }).MonacoEnvironment =
  monacoEnvironment
