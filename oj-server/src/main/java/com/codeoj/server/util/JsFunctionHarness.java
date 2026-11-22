package com.codeoj.server.util;

/**
 * JS 场景题（函数实现题）判题代码包装器。
 *
 * <p>约定：题目的 entry_function 非空时，用户实现一个具名函数（如 {@code function solve(a,b)}），
 * 测试用例的 input 存 JSON 参数数组（如 {@code [1,2]}），expectedOutput 存期望返回值的字符串形式
 * （字符串原样、数组/对象 JSON.stringify、布尔 true/false）。
 *
 * <p>判题链路对 judge 完全透明：本类在 oj-server 入库时把"用户代码 + 平台调用器"拼成
 * 可直接执行的 Node 脚本（stdin 读 JSON 参数数组 → 逐组调用入口函数 → stdout 输出返回值），
 * judge 读到的 code 与普通提交无异；判题结果页展示用户代码时按 {@link #HARNESS_MARK} 剥离调用器。
 *
 * <p>前端浏览器沙箱（oj-web/src/utils/jsHarness.ts）以同一协议实现"运行"按钮，
 * 两份实现需保持一致，改动时同步。
 */
public final class JsFunctionHarness {

    /** 调用器起始标记：详情页按它剥离平台注入的调用器部分 */
    public static final String HARNESS_MARK = "/*__OJ_HARNESS__*/";

    /** 入口函数名白名单：标识符字符集，防止把任意代码片段注入用户源码 */
    private static final String ENTRY_PATTERN = "[A-Za-z_$][A-Za-z0-9_$]*";

    private JsFunctionHarness() {
    }

    public static boolean isWrapped(String code) {
        return code != null && code.contains(HARNESS_MARK);
    }

    /** 拼接"用户代码 + 调用器"作为判题代码（entryFunction 空/空白时原样返回） */
    public static String wrap(String userCode, String entryFunction) {
        if (entryFunction == null || entryFunction.isBlank()) return userCode;
        String entry = entryFunction.strip();
        if (!entry.matches(ENTRY_PATTERN)) {
            throw new IllegalArgumentException("入口函数名必须是合法 JS 标识符: " + entryFunction);
        }
        return userCode.stripTrailing() + "\n\n" + HARNESS_MARK + "\n" + buildHarness(entry);
    }

    /**
     * 从入库代码剥离调用器，还原用户原始代码（结果页展示用）。
     * 无标记（标准题/历史数据）原样返回。
     */
    public static String strip(String storedCode) {
        if (!isWrapped(storedCode)) return storedCode;
        int idx = storedCode.indexOf(HARNESS_MARK);
        return storedCode.substring(0, idx).stripTrailing();
    }

    /**
     * 调用器：stdin 读**本条用例**的 JSON 参数数组 → 调用一次入口函数 → stdout 输出返回值。
     * judge 的判题模型是"每条用例独立运行一次程序"（input 进 stdin、stdout 比对 expected），
     * 所以这里是单次调用而非多组循环。字符串返回值原样输出，其余 JSON 序列化
     * （与用例 expectedOutput 的存储约定一致）；入口函数抛异常时输出
     * "RuntimeError: ..." 行（比对不通过判 WA，错误信息可见）。
     */
    private static String buildHarness(String entry) {
        return "(async () => {\n"
                + "  const __raw = require(\"fs\").readFileSync(0, \"utf8\");\n"
                + "  const __args = JSON.parse(__raw || \"[]\");\n"
                + "  let __r;\n"
                + "  try {\n"
                + "    __r = await " + entry + "(...__args);\n"
                + "  } catch (__e) {\n"
                + "    console.log(\"RuntimeError: \" + ((__e && __e.message) || __e));\n"
                + "    return;\n"
                + "  }\n"
                + "  console.log(typeof __r === \"string\" ? __r : (__r === undefined ? \"undefined\" : JSON.stringify(__r)));\n"
                + "})();\n";
    }
}
