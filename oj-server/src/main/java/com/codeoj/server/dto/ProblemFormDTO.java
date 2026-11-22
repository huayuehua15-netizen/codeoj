package com.codeoj.server.dto;

import com.codeoj.common.entity.Problem;
import com.codeoj.common.enums.Language;
import jakarta.validation.constraints.AssertTrue;
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

    /**
     * JS 场景题（函数实现题）入口函数名，如 "solve"。
     * 留空 = 标准 stdin/stdout 题（既有题目不受影响）；填了则判题走"平台调用器"协议，
     * 测试用例 input 须为 JSON 参数数组（如 [1,2]），expectedOutput 为期望返回值。
     */
    @Size(max = 64, message = "入口函数名不得超过 64 字符")
    @Pattern(regexp = "^[A-Za-z_$][A-Za-z0-9_$]*$", message = "入口函数名必须是合法 JS 标识符")
    private String entryFunction;

    /** 是否上架 */
    private Boolean enabled = true;

    @AssertTrue(message = "Java 和 JavaScript 题目的内存限制不得低于 64 MB")
    public boolean isLanguageMemoryValid() {
        if (memoryLimitMb == null || languages == null) return true;
        for (Language language : Language.values()) {
            if (languages.contains(language.getCode()) && memoryLimitMb < language.getMinimumMemoryMb()) return false;
        }
        return true;
    }
}
