package com.codeoj.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分页参数夹取边界（review P2-22 / P2-24 / P2-25）。
 *
 * <p>这些断言存在的意义是：夹取一旦失效不会抛异常，只会悄悄返回错误数据，
 * 所以必须由测试把边界钉死。
 */
class PageUtilsTest {

    @Test void pageBelowOneFallsBackToFirstPage() {
        assertEquals(1L, PageUtils.page(0));
        assertEquals(1L, PageUtils.page(-1));
        assertEquals(1L, PageUtils.page(Long.MIN_VALUE));
        assertEquals(7L, PageUtils.page(7));
    }

    @Test void sizeIsClampedToOneAndHundred() {
        assertEquals(1L, PageUtils.size(0));
        assertEquals(1L, PageUtils.size(-20));
        assertEquals(100L, PageUtils.size(101));
        assertEquals(100L, PageUtils.size(Long.MAX_VALUE));
        assertEquals(20L, PageUtils.size(20));
    }

    @Test void ofBuildsClampedPageAndNeverProducesNegativeOffset() {
        var page = PageUtils.of(0, 0);
        assertEquals(1L, page.getCurrent());
        assertEquals(1L, page.getSize());
        // offset=(current-1)*size 必须非负，否则 Redis 分页会被解释成倒数区间
        assertEquals(0L, (page.getCurrent() - 1) * page.getSize());

        var huge = PageUtils.of(-5, 1000);
        assertEquals(1L, huge.getCurrent());
        assertEquals(100L, huge.getSize());
    }
}
