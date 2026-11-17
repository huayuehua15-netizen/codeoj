# A 侧开发交接说明（评测链路）

> 2026-11-16 A 修复补充：真实评测已经实现，当前交接与升级见 [JUDGE-HARDENING.md](./JUDGE-HARDENING.md)；下文保留 B 的上手说明与历史基线。旧库须执行租约迁移，默认运行 real。前端构建使用 Node 22+。

> 给成员 A 的起点文档。你的战场：**oj-judge、judge-images、判题相关部署**。
> oj-server / oj-web / docs 已由成员 B 交付并实测——**不要动**（oj-common 改动需双方同意）。
> 配套：[CONTRACT.md](./CONTRACT.md)（模块契约）、[API.md](./API.md)（前后端契约）。

## 0. 上手第一步（新手请先看完这段，10 分钟）

### 0.1 你需要装什么

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | 17（必须 17，不是 21） | `java -version` 确认 |
| Maven | 3.9+ | 项目根目录有 `pom.xml` |
| Node.js | 18+ | 只在你需要跑前端时用 |
| Docker Desktop | 最新 | 判题沙箱依赖它，**装完要确认 `docker ps` 能跑通** |
| Git | 最新 | |

> Docker 在本机可能连不上镜像源（国内网络），需要配 DaoCloud 加速，见 §3。

### 0.2 把项目跑起来（4 步）

```bash
# 1. 拿到代码，切到自己的分支
git clone <仓库地址>
cd code-king
git checkout dev
git checkout -b feat/judge-engine     # 建自己的分支
git push -u origin feat/judge-engine

# 2. 起中间件（MySQL / Redis / RabbitMQ，三个容器）
docker run -d --name oj-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0
docker run -d --name oj-redis -p 6379:6379 redis:7
docker run -d --name oj-rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=codeoj -e RABBITMQ_DEFAULT_PASS=codeoj123 \
  rabbitmq:3.13-management

# 3. 导入数据库
mysql -h127.0.0.1 -uroot -p123456 < sql/schema.sql

# 4. 编译整个项目（第一次会比较慢，要下依赖）
mvn clean install -DskipTests
```

**验证成功**：`mvn` 最后显示 `BUILD SUCCESS`。失败先看 §3 的环境备忘。

### 0.3 启动两个服务

```bash
# 终端 1：业务服务（8080）
mvn spring-boot:run -pl oj-server

# 终端 2：判题服务（8081）——你主要改这个
mvn spring-boot:run -pl oj-judge
```

> **RabbitMQ 账号必须对上（最常见的启动失败原因）**
>
> 项目配置文件里的默认值是 `guest / guest`，但**实际联调环境用的是 `codeoj / codeoj123`**。
> 容器和代码两边必须一致，否则启动会报
> `ACCESS_REFUSED - Login was refused using authentication mechanism PLAIN`。两种做法任选其一：
>
> - **做法 A（本仓库现状）**：容器按上面带 `-e RABBITMQ_DEFAULT_USER=codeoj ...` 创建，
>   启动服务前一定要设环境变量（否则代码用 guest 去连 codeoj 容器，连不上）：
>
>   ```bash
>   # Windows PowerShell
>   $env:RABBITMQ_USER="codeoj"; $env:RABBITMQ_PASSWORD="codeoj123"
>   # macOS / Linux
>   export RABBITMQ_USER=codeoj; export RABBITMQ_PASSWORD=codeoj123
>   ```
>
> - **做法 B（更省事）**：容器创建时**不加** `-e` 参数（即默认 guest/guest），
>   代码也不用设环境变量，开箱即连。
>
> 判断当前容器用的什么账号：`docker inspect oj-rabbitmq | findstr RABBITMQ_DEFAULT`

### 0.4 怎么知道你的判题真的跑通了

**最简单的验证**：在浏览器打开前端（或直接用 curl），注册一个账号 → 提交一段代码 → 看返回的判题状态。

```bash
# 注册（拿 token）
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test1","password":"123456"}'

# 登录，把返回的 token 记下来
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test1","password":"123456"}'

# 提交一段 C++ 代码（把 <token> 换成上一步的）
curl -X POST http://localhost:8080/api/submissions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"problemId":1,"language":"cpp","code":"#include<iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<a+b;}"}'

# 几秒后查结果（<id> 是上一步返回的 submissionId）
curl http://localhost:8080/api/submissions/<id> -H "Authorization: Bearer <token>"
```

返回里 `status` 的数字含义：**0=排队中 1=判题中 2=通过 3=答案错误 4=超时 5=超内存 6=运行错误 7=编译错误 8=系统错误**。

> 现在 `ENGINE_MODE=local`，判的是"代码里有没有入口/有没有输出"这种静态检查。
> 你要做的是让它**真编译真运行**，见 §2。

### 0.5 卡住了看哪里

| 现象 | 先看 |
|---|---|
| 启动报连不上 MySQL/Redis | 三个 docker 容器起了吗？`docker ps` |
| 启动报 `ACCESS_REFUSED` | RabbitMQ 账号错了，见 §0.3 |
| 端口被占用 | `netstat -ano \| findstr :8080` 找到进程杀掉 |
| 提交后一直"排队中" | oj-judge 没启动，或没连上 RabbitMQ |
| 改了代码没生效 | `mvn clean install -DskipTests`（改 pom 后必须 clean） |
| 判题结果和你预期不符 | 看 `oj-judge` 控制台日志 |

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
| `judge-images/*` | 五镜像构建并验证：已提供 `judge-images/build.sh`（一键构建 + 五语言探针自检 + 镜像源回退），构建与验证方法见 [JUDGE-IMAGES.md](./JUDGE-IMAGES.md) |

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
| 五语言正确代码 | AC，耗时/内存有真实值 |
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
