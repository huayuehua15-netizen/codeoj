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

/** console 重定向前奏的结束标记：剥离时连同前奏一起还原用户原始代码 */
export const JS_PRELUDE_MARK = '/*__OJ_PRE__*/'

/** 入口函数名白名单：与后端 ENTRY_PATTERN 一致 */
const ENTRY_PATTERN = /^[A-Za-z_$][A-Za-z0-9_$]*$/

/** 函数题编辑器初始模板 */
export function functionTemplate(entry: string): string {
  return `function ${entry}() {
  // 在此实现，返回结果（参数说明见题面与样例）
}
`
}

/**
 * 包装为可直接执行的 Node 脚本（判题协议）；entry 非法时抛错。
 * 结构：console 重定向前奏 + 用户代码 + 调用器——前奏必须在前，
 * 学生顶层（函数外）的 console.log 调试语句也要被管住。
 */
export function wrapJsFunction(userCode: string, entry: string): string {
  if (!ENTRY_PATTERN.test(entry)) {
    throw new Error(`入口函数名必须是合法 JS 标识符：${entry}`)
  }
  return `;${CONSOLE_REDIRECT}\n${JS_PRELUDE_MARK}\n${userCode.replace(/\s+$/, '')}\n\n${JS_HARNESS_MARK}\n${buildHarness(entry)}`
}

/** 与 oj-server 的 CONSOLE_REDIRECT 保持一致 */
const CONSOLE_REDIRECT =
  'console.log = console.info = console.debug = console.table = console.trace = (...__a) => process.stderr.write(__a.map(String).join(" ") + "\\n");'

/** 与 oj-server JsFunctionHarness.buildHarness 保持逐行一致：单条用例单次调用。
 *  防御点（边界矩阵实测）：调用器开头 ; 防 ASI、console 前奏管顶层语句、
 *  最终输出走 process.stdout.write 直写、错误消息截首行。 */
function buildHarness(entry: string): string {
  return `;
(async () => {
  const __raw = require("fs").readFileSync(0, "utf8");
  const __args = JSON.parse(__raw || "[]");
  let __r;
  try {
    __r = await ${entry}(...__args);
  } catch (__e) {
    process.stdout.write("RuntimeError: " + firstLine((__e && __e.message) || __e) + "\\n");
    return;
  }
  let __s;
  try {
    __s = typeof __r === "string" ? __r : (__r === undefined ? "undefined" : JSON.stringify(__r));
  } catch (__e) {
    __s = "RuntimeError: " + firstLine((__e && __e.message) || __e);
  }
  process.stdout.write(__s + "\\n");
})();
function firstLine(v) { return String(v).split("\\n")[0]; }
`
}

/** 剥离前奏与调用器，还原学生原始代码（前端"运行"展示用） */
export function stripJsHarness(stored: string): string {
  if (!stored || !stored.includes(JS_HARNESS_MARK)) return stored
  let userPart = stored.slice(0, stored.indexOf(JS_HARNESS_MARK))
  const preludeEnd = userPart.indexOf(JS_PRELUDE_MARK)
  if (preludeEnd >= 0) userPart = userPart.slice(preludeEnd + JS_PRELUDE_MARK.length)
  return userPart.trim()
}

/**
 * 函数题"运行"自测输入：stdin 即本条用例的 JSON 参数数组。
 * 用户没填参数时给空数组（调用无参函数也能跑）。
 */
export function functionRunInput(rawInput: string): string {
  const trimmed = (rawInput ?? '').trim()
  return trimmed || '[]'
}
