/**
 * 浏览器端 JS 自测运行器（控制台功能）：
 * 用 Web Worker 沙箱真实执行用户 JS 代码——
 * 1. Worker 无 DOM 访问，天然与主页面隔离；
 * 2. 注入 mock：console.* 重定向捕获输出、require("fs").readFileSync(0) 返回自定义输入、
 *    process.exit 触发终止（题目提示的两种读入方式均可运行）；
 * 3. 超时 5 秒自动 terminate（防死循环卡页面）。
 * 仅适用于 JavaScript/Vue3 逻辑题；其他语言的运行依赖 A 侧 real 沙箱。
 */

export interface RunResult {
  /** console 输出（\n 分隔） */
  stdout: string
  /** 运行错误（语法错误/运行时异常/超时） */
  error?: string
  /** 耗时（毫秒） */
  elapsedMs: number
}

const WORKER_SOURCE = `
self.onmessage = (e) => {
  const { code, stdin, timeoutMs } = e.data;
  const out = [];
  const push = (args) => out.push(args.map(a => {
    if (typeof a === 'object' && a !== null) { try { return JSON.stringify(a); } catch (_) { return String(a); } }
    return String(a);
  }).join(' '));
  const fakeConsole = {
    log: (...a) => push(a),
    info: (...a) => push(a),
    warn: (...a) => push(a),
    error: (...a) => push(a),
    table: (...a) => push(a),
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
    exit: () => { throw { __exit: true }; },
    stdout: { write: (s) => { out.push(String(s).replace(/\\n$/, '')); return 1; } },
  };
  const timer = setTimeout(() => {
    out.push('── 超时：程序运行超过 ' + timeoutMs + 'ms 已被终止 ──');
    self.postMessage({ stdout: out.join('\\n'), error: 'TimeLimit', elapsedMs: timeoutMs });
    self.close();
  }, timeoutMs);
  const started = Date.now();
  try {
    const fn = new Function('console', 'require', 'process', code);
    fn(fakeConsole, fakeRequire, fakeProcess);
    clearTimeout(timer);
    self.postMessage({ stdout: out.join('\\n'), elapsedMs: Date.now() - started });
  } catch (err) {
    clearTimeout(timer);
    const isExit = err && err.__exit;
    self.postMessage({
      stdout: out.join('\\n'),
      error: isExit ? undefined : String((err && err.message) || err),
      elapsedMs: Date.now() - started,
    });
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
    const worker = new Worker(getWorkerUrl())
    const timer = setTimeout(() => {
      worker.terminate()
      resolve({ stdout: '', error: '运行超时（5 秒）', elapsedMs: 5000 })
    }, timeoutMs + 1000)
    worker.onmessage = (e) => {
      clearTimeout(timer)
      worker.terminate()
      resolve(e.data as RunResult)
    }
    worker.onerror = (e) => {
      clearTimeout(timer)
      worker.terminate()
      resolve({ stdout: '', error: e.message || 'Worker 异常', elapsedMs: 0 })
    }
    worker.postMessage({ code, stdin, timeoutMs })
  })
}
