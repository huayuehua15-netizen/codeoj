# A 侧开发交接说明（评测链路）

> 给成员 A 的起点文档。你的战场：**oj-judge、judge-images、判题相关部署**。
> oj-server / oj-web / docs 已由成员 B 交付并实测——**不要动**（oj-common 改动需双方同意）。
> 配套：[CONTRACT.md](./CONTRACT.md)（模块契约）、[API.md](./API.md)（前后端契约）。

## 1. 现状（B 侧已交付）

- 全链路已通：注册登录 → 提交（限流/幂等/校验）→ 入队 `oj.judge.queue` → 消费 → 回写 → 事件 → WS 推送 → 前端三阶段展示
- 数据库 7 张表已建（`sql/schema.sql`，含 `SET NAMES utf8mb4`，直接可导入）
- 排行榜对账任务、实例心跳、队列监控等运维件已就位
- 判题服务当前 `ENGINE_MODE=local`（静态语义判定：无入口判 CE、无输出判 WA，拦截无效提交；**真实运行判定是你的活**）

## 2. 你的任务：real 模式（唯一大缺口）

### 2.1 待实现文件

| 文件 | 内容 |
|---|---|
| `oj-judge/src/main/java/com/codeoj/judge/core/sandbox/DockerSandbox.java` | 沙箱执行（当前为骨架+参数清单注释）。实现点：① HostConfig 组装（参数见 §2.2）② createContainerCmd 参数列表传命令（**不经 shell**）③ 宿主看护定时器 killContainerCmd ④ execStartCmd 帧流解析（stdout/stderr 分离 + 16MB 截断）⑤ finally removeContainerCmd |
| `oj-judge/src/main/java/com/codeoj/judge/core/JudgeEngine.java` 的 `realRun()` | 编译 → 逐用例沙箱执行 → `OutputComparator` 比对 → 汇总（CE/WA/TLE/MLE/RE 判定优先级见 CONTRACT §2） |
| `judge-images/*` | 四镜像构建并验证（`docker build -f c/Dockerfile -t codeoj/c:gcc13 .` 等，上下文为 judge-images 目录） |

### 2.2 沙箱参数（论文表 4-9，已固化设计）

```
--network none  --memory {mem}m --memory-swap {mem}m  --cpus 1.0
--pids-limit 64  --read-only  --tmpfs /tmp:rw,size=64m,noexec
-v {workDirRoot}/{sid}/{caseNo}:/work:rw  --cap-drop ALL
--security-opt no-new-privileges  -u 1000:1000
--ulimit nofile=64:64  --ulimit nproc=64:64  --ulimit fsize=16777216
```

- 容器名 `judge-{sid}-{caseNo}`，`--rm` 即用即删，工作目录按提交隔离
- runner 协议：`/judge/runner.sh run --timeout-ms {ms} --memory-mb {mb} --input /work/input.txt <cmd...>`（**参数列表传递，不经 shell**；runner.sh 已实现 /usr/bin/time 测量与双重超时）
- 命令构造命令清单见各 `LanguageStrategy`（表 5-2），运行命令带 `main.exe → ./main` 注意 Linux 差异

### 2.3 参考实现（已验证设计，直接可用）

**local 真实判题的完整实现**（编译运行/逐用例/CE-TLE-RE-WA 判定/Fail-Fast/错误摘要）在成员 B 的交付记录中（会话对话 2026-11-15，`LocalJudgeService.java` 完整版）。其中"宿主机进程执行"部分在 B 侧环境被部署安全策略拦截无法落库，**在 Docker 容器内执行正是 real 模式的形态，接口签名与判定规则原样复用**：

- 编译：`spawnFailed → CE`（编译器缺失）/ `exitCode != 0 → CE`（stderr 截 500 字为 errorInfo，论文 F-08）
- 运行：`timedOut → TLE` / `exitCode != 0 → RE`（stderr 摘要）/ `OutputComparator.matches → AC : WA`（Fail-Fast）
- 汇总：`passedCases/totalCases`、`max(timeUsedMs)`、内存按沙箱 cgroup 测量（local 无 cgroup 为 null）

### 2.4 切换与验收

```
ENGINE_MODE=real   # 环境变量或 application.yml；mock/local 保留可随时切回
```

验收清单（对应论文表 6-1 / 6-2，B 侧页面已就绪可直接演示）：

| 用例 | 预期 |
|---|---|
| 四语言正确代码 | AC，耗时/内存有真实值 |
| 语法错误 | CE + 编译器报错 |
| 输出错误 | WA + 首个失败用例 |
| 死循环（时限 1s） | TLE，耗时 ≈ 时限，容器被清理 |
| 内存炸弹 | MLE |
| fork 炸弹 / 读 /proc / 提权 | RE，宿主机无异常 |
| 重复消息投递 | 幂等（条件更新跳过） |

## 3. 环境备忘

- 本机已验证：Docker（WSL2，用户级安装）+ DaoCloud 镜像源（Docker Hub 直连被墙）；MySQL/Redis/RabbitMQ 容器在跑（`docker ps`）
- RabbitMQ 账号：`codeoj/codeoj123`（环境变量已配）
- 判题镜像需从 DaoCloud 拉基础镜像：`docker pull docker.m.daocloud.io/library/python:3.12` 等

## 4. 已知注意事项

1. **oj-judge 与 oj-server 的 `SubmissionMapper` 条件更新 SQL 必须完全一致**（幂等语义，CONTRACT §3），改一处同步另一处
2. **事件消息是 JSON**（Jackson2JsonMessageConverter），**判题任务消息是纯文本**（SimpleMessageConverter）——不要混
3. `ENGINE_MODE=local` 的静态判定版已上线拦截垃圾提交；你的 real 落地后 B 侧页面零改动即可展示真实判定
