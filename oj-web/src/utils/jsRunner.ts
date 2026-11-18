/**
 * 浏览器端 JS 自测运行器（控制台功能）：
 * 用 Web Worker 沙箱真实执行用户 JS 代码——
 * 1. Worker 无 DOM 访问，天然与主页面隔离；
 * 2. 注入 mock：console.* 重定向捕获输出、require("fs").readFileSync(0) 返回自定义输入、
 *    process.exit 触发终止（题目提示的两种读入方式均可运行）；
 * 3. 超时 5 秒自动 terminate（防死循环卡页面）。
 * 仅适用于 JavaScript/Vue3 逻辑题；其他语言的运行依赖 A 侧 real 沙箱。
 *
 * 输出为什么逐行回传而不是最后一次 postMessage：
 * Worker 与主线程共用同一个事件循环线程模型，用户代码一旦同步死循环
 * （while(true){}），Worker 内 setTimeout 永远排不上，只能由主线程超时后 terminate。
 * 此时若把输出攒在 Worker 里，之前 print 的内容会全部丢失（P1-8）。
 * 因此每产生一行就 postMessage 一次，主线程增量累积——纵使被强杀也能保住已输出的部分。
 */

export interface RunResult {
  /** console 输出（\n 分隔） */
  stdout: string
  /** 运行错误（语法错误/运行时异常/超时） */
  error?: string
  /** 耗时（毫秒），主线程实测，同步阻塞场景下也准确 */
  elapsedMs: number
}

/** Worker → 主线程的消息协议 */
type WorkerMessage =
  | { type: 'line'; text: string }
  | { type: 'done'; ok: boolean; error?: string }

const WORKER_SOURCE = `
const push = (text) => self.postMessage({ type: 'line', text: String(text) });
self.onmessage = (e) => {
  const { code, stdin } = e.data;
  const fmt = (args) => args.map((a) => {
    if (typeof a === 'object' && a !== null) {
      try { return JSON.stringify(a); } catch (_) { return String(a); }
    }
    return String(a);
  }).join(' ');
  const fakeConsole = {
    log: (...a) => push(fmt(a)),
    info: (...a) => push(fmt(a)),
    warn: (...a) => push(fmt(a)),
    error: (...a) => push(fmt(a)),
    table: (...a) => push(fmt(a)),
    debug: (...a) => push(fmt(a)),
  };
  const fakeFs = {
    readFileSync: (fd) => (fd === 0 || fd === '/dev/stdin' ? stdin : ''),
  };
  const fakeRequire = (m) => {
    if (m === 'fs') return fakeFs;
    throw new Error('运行沙箱仅支持 require("fs")（读取标准输入）');
  };
  const fakeProcess = {
    argv: ['node', 'main.js'],
    exit: (code) => { throw { __exit: true, __code: code }; },
    stdout: { write: (s) => { push(String(s).replace(/\\n$/, '')); return 1; } },
    stderr: { write: (s) => { push(String(s).replace(/\\n$/, '')); return 1; } },
  };
  try {
    const fn = new Function('console', 'require', 'process', code);
    const ret = fn(fakeConsole, fakeRequire, fakeProcess);
    // 兼容"最后 return 一个值当输出"的写法
    if (ret !== undefined) push(fmt([ret]));
    self.postMessage({ type: 'done', ok: true });
  } catch (err) {
    if (err && err.__exit) {
      self.postMessage({ type: 'done', ok: true });
    } else {
      self.postMessage({ type: 'done', ok: false, error: String((err && err.message) || err) });
    }
  }
};
`

let workerUrl: string | undefined

function getWorkerUrl(): string {
  if (!workerUrl) {
    workerUrl = URL.createObjectURL(new Blob([WORKER_SOURCE], { type: 'application/javascript' }))
  }
  return workerUrl
}

/** 在 Web Worker 沙箱中运行用户 JS 代码 */
export function runInSandbox(code: string, stdin: string, timeoutMs = 5000): Promise<RunResult> {
  return new Promise((resolve) => {
    const lines: string[] = []
    const started = performance.now()
    let settled = false
    const worker = new Worker(getWorkerUrl())

    const finish = (result: Omit<RunResult, 'stdout' | 'elapsedMs'>) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      worker.terminate()
      const elapsedMs = Math.round(performance.now() - started)
      resolve({ stdout: lines.join('\n'), elapsedMs, ...result })
    }

    // 比 Worker 内部的软超时再宽 300ms，留出正常收尾的时间；
    // 真正兜底的是这里的 terminate（同步死循环只有它能打断）。
    const timer = setTimeout(() => {
      lines.push(`── 超时：程序运行超过 ${timeoutMs}ms 已被终止，以上为已产生的输出 ──`)
      finish({ error: 'TimeLimit' })
    }, timeoutMs + 300)

    worker.onmessage = (e: MessageEvent<WorkerMessage>) => {
      const msg = e.data
      if (msg.type === 'line') {
        lines.push(msg.text)
      } else if (msg.type === 'done') {
        finish(msg.ok ? {} : { error: msg.error || '运行异常' })
      }
    }

    worker.onerror = (e) => {
      finish({ error: e.message || 'Worker 异常' })
    }

    worker.postMessage({ code, stdin })
  })
}
