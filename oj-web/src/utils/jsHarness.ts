/**
 * JS 场景题（函数实现题）协议的前端实现。
 *
 * 约定（与 oj-server 的 JsFunctionHarness.java 保持一致，改动需两侧同步）：
 * - 题目 entryFunction 非空 = 函数题：用户实现具名函数，用例 input 为 JSON 参数数组
 *   （如 [1,2]），expectedOutput 为期望返回值的字符串形式（字符串原样、其余 JSON）；
 * - 判题代码 = 用户代码 + 平台调用器（stdin 读 JSON 参数数组 → 逐组调用入口函数 →
 *   stdout 逐行输出返回值），由 oj-server 在提交入库时拼接，judge 无感知；
 * - 本文件只负责浏览器侧两件事：① "运行"按钮在 Worker 沙箱里用同一协议自测；
 *   ② 函数题的编辑器初始模板。
 */

export const JS_HARNESS_MARK = '/*__OJ_HARNESS__*/'

/** 入口函数名白名单：与后端 ENTRY_PATTERN 一致 */
const ENTRY_PATTERN = /^[A-Za-z_$][A-Za-z0-9_$]*$/

/** 函数题编辑器初始模板 */
export function functionTemplate(entry: string): string {
  return `function ${entry}() {
  // 在此实现，返回结果（参数说明见题面与样例）
}
`
}

/** 包装为可直接执行的 Node 脚本（判题协议）；entry 非法时抛错 */
export function wrapJsFunction(userCode: string, entry: string): string {
  if (!ENTRY_PATTERN.test(entry)) {
    throw new Error(`入口函数名必须是合法 JS 标识符：${entry}`)
  }
  return `${userCode.replace(/\s+$/, '')}\n\n${JS_HARNESS_MARK}\n${buildHarness(entry)}`
}

/** 与 oj-server JsFunctionHarness.buildHarness 保持逐行一致 */
function buildHarness(entry: string): string {
  return `(async () => {
  const __raw = require("fs").readFileSync(0, "utf8");
  const __cases = JSON.parse(__raw || "[]");
  const __out = [];
  for (const __args of __cases) {
    let __r;
    try {
      __r = await ${entry}(...__args);
    } catch (__e) {
      __out.push("RuntimeError: " + ((__e && __e.message) || __e));
      continue;
    }
    __out.push(typeof __r === "string" ? __r : (__r === undefined ? "undefined" : JSON.stringify(__r)));
  }
  console.log(__out.join("\\n"));
})();
`
}

/**
 * 函数题"运行"自测：把控制台的单组参数 JSON（如 `[1,2]`）包成调用器输入并执行用户代码。
 * 输入本身已是数组时按多组处理（`[[1,2],[3,4]]`）。
 */
export function functionRunInput(rawInput: string): string {
  const trimmed = (rawInput ?? '').trim()
  if (!trimmed) return '[]'
  if (trimmed.startsWith('[[')) return trimmed
  return JSON.stringify([JSON.parse(trimmed)])
}
