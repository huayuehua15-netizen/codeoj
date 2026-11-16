# CodeOJ —— 在线代码评测与编程练习平台

基于 **Spring Boot + Vue3 + Docker** 的在线代码评测与编程练习平台（毕业设计项目）。

设计文档：《基于 Spring Boot + Vue3 + Docker 的在线代码评测与编程练习平台设计与实现》

## 📚 协作文档（双人对接以此为准）

| 文档 | 内容 | 谁看 |
|---|---|---|
| [docs/API.md](docs/API.md) | REST 全量接口 + WebSocket 推送协议（含请求/响应示例、错误码、联调清单） | **前端必读**，后端按此补全 |
| [docs/CONTRACT.md](docs/CONTRACT.md) | oj-server ↔ oj-judge 契约：MQ 拓扑、判题状态机、幂等语义、Redis 键、沙箱参数、职责边界 | **后端必读** |
| [docs/COLLABORATION.md](docs/COLLABORATION.md) | 双人分工方案（A 评测链路 / B 业务前端）、里程碑 M1~M4、Git 规范、联调环境 | **全员必读** |

**契约变更纪律：先改文档并登记版本 → 知会对方 → 再改代码。**

## 工程结构（论文 5.1）

```
code-oj/
├── pom.xml                 # Maven 父工程（聚合三个后端模块）
├── oj-common/              # 共享模块：实体、枚举、常量、消息体、统一返回
├── oj-server/              # 业务服务（8080）：认证、题目、提交、排行、WebSocket 推送
├── oj-judge/               # 判题服务（8081）：消费队列、docker-java 调度沙箱
├── oj-web/                 # 前端：Vue3 + Vite + TS + Pinia + Element Plus + Monaco Editor
├── judge-images/           # 判题镜像：c/cpp/java/python3/js 五语言 + runner.sh
├── deploy/                 # 部署：docker-compose、Nginx、多阶段 Dockerfile
├── sql/schema.sql          # 数据库初始化与种子数据
└── README.md
```

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | JDK 17 / Spring Boot 3.2.x / Spring Security(JWT) / MyBatis-Plus |
| 消息 | RabbitMQ（direct 交换机 `oj.judge.exchange` + 持久化队列 `oj.judge.queue` + 死信） |
| 缓存 | Redis（ZSet 排行榜、限流计数、幂等锁、令牌黑名单） |
| 沙箱 | docker-java 3.3.x，一次性容器；root supervisor 仅保留 SETUID/SETGID，编译器和程序降权；网络、资源与只读用例隔离 |
| 推送 | WebSocket（STOMP），端点 `/ws`，用户级 `/user/queue/judge` |
| 前端 | Vue3 / Vite / TypeScript / Pinia / Element Plus / Monaco Editor / SockJS |
| 存储 | MySQL 8.0（utf8mb4/InnoDB）、Redis 7.x |

## 第一阶段（骨架）已完成

- ✅ Maven 多模块工程与全部依赖配置
- ✅ 实体、枚举（判题状态机）、Redis 键、MQ 拓扑常量
- ✅ 认证（BCrypt + JWT + Redis 黑名单）、限流、幂等锁
- ✅ 提交链路（限流→幂等锁→校验→事务写库→事务后发消息）
- ✅ 判题消费者（手动 ACK、prefetch=1、可配置并发、租约/token 条件更新、死信与周期恢复）
- ✅ WebSocket 推送链路（judge → 事件队列 → server → convertAndSendToUser）
- ✅ 全部 REST 接口骨架（按论文表 4-7）
- ✅ 数据库 schema（表 4-1~4-5 + course/course_problem + 种子题目）
- ✅ 前端全部页面骨架（题目列表/详情/编辑器/提交/记录/统计/排行/管理端）
- ✅ 判题镜像与 runner.sh（/usr/bin/time 测量 + 双重超时 + 16MB 截断）
- ✅ Docker Compose 一键编排与多阶段构建

## 第二阶段（实现）进度

| 模块 | 状态 | 说明 |
|---|---|---|
| oj-judge | 实现与加固已落盘，待集成验收 | 五语言真实判题、沙箱可信统计、程序墙钟时限、用例工作目录隔离及恢复逻辑；以最新测试证据确认可运行版本 |
| oj-server / oj-web | B 侧代码已合并 | 已实现业务与前端功能见 [进度文档](docs/PROGRESS.md)，真实评测仍需与 A 侧联调 |
| 部署与测试 | 修复后待复测 | 历史四语言记录与当前验证状态分开保存；旧压测将中位数误标为平均值，不能沿用达标结论。见 [验证记录](judge-tests/联调报告.md)、[独立复测步骤](judge-tests/perf/README.md) |

当前修复包含持久 QUEUING 补发、租约过期恢复与 token 写回校验、去重解题数榜单、程序墙钟耗时和按执行尝试/用例隔离。完整协议与数据库升级要求见 [CONTRACT v1.1](docs/CONTRACT.md)，交接清单见 [HANDOFF-A](docs/HANDOFF-A.md)。新增回归已执行，真实环境结果以对应验证记录为准。

默认 `ENGINE_MODE=real`。显式 `mock` 只供联调并标注未执行代码；`local` 和未知模式返回系统错误。镜像或 Docker 缺失时拒绝产生通过结果。

真实判题服务部署于 Linux/WSL 的 Docker 环境，按同一宿主路径共享工作目录；Windows 前端开发可保留本机环境。不能用宿主机直接运行用户代码代替沙箱。

## 本地开发

```bash
# 1. 准备中间件（MySQL/Redis/RabbitMQ）
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0
docker run -d -p 6379:6379 redis:7
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management

# 2. 仅新库：初始化数据库
mysql -h127.0.0.1 -uroot -p123456 < sql/schema.sql

# 3. 启动后端
mvn install -DskipTests
mvn spring-boot:run -pl oj-server
mvn spring-boot:run -pl oj-judge

# 4. 启动前端（Node 22+；使用锁文件安装）
cd oj-web && npm ci && npm run dev   # http://localhost:5173

# 5. 构建判题镜像（需要 Docker，Linux 部署环境）
cd judge-images
docker build -f c/Dockerfile      -t codeoj/c:gcc13 .
docker build -f cpp/Dockerfile    -t codeoj/cpp:gcc13 .
docker build -f java/Dockerfile   -t codeoj/java:openjdk17 .
docker build -f python/Dockerfile -t codeoj/python:3.12 .
docker build -f node/Dockerfile   -t codeoj/node:20 .
```

## 一键部署

已有数据库先备份并停止所有旧 server/judge，执行一次 [租约迁移](sql/migrations/20261116_judge_leases.sql)，再启动新服务；全新数据库使用当前 schema，不重复迁移。判题镜像必须按上文五条命令先构建，Compose 的服务构建不会替代判题镜像构建。

```bash
cd deploy
docker compose up -d --build
# 判题扩容
docker compose up -d --scale oj-judge=4
```

`JUDGE_CONCURRENCY` 与 `MAX_PARALLEL_CONTAINERS` 默认均为 4，分别限制消费线程和容器信号量，稳态并发受两者较小值限制。调整后核查实际线程与容器峰值，再采集吞吐；不要沿用旧报告的槽位推算。`/judge/work` 在宿主机与判题服务中必须对应同一路径。

## 管理员账号

通过 `/api/auth/register` 注册后提权：

```sql
UPDATE user SET role = 1 WHERE username = '你的用户名';
```
