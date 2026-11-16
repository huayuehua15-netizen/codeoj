package com.codeoj.server.dto;

import com.codeoj.common.entity.Problem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 题目创建/编辑请求（论文表 4-2）。
 */
@Data
public class ProblemFormDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    /** 题目描述（Markdown） */
    private String description;

    /** 标签（动态规划/贪心/模拟…，可自定义） */
    private List<String> tags;

    /** 样例输入输出数组 */
    private List<Problem.Sample> samples;

    /** 1=简单，2=中等，3=困难 */
    @NotNull @Min(1) @Max(3)
    private Integer difficulty = 1;

    /** 时间限制（毫秒），默认 1000 */
    @NotNull @Min(1) @Max(60000)
    private Integer timeLimitMs = 1000;

    /** 内存限制（MB），默认 256 */
    @NotNull @Min(16) @Max(1024)
    private Integer memoryLimitMb = 256;

    /** 允许提交的语言数组 */
    @NotEmpty
    private List<@Pattern(regexp = "c|cpp|java|python3|js") String> languages;

    /** 是否上架 */
    private Boolean enabled = true;
}
