# 评测修复与共同作者代码兼容登记

2026-11-16，当前仅本地修改、待评审。个人分支 `feat/judge-hardening` 基于最新集成分支 `72a9c2e82eb5610b810f2c739b206be57e23d9ef`，包含 main `058b7c7ff90a7ec89396f9db3eead48ab53e05cf`。未合并或推送 dev/main，B 的所有提交及作者记录保留。

## 保留的 B 侧能力

完整保留最新统计页、排行榜页、竞赛页、Monaco worker/拆包、轻量题目列表与编辑详情、权限修复、数字状态、时间序列化、MQ confirm/return 回调、新统计字段和三组复合索引。上游自旧基线变动的 58 个文件，47 个保持与 dev 相同的 Git blob；其余 11 个重叠文件（包括补充登记的 API 文档）逐项合并。B 的 HANDOFF 上手章节与 PROGRESS 历史记录完整保留，A 实现移入 JUDGE-HARDENING.md。

## 跨边界修改清单

| 范围 | 必要改动与兼容处理 |
| --- | --- |
| oj-common Submission / SubmissionLeaseOperations | 增加租约字段、两端共享状态 SQL；无修改其他公共实体和枚举 |
| server SubmissionServiceImpl | 保留 B TransactionTemplate 构造器、事务和 dev 全部统计汇总，只在真实提交后先 QUEUED 再派发；初始状态仍数字 0 |
| server SubmissionMapper / JudgeDispatcher | 保留 dev 新统计方法；仅旧领取/写回 SQL 改继承共享接口。发送失败由持久记录补发，保留 B RabbitMqConfig 全部回调 |
| server WebSocketConfig / 新拦截器 | 保留 B LoginUser Principal/getName=userId、小写鉴权头兼容；委托单个拦截器，增加黑名单/用户状态和目标限制 |
| web src/api/ws.ts | 保留 B JSON 与 handler 异常隔离、disconnect 不清 handlers，仅在每次 beforeConnect 刷新 token |
| server ProblemFormDTO | 只增加时间/内存/语言合法范围校验，字段名称和有效请求格式不变 |
| SQL | B 三个新索引与 A 两个索引取并集；增加独立租约迁移，未删除字段/表/业务数据 |
| 部署 | MQ 自定义凭据两端一致；前端 Node22/npm ci/.dockerignore；服务 JVM 显式 Shanghai，与 B 业务查询/展示一致 |
| 文档 | 保留 API v1.3/CONTRACT v1.1 历史，分别递增 v1.4/v1.2；A/B 交接分文件说明 |

## 时钟兼容修复

B 业务字段 created_at/judged_at 使用 Asia/Shanghai 本地 DATETIME，序列化保持 `yyyy-MM-dd HH:mm:ss`。createQueued 显式上海时间，judged_at 使用上海数据库会话 NOW()，服务镜像 JVM 固定上海时区以维持竞赛状态和按日统计。

A 内部 judge_lease_until/next_dispatch_at 使用 UTC 数据库时钟。`next_dispatch_at` 必须 `DEFAULT (UTC_TIMESTAMP(6))`，若沿用 CURRENT_TIMESTAMP，上海会话会令新任务错误延迟八小时。新库、租约迁移均已修正，并在真实 MySQL +08:00 会话验证。

已有旧库须停全部旧 server/judge 后执行租约迁移一次，并按 B 的 `sql/migration-20261116-indexes.sql` 补齐缺失索引。若曾执行 A 早期租约迁移而已存在四字段，使用 `sql/migrations/20261116_dispatch_utc_default.sql` 修正默认值，不能重复 ADD COLUMN。历史业务时间不自动整体加/减八小时，须依据原部署时区确认。

## 回归与协作边界

最新代码上 Java 61 项、前端 TypeScript/Vite 构建通过；五语言真实接口 AC、WS 三阶段/用户隔离通过；11 项真实 MySQL 时钟/租约/索引检查通过；8 项 B 业务接口检查通过（题目列表/详情、统计维度、榜单新字段、北京时间、今日趋势、竞赛窗口、匿名收藏）。完整证据另见本次兼容复核交付报告。

这些跨模块修复应随个人分支交给 B 一并评审。遵守 COLLABORATION.md：仅 B 合并 dev/main；不能直接用旧版整文件覆盖。租约协议需要同版 server/judge 与数据库升级配套，不能混跑旧 judge。没有宣称所有浏览器流程、完整安全套件和大并发性能已验收。
