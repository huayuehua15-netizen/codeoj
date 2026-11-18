# CodeOJ 模块对接契约

> 版本：v1.3 ｜ 更新日期：2026-11-18
> 适用当前 A 评测链路修复分支。oj-server 与 oj-judge 通过数据库、RabbitMQ、Redis 协作，共享实体和 SQL 契约由 oj-common 提供。部署升级步骤见第 8 节。

## 1. MQ 协议

MQ 名称、提交编号消息体、结果事件字段与 v1.0 保持兼容。

### 1.1 判题任务 oj-server → oj-judge

| 组件 | 值 | 配置 |
|---|---|---|
| Exchange | `oj.judge.exchange` | direct、durable |
| Routing Key | `judge.submit` | — |
| Queue | `oj.judge.queue` | durable、绑定死信交换机 |
| DLX / DLQ | `oj.judge.dlx` / `oj.judge.dlq` | routing key 为 `judge.dead` |
| 消息体 | 提交 ID 的 UTF-8 纯文本，如 `100` | 不携带代码和测试用例 |
| Delivery mode | PERSISTENT | 发送原始 Message，绕过全局 JSON converter |

消费端兼容历史 JSON 引号字符串，如 `"100"`；非法编号直接 NACK、不重投、进入死信。手动 ACK、prefetch=1 保持不变。并发从 `spring.rabbitmq.listener.simple.concurrency` / `JUDGE_CONCURRENCY` 读取，默认 4，监听器不再用注解固定为 4。

生产端在提交事务提交后先尽力推送 QUEUED，再发送判题消息。两端发布任务均启用 publisher confirm 和 mandatory return。数据库只记录经 broker 确认且有路由的投递，已确认但仍在排队的任务不因等待时间长而重复发送。确认丢失时，发送窗口到期后允许重试，消费者继续用租约防止重复判题。

新提交在接受事务内绑定题目资源限制和当时按 ID 排序的隐藏用例快照，后续重试使用同一份数据。旧提交缺少快照时兼容读取现有题目和用例。Docker 临时调度、连接或日志读取错误进入有上限的延迟重试；缺失镜像等配置错误返回 SYSTEM_ERROR。

### 1.2 判题事件 oj-judge → oj-server

| 组件 | 值 |
|---|---|
| Exchange | `oj.event.exchange`，direct、durable |
| Routing Key | `event.judge.finished` |
| Queue | `oj.event.judge.queue`，durable |
| 序列化 | JSON，两端使用 `judgeEventMessageConverter` |
| 事件类型 | `JudgePushEvent`，status 是共享枚举名称 |

完成事件示例：

```json
{
  "submissionId": 100,
  "userId": 1,
  "problemId": 1,
  "status": "ACCEPTED",
  "timeUsedMs": 34,
  "memoryUsedKb": 3200,
  "passedCases": 4,
  "totalCases": 4,
  "errorInfo": null
}
```

JUDGING 阶段也使用该事件队列。oj-server 将枚举转换为前端数字状态码，再通过 `convertAndSendToUser(userId, "/queue/judge", ...)` 发送 `JUDGING` 或 `JUDGE_FINISHED` 信封。QUEUED 由提交入口推送。

实时推送和排行榜投影属于数据库提交后的尽力送达副作用，其异常不能中断判题或重判已提交终态。前端断线、漏消息时通过 `GET /api/submissions/{id}` 兜底，数据库是判题结果的权威来源。WebSocket 鉴权及前端信封结构见 API.md。

## 2. 判题状态机（论文表 4-8）

共享枚举为 `oj-common/enums/SubmissionStatus`，两端不得定义不同状态值。

| 状态 | code | 含义 |
|---|---|---|
| QUEUING | 0 | 已接受，等待领取 |
| JUDGING | 1 | 当前持有执行租约 |
| ACCEPTED | 2 | 全部有效用例运行成功且输出一致 |
| WRONG_ANSWER | 3 | 第一个输出不匹配的用例，Fail-Fast |
| TIME_LIMIT_EXCEEDED | 4 | 用户程序墙钟时间超限或运行阶段超时看护触发 |
| MEMORY_LIMIT_EXCEEDED | 5 | 运行阶段 cgroup OOM 或测得峰值内存超限 |
| RUNTIME_ERROR | 6 | 非零程序退出码、运行异常或输出超限 |
| COMPILE_ERROR | 7 | 编译失败、编译超时或编译内存超限 |
| SYSTEM_ERROR | 8 | 沙箱/统计协议故障、无有效测试用例、无效配置或评测服务重试耗尽 |

正常流转为 `0 → 1 → 终态`。恢复允许当前所有者释放 `1 → 0`，或扫描器仅将过期租约 `1 → 0`；终态不可被新消息覆盖。

`timeUsedMs` / `time_used_ms` 表示程序墙钟毫秒，提交结果取已执行用例最大值；不含容器创建和启动耗时。runner 的 `cpu_ms` 是诊断值，不作为题目时间或 TLE 判定依据。内存单位为 KB，取进程峰值 RSS；语言容器额外内存余量用于运行时开销，不能据此提高题目内存判定阈值。基础设施故障不得假判 AC。

## 3. 租约与事务幂等

### 3.1 共享 SQL

两端 `SubmissionMapper` 均继承 `oj-common/persistence/SubmissionLeaseOperations`，租约与终态 SQL 只在共享接口维护。

submission 新增字段：

| 字段 | 用途 |
|---|---|
| `judge_token` | 每次领取生成的 UUID，标识本次执行所有者 |
| `judge_lease_until` | 数据库 UTC 时钟计算的到期时间，DATETIME(6) |
| `judge_attempts` | 累计领取次数，恢复不会清零 |
| `next_dispatch_at` | QUEUING 记录下次可补发时间 |
| `dispatch_token` | 只允许当前发送窗口保存 broker 确认 |
| `dispatched_at` | 当前入队经 broker 确认的 UTC 时间，空值允许到期补发 |
| `judge_snapshot_key` | 指向内部判题快照，旧提交为空 |
| `total_cases` | 该次判题计划执行的总用例数，与实际明细行数分开 |

领取仅允许 status=0，原子设置 status=1、token、到期时间并增加 attempts。默认租期 120 秒，独立线程约每 40 秒续租；续租必须同时匹配 status=1、token 和未过期租约。数据库使用 `UTC_TIMESTAMP(6)`，不以各实例本机时钟判断所有权。

### 3.2 写回与失败释放

终态更新必须同时满足：

```sql
WHERE id = :id AND status = 1
  AND judge_token = :token
  AND judge_lease_until > UTC_TIMESTAMP(6)
```

同一事务完成终态更新、judge_case 明细插入和 problem 冗余统计更新。条件更新为 0 时，禁止插入明细、增加统计、投影排行榜或推送该次执行结果。事务异常应整体回滚。

失败释放也必须匹配当前 token；旧 worker 不能释放新 worker 的租约。消费者为 `JudgeTask.executionToken` 设置本次 token，文件和容器也必须按执行尝试隔离。仅有 `WHERE status=1` 不足以证明任务所有权。

## 4. 可靠投递与恢复

submission 的 QUEUING 行本身就是持久待发送记录，无需依赖进程内重试列表。

1. `SubmissionServiceImpl.submit` 在真实事务中保存 QUEUING，事务回滚不推送、不发送 MQ。
2. afterCommit 先尝试 QUEUED 推送，再将 `JudgeDispatcher.publish` 交给有容量限制的独立线程池。首发在独立事务中预留窗口和等待确认，HTTP 提交线程无需等待 broker。当线程池繁忙时，已保存的 QUEUING 记录由周期扫描接手。
3. 启动及周期恢复扫描先恢复过期租约，再查找尚未经 broker 确认且已到期的 QUEUING 记录，按 `next_dispatch_at, id` 排序，每轮最多 200 条。
4. 发送前通过条件更新预留 120 秒窗口并生成发送 token。broker 确认且未退回时写入 `dispatched_at`。确认超时或发送进程中断后，窗口到期允许重试。消息被明确拒绝或退回后等待 15 秒。
5. 默认扫描间隔 15 秒，可用 `oj.judge.recovery-interval-ms` 配置。普通 broker 重启依靠持久化消息保留已确认任务。若发生队列被清除或数据卷丢失，恢复 broker 后对受影响的已确认排队记录清除 `dispatched_at` 并设为到期重新投递。领取和 token 校验仍能处理不确定确认产生的重复消息。

恢复扫描只能重置 `judge_lease_until <= UTC_TIMESTAMP(6)` 的 JUDGING；兼容迁移后的无租约历史 JUDGING。新增或重启一个实例不能重置其他实例的健康租约。

首次处理异常时，消费者先按 token 释放领取，再 `basicNack(requeue=true)`；重投仍失败时，尝试按当前租约写 SYSTEM_ERROR，再 `basicNack(requeue=false)` 进入死信。若恢复写库也失败，保留租约等待过期扫描，不凭一次失败就删除数据库记录。重复宕机默认最多执行 3 次，超过该领取次数后直接终结 SYSTEM_ERROR；参数为 `oj.judge.max-attempts`。

Docker 临时故障走数据库延迟重试：按租约把任务退回 QUEUING，延迟以 5 秒为起点，保存成功后 ACK 原消息。达到领取次数上限后写入 SYSTEM_ERROR。程序自身的错误及已确认的环境配置错误不走该重试。

| 故障 | 恢复机制 |
|---|---|
| 入库后未成功发 MQ | 到期 QUEUING 周期补发 |
| 已领取但 worker 宕机 | 租约过期后恢复并补发 |
| 重复消息 | 原子领取失败时 ACK；有权执行者或周期恢复继续负责 |
| 旧 worker 延迟返回 | token 和租期条件拒绝写回 |
| Redis 或推送失败 | 保留数据库终态；排行榜对账、前端查询兜底 |
| 持续处理异常 | SYSTEM_ERROR 与死信保留排查线索 |

## 5. Redis 协议与排行榜

| Key | 类型 | TTL | 写方 | 读方 | 含义 |
|---|---|---|---|---|---|
| `oj:leaderboard:ac` | ZSET | 永久 | oj-judge 投影；oj-server 对账 | oj-server | member=userId 裸字符串，score=去重通过题数 |
| `oj:stats:problem:{id}` | HASH | 5 min | oj-server | oj-server | 热点题目统计缓存 |
| `oj:token:blacklist:{jti}` | STRING | token 剩余时长 | oj-server | oj-server | 令牌黑名单 |
| `oj:rate:{type}:{key}` | STRING | 窗口 | oj-server | oj-server | 限流 |
| `oj:lock:submit:{userId}:{problemId}` | STRING NX | 2 s | oj-server | — | 防短时间重复提交 |
| `oj:queue:backlog` | STRING | 30 s | oj-server | oj-server | 队列积压缓存 |
| `oj:judge:instance:{uuid}` | STRING | 30 s | oj-judge | oj-server | 约每 20 秒续写的实例心跳 |

键名沿用 `RedisKeys`。排行榜两端统一 `StringRedisTemplate`，禁止把 userId 用 JSON 编成带引号的另一个 member。

AC 数据库事务提交后，judge 查询该用户 `COUNT(DISTINCT problem_id)`，用 Lua 原子投影较大分数；同一题多次 AC 不增加解题数，较早读取的低分不能覆盖较晚更新。禁止每条 AC 提交直接 ZINCRBY。problem.accepted_count 仍表示 AC 提交次数，与排行榜解题数是不同统计。

### 5.1 WebSocket 会话与 Principal 约定（oj-server 内部，但影响推送可用性）

`convertAndSendToUser(userId, "/queue/judge", msg)` 的投递依赖 **STOMP 会话已绑定 `java.security.Principal`**，
且按 `Principal#getName()` 的返回值匹配目标。约定如下，**三条同时成立才能送达**：

| # | 约定 | 位置 |
|---|---|---|
| 1 | `LoginUser` **实现 `java.security.Principal`**，`getName()` 返回 **userId 字符串**（不是 username） | `oj-server/security/LoginUser` |
| 2 | STOMP `CONNECT` 帧原生头携带 `Authorization: Bearer <token>`，入站通道拦截器校验后 `accessor.setUser(...)` | `oj-server/config/WebSocketConfig#configureClientInboundChannel`（委托 JudgeWebSocketInterceptor） |
| 3 | 前端 `@stomp/stompjs` 的 `Client` 每次 `beforeConnect` 更新 `connectHeaders: { Authorization }` | `oj-web/src/api/ws.ts` |

> **踩坑实证（2026-11-16）**：v1.0 三条全缺——`/ws/**` permitAll、无拦截器、`LoginUser` 不是 Principal、
> 前端不带凭证。结果是 `UserDestinationMessageHandler` 打印
> `No active sessions for user destination` 后**静默丢弃**全部推送，接口不报错、日志不打错
> （只有 `log.debug`），页面看起来正常纯靠前端 2s 轮询兜底。这类"静默失败"必须靠约定锁死。


`LeaderboardReconcileTask` 每 5 分钟根据数据库去重汇总纠偏，用于 Redis 故障漏更新、历史脏数据等场景。ZSet 是页面读取来源，数据库汇总是纠偏依据。

## 6. 真实评测执行

### 6.1 模式与语言

默认 `ENGINE_MODE=real`，application.yml 与 Compose 一致。Docker 或镜像缺失时返回 SYSTEM_ERROR，不自动退回伪判。显式 `mock` 仅用于联调演示，结果标注未执行代码；`local` 返回 SYSTEM_ERROR，静态文本检查不提供通过结果；未知模式同样拒绝评测。

| language | 镜像 | 编译命令 | 运行命令 |
|---|---|---|---|
| `c` | `codeoj/c:gcc13` | `gcc -O2 -std=c17 main.c -o main` | `./main` |
| `cpp` | `codeoj/cpp:gcc13` | `g++ -O2 -std=c++17 main.cpp -o main` | `./main` |
| `java` | `codeoj/java:openjdk17` | `javac -encoding UTF-8 Main.java` | `java -Xss8m -Xmx{mem}m Main` |
| `python3` | `codeoj/python:3.12` | 无 | `python3 main.py` |
| `js` | `codeoj/node:20` | 无 | `node --max-old-space-size={mem} main.js` |

题目必须有 1～1000 个有效用例，时限为 1～60000 ms，内存为 16～1024 MB。Java、JavaScript 的内存下限为 64 MB。新提交保存资源限制与有序的用例快照，按内容去重，同一提交各次执行均使用原快照。序列化快照超过 32 MiB 或用例超过 1000 组时拒绝受理。空输入和空期望输出合法，缺少有效用例时判 SYSTEM_ERROR。

### 6.2 supervisor 与程序身份

当前容器使用可信 root runner supervisor 保存统计和输出文件；编译器、用户程序及其内层 timeout 都通过 setpriv 降权为配置的非 root UID:GID（默认 1000:1000）。supervisor 仅保留降权所需 SETUID、SETGID，用户代码不以 root 运行。

共同隔离参数：

```text
network=none; memory={containerMem}m; memory-swap={containerMem}m; cpus=1
pids-limit=64; read-only rootfs
cap-drop=ALL; cap-add=SETUID,SETGID; no-new-privileges
tmpfs /tmp: rw,size=64m,noexec,nosuid,nodev
tmpfs /run/codeoj: rw,size=40m,noexec,nosuid,nodev,mode=0700
ulimit nofile=64:64; nproc=64:64; fsize=16777216:16777216
```

`/run/codeoj` 内的统计文件只对 supervisor 可访问。程序 stderr 中伪造统计文本不能替换可信文件生成的最终统计；统计协议缺失或容器异常退出返回 SYSTEM_ERROR。runner 协议为：

```text
/judge/runner.sh run --timeout-ms {ms} --memory-mb {mb} --input /work/input.txt --user 1000:1000 -- <command arguments>
```

命令始终使用参数数组传递，不拼接用户 shell 字符串。输出正文上限为 16 MB，错误摘要最多 2048 字符。runner 内 timeout 与宿主 docker kill 看护共同限制执行；宿主看护时限覆盖程序时限及启动/收尾余量，不能用固定 15 秒提前杀死合法的长时限题目。

### 6.3 执行尝试与用例隔离

每次领取创建包含 submissionId、executionToken 和随机后缀的独占工作目录，不能清理其他尝试目录。编译目录可写；编译结束后仅接收校验通过的普通产物文件，拒绝符号链接和超大产物。

每个用例分别创建目录，复制只读产物和本用例 input.txt，以 `/work:ro` 挂载运行。程序可写临时空间位于本容器 tmpfs，不得修改下一用例输入、替换可执行文件或沿符号链接诱使宿主覆盖文件。容器名带随机 UUID，重试执行不复用旧容器名。finally 负责移除容器和本次目录；异常遗留资源的清理由运维单独核查。

### 6.4 资源与比对口径

编译容器内存预算为 512 MB；运行容器内存为题目限制加语言运行时余量。规划并发必须同时考虑编译高峰、运行容器、可信输出 tmpfs 和其他服务开销，不能只用题目内存乘槽位数。

输出比对统一换行，去行尾空白、忽略末尾空行，保留其他有意义的空白。任务在逐用例执行期间检查总预算（预算上限 10 分钟），每个容器还受自身看护限制。

稳态活动沙箱上限受 `min(JUDGE_CONCURRENCY, MAX_PARALLEL_CONTAINERS)` 限制，两者默认 4。性能数据必须记录实际消费线程数、容器并发峰值、镜像和硬件，不能仅按环境变量推算槽位或吞吐。

## 7. 职责边界

| 职责 | oj-server | oj-judge |
|---|---|---|
| 用户/题目/用例业务 | 负责 | 按提交编号回查 |
| 事务接受提交、首发 MQ | 负责 | — |
| QUEUING 补发与过期租约恢复 | — | 负责 |
| 编译运行、比对、终态/明细/题目统计 | — | 负责 |
| 排行榜 | 读取与对账 | AC 后投影 |
| WebSocket | 鉴权与定向推送 | 发布阶段/结果事件 |
| 队列监控 | 读取积压与心跳 | 注册及续写实例心跳 |

oj-judge 数据库写入范围仍为 submission 的状态/租约/发送窗口、judge_case 明细及 problem 统计字段；其他业务表不写。共享实体、SQL、接口变化要一起审阅，不能覆盖成员 B 已交付功能。

## 8. 旧数据库升级

1. 备份数据库，停止所有旧 oj-server 与 oj-judge 实例，避免旧版仅按 status 写回跨越新租约边界。
2. 已有数据库执行一次 `sql/migrations/20261116_judge_leases.sql`，新增四字段与恢复索引。该脚本不是重复执行脚本。
3. 全新数据库直接导入当前 `sql/schema.sql`，不要再重复运行迁移。
4. 已有库在旧迁移完成后执行一次 `sql/migrations/20261118_judge_dispatch_snapshot.sql`，新增投递确认、快照以及用例总数。旧记录没有总数时按历史数据兼容展示。
5. 重建五个判题镜像和服务镜像，再启动新版本。runner 更新后会区分程序主动返回 124 与计时器超时。确认 ENGINE_MODE=real、共享宿主工作目录与 Docker socket 挂载正确。
6. 核查任务领取、异常恢复、同题重复 AC、WebSocket 与只读用例回归。真实 Docker 验证和性能结论以本次验证记录为准。

## 9. 变更登记

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-11-13 | v1.0 | 骨架协议定稿 |
| 2026-11-16 | v1.1 | 成员 B：WebSocket LoginUser Principal 与 CONNECT 鉴权约定 |
| 2026-11-16 | v1.2 | 租约与 token 写回校验；持久 QUEUING 周期补发；共享 SQL；去重榜单；墙钟时间；可信 supervisor；执行/用例隔离；real 默认及拒绝伪判；数据库迁移 |
| 2026-11-18 | v1.3 | 记录投递确认、Docker 瞬时故障延迟重试、用例快照和总数、语言内存下限及超时判定修正 |

业务时间 created_at/judged_at 沿用成员 B 的 Asia/Shanghai DATETIME、序列化及查询契约；内部 judge_lease_until/next_dispatch_at 统一 UTC 数据库时钟。next_dispatch_at 默认表达式显式 UTC，不能改为按上海会话生成的 CURRENT_TIMESTAMP。CONNECT 使用 LoginUser 身份，校验签名、黑名单及数据库用户状态，只允许 /user/queue/judge 订阅，拒绝客户端 SEND。
