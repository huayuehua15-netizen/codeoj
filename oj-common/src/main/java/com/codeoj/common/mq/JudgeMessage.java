package com.codeoj.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 判题任务消息（论文 4.6）：消息体只携带 submissionId，
 * 代码与测试用例由消费者回查数据库获取，既控制消息体积，又天然保证数据一致。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeMessage {

    private Long submissionId;
}
