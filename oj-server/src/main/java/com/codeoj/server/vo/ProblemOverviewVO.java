package com.codeoj.server.vo;

import java.util.List;

/** 上架题库的概览，不受当前分页和筛选影响。 */
public record ProblemOverviewVO(long totalProblems, List<String> tags) { }
