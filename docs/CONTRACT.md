# CodeOJ 模块对接契约（oj-server ↔ oj-judge）

> 版本：v1.0（骨架阶段定稿）｜ 本文是两个后端服务之间唯一的对接依据。
> 两个服务通过 **数据库 + RabbitMQ + Redis** 协作，代码上无任何直接调用（论文 5.1：可独立发布与扩容）。

## 1. 判题消息拓扑（论文 4.6）

声明位置：`oj-common/MqConstants` + 两端各自的 `RabbitMqConfig`（声明幂等，谁先启动谁生效）。

### 1.1 判题任务（oj-server → oj-judge）

| 组件 | 值 | 配置 |
|---|---|---|
| Exchange | `oj.judge.exchange` | direct，durable |
| Routing Key | `judge.submit` | — |
| Queue | `oj.judge.queue` | durable + 死信绑定 |
| DLX / DLQ | `oj.judge.dlx` / `oj.judge.dlq` | routing key `judge.dead` |

**消息体：纯文本，仅提交 ID**（如 `"100"`），**不携带代码与用例**——消费者回查数据库，既控制消息体积又天然保证数据一致（论文 4.6）。

生产端语义（oj-server）：
- 消息在**事务提交之后**发布（避免"库未落盘、消息先到"，论文 5.3）；
- publisher-confirm 开启（`correlated`），投递失败打日志，由启动扫描兜底（见 §4）。

消费端语义（oj-judge）：
- `acknowledge-mode: manual`，`prefetch: 1`，`concurrency: 4`（论文 5.4）；
- **只能 basicAck / basicNack**：nack 的 `requeue` 参数必须为 `!msg.isRedelivered()`——首次失败重投一次，再次失败进死信（论文 4.6）。

### 1.2 判题结果事件（oj-judge → oj-server）

| 组件 | 值 |
|---|---|
| Exchange | `oj.event.exchange`（direct，durable） |
| Routing Key | `event.judge.finished` |
| Queue | `oj.event.judge.queue`（durable） |
| 序列化 | JSON（Jackson2JsonMessageConverter，两端同名 bean `judgeEventMessageConverter`） |

消息体（`oj-common/JudgePushEvent`）：

```json
{
  "submissionId": 100,
  "userId": 1,
  "problemId": 1,
  "status": "ACCEPTED",          // 枚举名，见 §2
  "timeUsedMs": 34,
  "memoryUsedKb": 3200,
  "passedCases": 4,
  "totalCases": 4,
  "errorInfo": null
}
```

oj-server 收到后经 `convertAndSendToUser(userId, "/queue/judge", {type: "JUDGE_FINISHED", data})` 定向推送（见 API.md §7）。**事件是"尽力送达"，结果权威来源是数据库**——前端断线兜底靠回查接口。

## 2. 判题状态机（论文表 4-8）

定义位置：`oj-common/enums/SubmissionStatus`。**两端禁止各自定义状态值。**

| 状态 | code | 判定依据 | 性质 |
|---|---|---|---|
| QUEUING 排队中 | 0 | 提交入库，等待消费 | 中间态 |
| JUDGING 判题中 | 1 | 消费者领取任务 | 中间态 |
| ACCEPTED 通过 | 2 | 全部用例输出与期望一致 | 终态 |
| WRONG_ANSWER 答案错误 | 3 | 首个用例输出不匹配（Fail-Fast） | 终态 |
| TIME_LIMIT_EXCEEDED 超时 | 4 | 运行时间超过时间限制 | 终态 |
| MEMORY_LIMIT_EXCEEDED 超内存 | 5 | 内存使用超过内存限制 | 终态 |
| RUNTIME_ERROR 运行错误 | 6 | 非零退出码/段错误/异常 | 终态 |
| COMPILE_ERROR 编译错误 | 7 | 编译失败 | 终态 |
| SYSTEM_ERROR 系统错误 | 8 | 判题服务自身故障 | 终态 |

**流转规则：`0 → 1 → (2~8)`，单向、不可跳过、不可回退。** 任何写库必须走条件更新（§3），禁止无条件 UPDATE status。

## 3. 幂等语义（论文 5.4 条件更新）

SQL 固化在两端的 `SubmissionMapper`（同名同实现，改一处必须同步另一处）：

```sql
-- 领取任务：0→1。影响行数 0 = 重复投递，直接 basicAck 跳过
UPDATE submission SET status = 1 WHERE id = #{id} AND status = 0;

-- 回写终态：1→终态。影响行数 0 = 重复投递/已被处理，跳过计分与明细
UPDATE submission SET status = #{status}, time_used_ms = #{time}, memory_used_kb = #{mem},
       error_info = #{err}, judged_at = NOW()
 WHERE id = #{id} AND status = 1;
```

结果回写事务内完成三件事：① 条件更新主记录 ② 插入 `judge_case` 明细 ③ `problem` 冗余统计 +1（AC 时 submit_count 与 accepted_count 同时 +1）。**重复投递绝不重复计分。**

## 4. 可靠性分工（论文 4.6）

| 机制 | 责任方 | 说明 |
|---|---|---|
| 持久化消息 + 手动 ACK | 双方 | 队列/交换机 durable，消息落盘后确认 |
| 消费者宕机重投 | RabbitMQ | 未确认消息自动重投；配合 §3 幂等安全 |
| 死信队列告警 | oj-judge 消费时 nack | 持续失败任务进 `oj.judge.dlq`，人工排查 |
| **启动扫描** | oj-judge（`StartupScanner`） | 服务重启时将滞留 `status=1` 的任务重置为 0 并重新入队 |
| 事件推送丢失兜底 | oj-web | 轮询 `GET /api/submissions/{id}`（API.md §7.4） |

## 5. Redis 键约定（论文表 4-6）

定义位置：`oj-common/constant/RedisKeys`。**禁止在代码中硬编码键名。**

| Key | 类型 | TTL | 写方 | 读方 | 说明 |
|---|---|---|---|---|---|
| `oj:leaderboard:ac` | ZSET | 永久 | **oj-judge**（AC 时 ZINCRBY +1） | oj-server（分页倒序） | member=userId，score=AC 题数 |
| `oj:stats:problem:{id}` | HASH | 5 min | oj-server | oj-server | 热点题目统计缓存，过期回源 |
| `oj:token:blacklist:{jti}` | STRING | token 剩余时长 | oj-server（登出） | oj-server（鉴权） | 令牌黑名单 |
| `oj:rate:{type}:{key}` | STRING | 窗口 | oj-server | oj-server | 登录/提交滑动窗口限流 |
| `oj:lock:submit:{userId}:{problemId}` | STRING NX | 2 s | oj-server | — | 重复提交幂等锁 |
| `oj:queue:backlog` | STRING | 30 s | oj-server | oj-server | 队列积压量缓存 |

> 对账：`oj:leaderboard:ac` 与数据库汇总由异步对账任务纠偏（每 5 分钟，`LeaderboardReconcileTask`），以 ZSet 为准。
>
> **⚠️ 序列化约定（已踩坑）**：排行榜 ZSet 的 **member 一律为裸字符串 userId**（StringRedisTemplate）。
> 两端序列化不一致会产生同一用户的双 member（`40` 与 `"40"`），页面出现重复行——
> oj-server（读侧/对账）与 oj-judge（ZINCRBY）均已统一为 StringRedisTemplate，勿改回 JSON 序列化。

## 6. 判题执行约定（oj-judge 内部，论文表 5-2 / 5.4）

### 6.1 语言与镜像

| language 值 | 镜像 | 编译命令 | 运行命令 |
|---|---|---|---|
| `c` | `codeoj/c:gcc13` | `gcc -O2 -std=c17 main.c -o main` | `./main` |
| `cpp` | `codeoj/cpp:gcc13` | `g++ -O2 -std=c++17 main.cpp -o main` | `./main` |
| `java` | `codeoj/java:openjdk17` | `javac -encoding UTF-8 Main.java` | `java -Xss8m -Xmx{mem}m Main` |
| `python3` | `codeoj/python:3.12` | — | `python3 main.py` |

### 6.2 沙箱隔离参数（论文表 4-9，第二阶段实现于 DockerSandbox）

```
--network none --memory {mem}m --memory-swap {mem}m --cpus 1.0
--pids-limit 64 --read-only --tmpfs /tmp:rw,size=64m,noexec
-v {workDirRoot}/{sid}/{caseNo}:/work:rw --cap-drop ALL
--security-opt no-new-privileges -u 1000:1000
--ulimit nofile=64:64 --ulimit nproc=64:64 --ulimit fsize=16777216
```

- 容器名：`judge-{sid}-{caseNo}`；`--rm` 即用即删；工作目录按提交编号隔离
- runner 协议：`/judge/runner.sh run --timeout-ms {ms} --memory-mb {mb} --input /work/input.txt <cmd...>`，参数列表传递不经 shell
- 双重超时：容器内 timeout（SIGTERM→1s→SIGKILL）+ 宿主看护定时器 docker kill
- 输出比对：去行尾空白、忽略末尾空行；输出 >16MB 判 RE（`OutputComparator`）

### 6.3 并发分层（论文 4.6）

队列层 prefetch=1（公平分发+背压）→ 实例层信号量 `max-parallel-containers: 4` → 任务层全局时限看护。

## 7. 两端职责边界一览

| 职责 | oj-server | oj-judge |
|---|---|---|
| 用户/题目/用例 CRUD | ✅ | ❌（只读回查 submission/problem/test_case） |
| 提交入口（限流/幂等/校验/入队） | ✅ | ❌ |
| 编译运行沙箱判题 | ❌ | ✅ |
| 回写 submission/judge_case/problem 统计 | ❌ | ✅（条件更新） |
| 排行榜 ZSet 写入 | ❌（只读+对账） | ✅（AC ZINCRBY） |
| WebSocket 推送 | ✅ | ❌（只发事件消息） |
| 队列监控 | ✅ | 心跳上报（第二阶段） |

> 数据库写权限约定：oj-judge **只允许写** `submission`（条件更新）、`judge_case`、`problem` 统计字段三个位置；其余一律不写。

## 8. 变更管理

| 日期 | 版本 | 变更内容 | 提出人 |
|---|---|---|---|
| 2026-11-13 | v1.0 | 骨架阶段定稿 | — |
