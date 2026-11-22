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
        assertTrue(wrapped.startsWith(USER_CODE), "用户代码必须在最前且原样保留");
        assertTrue(wrapped.contains(JsFunctionHarness.HARNESS_MARK), "必须带剥离标记");
        assertTrue(wrapped.contains("await solve(...__args)"), "必须调用约定的入口函数");
        assertTrue(wrapped.contains("JSON.parse(__raw"), "必须从 stdin 读 JSON 参数数组");
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
