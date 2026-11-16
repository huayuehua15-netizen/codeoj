package com.codeoj.server.vo;

import com.codeoj.common.entity.Problem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目列表项（docs/API.md §2.1）。
 *
 * <p>列表接口<b>不返回</b> description/samples/languages 大字段：
 * description 是 LONGTEXT、samples 是 JSON，一页 10 条就会把整份题面
 * （几十 KB）全塞进响应体。早期实现直接返回 {@code Problem} 实体，
 * 让列表接口的体积和详情接口一个量级，且与文档契约不符
 * （管理端编辑表单也因此依赖了列表接口的"超纲"行为，属隐患）。
 */
@Data
public class ProblemListItemVO {

    private Long id;
    private String title;
    private List<String> tags;
    private Integer difficulty;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Integer submitCount;
    private Integer acceptedCount;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProblemListItemVO of(Problem p) {
        ProblemListItemVO vo = new ProblemListItemVO();
        vo.setId(p.getId());
        vo.setTitle(p.getTitle());
        vo.setTags(p.getTags());
        vo.setDifficulty(p.getDifficulty());
        vo.setTimeLimitMs(p.getTimeLimitMs());
        vo.setMemoryLimitMb(p.getMemoryLimitMb());
        vo.setSubmitCount(p.getSubmitCount());
        vo.setAcceptedCount(p.getAcceptedCount());
        vo.setEnabled(p.getEnabled());
        vo.setCreatedAt(p.getCreatedAt());
        vo.setUpdatedAt(p.getUpdatedAt());
        return vo;
    }
}
