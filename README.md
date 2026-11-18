# CodeOJ —— 在线代码评测与编程练习平台

基于 **Spring Boot + Vue3 + Docker** 的在线代码评测与编程练习平台（毕业设计项目）。

设计文档：《基于 Spring Boot + Vue3 + Docker 的在线代码评测与编程练习平台设计与实现》

## 📚 协作文档（双人对接以此为准）

| 文档 | 内容 | 谁看 |
|---|---|---|
| [docs/API.md](docs/API.md) | REST 全量接口 + WebSocket 推送协议（含请求/响应示例、错误码、联调清单） | **前端必读**，后端按此补全 |
| [docs/CONTRACT.md](docs/CONTRACT.md) | oj-server ↔ oj-judge 契约：MQ 拓扑、判题状态机、幂等语义、Redis 键、沙箱参数、职责边界 | **后端必读** |
| [docs/COLLABORATION.md](docs/COLLABORATION.md) | 双人分工方案（A 评测链路 / B 业务前端）、里程碑 M1~M4、Git 规范、联调环境 | **全员必读** |
| [docs/PROBLEM-BANK.md](docs/PROBLEM-BANK.md) | 50 道教学题、测试数据、增量导入与真实判题验收 | 题库维护与演示 |

**契约变更纪律：先改文档并登记版本 → 知会对方 → 再改代码。**

## 工程结构（论文 5.1）

```
code-oj/
├── pom.xml                 # Maven 父工程（聚合三个后端模块）
├── oj-common/              # 共享模块：实体、枚举、常量、消息体、统一返回
├── oj-server/              # 业务服务（8080）：认证、题目、提交、排行、WebSocket 推送
├── oj-judge/               # 判题服务（8081）：消费队列、docker-java 调度沙箱
├── oj-web/                 # 前端：Vue3 + Vite + TS + Pinia + Element Plus + Monaco Editor
├── judge-images/           # 判题镜像：c/cpp/java/python3/js 五语言 + runner.sh + build.sh
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
- ✅ 判题镜像一键构建与自检脚本 `judge-images/build.sh`（构建 + 五语言探针自检 + 镜像源回退）
- ✅ Docker Compose 一键编排与多阶段构建

## 第二阶段（实现）进度

| 模块 | 状态 | 说明 |
|---|---|---|
| oj-judge | 五语言真实链路已验证 | 沙箱、租约恢复与隔离已有回归及真实 AC 证据；[镜像构建自检](docs/JUDGE-IMAGES.md) 支持新环境复现 |
| oj-server / oj-web | 已集成业务与页面 | 包含课程、竞赛、统计、排行榜和管理端；已补题组/用例完整性与历史删除保护 |
| 部署与测试 | 本地回归通过，持续完善验收 | Java 70 项、Python 19 项及真实接口验证通过；完整恶意样例与大并发仍需单独复测。新增 CI 配置，云端运行以提交后结果为准 |

当前修复包含持久 QUEUING 补发、租约过期恢复与 token 写回校验、去重解题数榜单、程序墙钟耗时和按执行尝试/用例隔离。完整协议与数据库升级要求见 [CONTRACT v1.2](docs/CONTRACT.md)，交接清单见 [HANDOFF-A](docs/HANDOFF-A.md)。2026-11-17 完善记录见 [交付检查](docs/DELIVERY-CHECKLIST.md)。

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
```

另开终端，在仓库根目录构建判题镜像：

```bash
# 5. 构建判题镜像（需要 Docker；在 judge-images/ 目录执行）
cd judge-images
./build.sh                 # 构建五个镜像并逐个自检，含镜像源回退
# 单独重建：./build.sh cpp java     只自检：./build.sh --verify-only
```

> **判题镜像不会随仓库分发**，必须在每台部署机器上构建一次。跳过这一步，判题链路能启动、能收发消息，
> 但所有真实提交都会失败（`status=8` 系统错误）。详见 [判题镜像构建与验证](docs/JUDGE-IMAGES.md)。

## 一键部署

已有数据库先备份并停止所有旧 server/judge，执行一次 [租约迁移](sql/migrations/20261116_judge_leases.sql)，再启动新服务；全新数据库使用当前 schema，不重复迁移。判题镜像必须先按上文第 5 步执行 `judge-images/build.sh` 一步构建，Compose 的服务构建不会替代判题镜像构建。

```bash
cd deploy
cp .env.example .env    # 首次准备部署配置，再编辑密码与 JWT_SECRET
docker compose up -d --build
# 判题扩容
docker compose up -d --scale oj-judge=4
```

`JUDGE_CONCURRENCY` 与 `MAX_PARALLEL_CONTAINERS` 默认均为 4，分别限制消费线程和容器信号量，稳态并发受两者较小值限制。调整后核查实际线程与容器峰值，再采集吞吐；不要沿用旧报告的槽位推算。`/judge/work` 在宿主机与判题服务中必须对应同一路径。

首次启动会等待 MySQL TCP 认证、Redis 和 MQ 就绪。旧库升级还需按现有索引状态执行 `sql/migration-20261116-indexes.sql`；若曾执行早期租约迁移，使用 `sql/migrations/20261116_dispatch_utc_default.sql` 修正默认值，不重复添加字段。镜像自检和真实提交不能由演示数据替代。

若沿用早期只有七张表的数据库，备份并停止旧服务后，还需在目标数据库执行 [业务结构补齐迁移](sql/migrations/20261117_legacy_business_schema.sql)。它补齐 `user.status`、`problem.tags`、竞赛/公告/收藏表，并兼容旧 `nickname` 必填列；保留已有数据且可重复执行。直接导入 `schema.sql` 不会给已存在的表增加缺失字段。

## 构建与回归

```bash
mvn -B verify
python3 -m unittest discover -s judge-tests/tests -v
python3 -m unittest discover -s tools/tests -v  # Linux/Git Bash 环境下检查 hook
cd oj-web && npm ci && npm run build          # Node22+
```

`.github/workflows/ci.yml` 在推送和 PR 时执行后端测试、Python 工具检查及前端构建；本地通过 `git config core.hooksPath .githooks` 启用推送检查。推 dev 时要求工作区干净、构建提交与待推提交一致；缺工具或失败会拒绝推送。

> ⚠️ **克隆后必做一步**：在仓库根目录执行 `git config core.hooksPath .githooks`。
> 不执行的话提交格式检查和推 dev 构建检查**都不会生效**。这条配置存在本地 `.git/config`，
> 不随仓库分发，所以别人配过不代表你配过。验证：`git config --get core.hooksPath` 应输出 `.githooks`。
> 详见 [协作规范 §3.0](docs/COLLABORATION.md)。

> 云端检查是否强制合并仍由仓库分支保护设置决定，本地钩子不能替代云端检查。

## 管理员账号

通过 `/api/auth/register` 注册后提权：

```sql
UPDATE user SET role = 1 WHERE username = '你的用户名';
```
