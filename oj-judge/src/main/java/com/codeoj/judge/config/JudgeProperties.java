package com.codeoj.judge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 判题沙箱配置（论文表 4-9 / 5.4 docker run 参数）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "oj.judge")
public class JudgeProperties {

    /** 实例内信号量：并行运行的容器数上限（论文 4.6，默认 4） */
    private int maxParallelContainers = 4;

    /** 沙箱宿主机工作目录根（按提交编号隔离） */
    private String workDirRoot = "/judge/work";

    /** runner 脚本容器内路径 */
    private String runnerPath = "/judge/runner.sh";

    /** 容器运行用户（论文表 4-9：-u 1000:1000） */
    private String containerUser = "1000:1000";

    /** 宿主看护：容器总时长超过上限直接 docker kill（双重超时控制） */
    private long containerHardTimeoutMs = 15000;

    /** 输出上限 16MB（论文 5.4：输出超过 16MB 判运行错误） */
    private long outputLimitBytes = 16777216L;

    /** tmpfs /tmp 大小 */
    private String tmpfsSize = "64m";
}
