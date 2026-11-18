package com.codeoj.server.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数归一化工具（review P2-22 / P2-24 / P2-25）。
 *
 * <p><b>为什么要在入口夹取</b>：MyBatis-Plus 的 {@code PaginationInnerInterceptor.maxLimit}
 * 只封上限，对 {@code page<=0}、{@code size<=0} 完全放行。{@code page=0} 时
 * {@code offset=(page-1)*size} 变成负数，Redis {@code reverseRangeWithScores(key,-20,-1)}
 * 会被解释成"从末尾数第 20 个开始"，返回的是一段与请求语义无关的区间；
 * {@code size=0} 则是 {@code offset+size-1 < offset}，静默返回空列表。
 * 两者都不报错，只是悄悄返回错误数据——比抛异常更难排查，所以必须在这里夹死。
 */
public final class PageUtils {

    /** 单页条数上限，与 {@code MybatisPlusConfig} 的 {@code maxLimit} 保持一致 */
    public static final long MAX_SIZE = 100L;

    private PageUtils() {
    }

    /** 页码归一：小于 1 一律视为第 1 页 */
    public static long page(long page) {
        return Math.max(1L, page);
    }

    /** 单页条数归一：夹在 [1, {@value #MAX_SIZE}] */
    public static long size(long size) {
        return Math.min(MAX_SIZE, Math.max(1L, size));
    }

    /** 直接构造一个已归一化的 Page，避免每个调用点重复写两行 Math.max/min */
    public static <T> Page<T> of(long page, long size) {
        return new Page<>(page(page), size(size));
    }
}
