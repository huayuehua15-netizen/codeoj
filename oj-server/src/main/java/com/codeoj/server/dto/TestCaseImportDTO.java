package com.codeoj.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 测试用例批量导入请求（论文表 4-7 / 5.7：管理端批量导入，文本粘贴解析）。
 */
@Data
public class TestCaseImportDTO {

    @Valid
    @NotNull
    @Size(min = 1, max = 200, message = "单次导入 1-200 组用例")
    private List<@NotNull Item> cases;

    /**
     * 单组用例。input/expectedOutput 均允许空串（无输入题目、空输出题目是合法场景）。
     */
    @Data
    public static class Item {

        @NotNull
        private String input;

        @NotNull
        private String expectedOutput;

        /** 分值（计分制），默认 0 */
        @NotNull
        @Min(0)
        private Integer score;

        /** 是否样例（样例可在题面展示） */
        @NotNull
        private Boolean isSample;
    }
}
