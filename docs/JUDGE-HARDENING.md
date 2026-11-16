# A 评测链路交接说明

> 最新兼容复核：已基于 dev 72a9c2e 保留 B 新功能，并通过 61 项 Java 回归与真实接口验证；跨成员改动和时间约定见 [JUDGE-COMPATIBILITY.md](./JUDGE-COMPATIBILITY.md)。下文验证数字记录初次修复，最新批次以兼容报告为准。

> 更新日期：2026-11-16 ｜ 适用当前修复分支
> real 评测引擎与沙箱已有实现。本次重点是正确性、隔离和故障恢复修复，不能再按旧骨架文档重写已完成模块。
> 配套：[CONTRACT.md](./CONTRACT.md) v1.2、[API.md](./API.md)、[PROGRESS.md](./PROGRESS.md)。

## 1. 当前实现

| 部分 | 当前行为 |
|---|---|
| 判题引擎 | C/C++/Java/Python3/JavaScript 编译或解释执行，逐用例比对、Fail-Fast、结果汇总 |
| 模式 | real 默认；mock 仅显式联调；local/未知模式返回 SYSTEM_ERROR |
| 空用例/越界配置 | 拒绝产生 AC，返回 SYSTEM_ERROR |
| 时间 | timeUsedMs 为程序墙钟时间，CPU 时间仅诊断 |
| 统计 | root supervisor 保存可信统计；编译器/用户程序/timeout 降权为非 root |
| 文件隔离 | 每次租约独占目录；每用例只读目录；产物校验拒绝链接 |
| 消息恢复 | QUEUING 持久记录周期补发；过期租约恢复；失败释放后重投 |
| 结果幂等 | token 与未过期租约校验；终态、明细和题目统计同一事务 |
| 排行榜 | 数据库去重通过题数投影，StringRedisTemplate；保留周期对账 |
| 并发 | JUDGE_CONCURRENCY 与 MAX_PARALLEL_CONTAINERS 分别控制消费线程和容器信号量 |

## 2. 主要代码入口

| 文件 | 责任 |
|---|---|
| oj-common/persistence/SubmissionLeaseOperations | 两端共享领取、续租、释放、恢复、发送窗口和终态 SQL |
| oj-judge/mq/JudgeConsumer | 消息格式校验、租约领取、失败重投、提交后副作用 |
| oj-judge/mq/JudgeLeaseService | 独立线程续租，不依赖恢复扫描线程 |
| oj-judge/mq/StartupScanner | 启动与周期恢复，补发 QUEUING |
| oj-judge/core/result/ResultWriter | 当前所有者事务写回终态、明细及统计 |
| oj-judge/core/JudgeEngine、AttemptWorkspace | 编译一次、逐用例只读隔离与清理 |
| oj-judge/core/sandbox/DockerSandbox、RunnerStatParser | Docker 生命周期、可信统计协议与判定 |
| judge-images/runner.sh、五种语言 Dockerfile | supervisor、降权、内部超时与运行环境 |
| oj-server/service/impl/SubmissionServiceImpl | 事务接受提交，afterCommit 先 QUEUED 推送再 MQ |
| oj-server/mq/JudgeDispatcher | 独立事务预留发送窗口，持久纯文本首发 |
| oj-judge/mq/LeaderboardUpdater | 提交后按去重题数投影榜单 |

上表 Java 路径均相对于各模块的 src/main/java/com/codeoj 下对应包。judge/server 端的 SQL 已通过继承共享接口统一维护，不再复制两份不同实现。

## 3. 升级与运行

已有数据库必须先备份并停止全部旧 server/judge，再执行一次：

```bash
mysql -h <数据库主机> -u <用户> -p codeoj < sql/migrations/20261116_judge_leases.sql
```

新库直接使用当前 schema.sql，不能再重复运行该迁移。旧程序不能和新租约程序混跑，否则旧版仅按 status 条件更新仍可能越过新所有者校验。

构建五个判题镜像，使用当前 runner.sh；随后构建并启动新服务。详细构建命令见仓库 README。保持 ENGINE_MODE=real，Docker 不可用时应显示系统错误，不应静默切换 mock/local。

Compose 中 /judge/work 必须在宿主与 judge 服务中对应同一绝对路径，沙箱兄弟容器才能挂载正确目录。仅 judge 服务持有 Docker socket；用户沙箱不得挂载 socket。

## 4. 联调验收清单

本次 55 项 Java 回归通过；已在隔离 Docker 环境完成五语言 AC、12 次短 C 程序、8 组判定边界、权限边界、恢复与双实例租约、去重排行榜、原生 SockJS/STOMP 实收及用户隔离验证，见 judge-tests/联调报告.md。完整恶意样例、100/300/500 性能和浏览器 UI 验收仍需另行完成。

- 五语言正确程序 AC；语法错误 CE、错误输出 WA、超时 TLE、真实超内存 MLE、异常 RE；无用例 SYSTEM_ERROR。
- sleep 等低 CPU 程序仍按墙钟时间超时，程序耗时不包含容器启动。
- 用户代码不能修改可信统计，不能写下一个用例输入或替换其可执行文件；链接产物拒绝接收。
- 第一次瞬态处理失败确实再次执行；持续失败终结 SYSTEM_ERROR 并留下死信。
- 增加实例时健康租约不被抢走；过期执行不能写入新执行的结果或统计。
- 入库与 MQ 之间中断后，QUEUING 周期补发能恢复；多扫描器只预留一个发送窗口。
- 同一用户同题多次 AC 排行分数不增加；Redis 故障不把已完成任务退回重判。
- WebSocket 认证、三阶段推送与轮询兜底共同验证；记录实际消费线程与沙箱峰值，核对并发配置。

## 5. 对接约束

保留成员 B 既有业务、页面与五语言能力。共享字段、SQL、接口和跨服务修复按 CONTRACT v1.1 一起审阅；变更说明应包含数据库迁移。性能结论需要本次实际数据支持，不根据槽位公式或开发机结果推定生产吞吐。
