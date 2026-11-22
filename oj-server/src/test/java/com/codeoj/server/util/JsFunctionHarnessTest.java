package com.codeoj.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JS 场景题包装器（函数实现题 → 标准 stdin/stdout 判题协议）。
 */
class JsFunctionHarnessTest {

    private static final String USER_CODE = "function solve(a, b) {\n  return a + b;\n}";

    @Test
    void wrapAppendsDeterministicHarness() {
        String wrapped = JsFunctionHarness.wrap(USER_CODE, "solve");
        // 结构：console 重定向前奏 → 用户代码（原样）→ 剥离标记 → 调用器
        assertTrue(wrapped.startsWith(";\nconsole.log"), "前奏（console 重定向）必须先于用户代码");
        assertTrue(wrapped.contains(JsFunctionHarness.PRELUDE_MARK + "\n" + USER_CODE), "用户代码必须原样保留在前奏之后");
        assertTrue(wrapped.contains(JsFunctionHarness.HARNESS_MARK), "必须带剥离标记");
        assertTrue(wrapped.contains("await solve(...__args)"), "必须调用约定的入口函数");
        assertTrue(wrapped.contains("JSON.parse(__raw"), "必须从 stdin 读参数列表");
        assertTrue(wrapped.contains("process.stderr.write"), "学生 console.* 必须重定向 stderr");
        assertTrue(wrapped.contains("process.stdout.write"), "调用器结果必须直写 stdout");
        // 幂等：同一输入两次包装产出一致（判题快照按内容哈希，不确定包装会破坏快照复用）
        assertEquals(wrapped, JsFunctionHarness.wrap(USER_CODE, "solve"));
    }

    @Test
    void stripRestoresUserCode() {
        String wrapped = JsFunctionHarness.wrap(USER_CODE + "\n", "solve");
        assertEquals(USER_CODE, JsFunctionHarness.strip(wrapped));
    }

    @Test
    void stripLeavesPlainCodeUntouched() {
        assertEquals(USER_CODE, JsFunctionHarness.strip(USER_CODE));
        assertEquals("", JsFunctionHarness.strip(""));
        assertEquals(null, JsFunctionHarness.strip(null));
    }

    @Test
    void wrapRejectsIllegalEntryName() {
        // 入口名会被拼进 JS 源码，非标识符一律拒绝（防注入）
        assertThrows(IllegalArgumentException.class, () -> JsFunctionHarness.wrap(USER_CODE, "solve();require('fs')"));
        assertThrows(IllegalArgumentException.class, () -> JsFunctionHarness.wrap(USER_CODE, "1abc"));
        assertThrows(IllegalArgumentException.class, () -> JsFunctionHarness.wrap(USER_CODE, "a b"));
    }

    @Test
    void wrapWithBlankEntryReturnsOriginal() {
        assertEquals(USER_CODE, JsFunctionHarness.wrap(USER_CODE, "  "));
        assertEquals(USER_CODE, JsFunctionHarness.wrap(USER_CODE, null));
        assertFalse(JsFunctionHarness.isWrapped(USER_CODE));
    }
}
