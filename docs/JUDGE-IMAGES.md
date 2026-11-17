# 判题镜像构建与验证

> 对应交付项：`docs/HANDOFF-A.md:136`（判题镜像构建与验证）。
> 相关文件：`judge-images/build.sh`、`judge-images/runner.sh`、`judge-images/{c,cpp,java,python,node}/Dockerfile`。
> 关联文档：[JUDGE-HARDENING.md](./JUDGE-HARDENING.md)（沙箱加固）、[CONTRACT.md](./CONTRACT.md) §6（真实评测执行）。

## 1. 为什么单独有这一节

五个判题镜像**不在 Docker Hub 上，也不可能随 git 仓库分发**：镜像层是构建产物，合计约 6.3 GB。仓库里只有 Dockerfile 和 `runner.sh`。

由此产生一个只在真实判题时才暴露的断链：

| 环节 | 缺镜像时的表现 |
|---|---|
| 服务启动 | 正常。`oj-judge` 不校验镜像是否存在 |
| 提交入库 | 正常。写成 `QUEUING`/`QUEUED` |
| MQ 投递与消费 | 正常。租约、token、租约续期全部走通 |
| **真实判题** | **失败**。docker-java 抛 `No such image`，被 `DockerSandbox` 兜底成 `SYSTEM_ERROR`（`status=8`） |

演示数据（`sql/seed-judge-case-demo.sql`、`tools/seed_demo_problems.py`）直接把 `status=AC` 的提交插进数据库，不经过沙箱，所以**页面看起来一切正常**。这使得缺口在开发和自测阶段完全隐形 —— 只有真跑一次判题才会发现。

`build.sh` 把「构建 + 自检」固化，让新环境与答辩现场可以确定性复现。

## 2. 前置条件

| 项 | 要求 |
|---|---|
| Docker | Docker Engine（Linux）或 Docker Desktop（Windows/macOS），守护进程可用 |
| 磁盘 | 约 8 GB（镜像 6.3 GB + 构建中间层） |
| 网络 | 能拉取 `gcc:13-bookworm`、`eclipse-temurin:17-jdk-jammy`、`python:3.12-slim`、`node:20-bookworm-slim` |
| 工作目录 | 必须在 `judge-images/` 下执行（脚本会自动 `cd` 到自身目录） |

## 3. 用法

```bash
cd judge-images

./build.sh                    # 构建五个镜像并逐个自检（推荐）
./build.sh cpp java           # 只构建并自检指定语言
./build.sh --verify-only      # 只自检已有镜像，不构建
./build.sh --no-verify        # 只构建，不自检
./build.sh --no-cache         # 忽略构建缓存
```

退出码：`0` 全部成功；`1` 任一镜像构建或自检失败（脚本会打印失败项与排查建议）。

### 3.1 国内网络

Docker Hub 直连不稳定时，用环境变量指定镜像站，**不必修改仓库里的 Dockerfile**：

```bash
# 基础镜像走镜像站
BASE_REGISTRY_MIRROR=registry.cn-hangzhou.aliyuncs.com ./build.sh

# 同时替换镜像内的 apt 源
BASE_REGISTRY_MIRROR=registry.cn-hangzhou.aliyuncs.com \
APT_MIRROR=mirrors.aliyun.com ./build.sh
```

脚本通过临时 Dockerfile 覆盖 `FROM` 与 apt 源，构建结束后销毁，个人网络配置不会进入版本库。

### 3.2 Windows + Git Bash

脚本已内置 `MSYS_NO_PATHCONV=1` 与 `cygpath` 路径转换，可直接在 Git Bash 里用 Docker Desktop 构建。若用 PowerShell，请显式调用 Git Bash：

```powershell
& "C:\Program Files\Git\bin\bash.exe" ./build.sh
```

## 4. 脚本做了什么

### 4.1 预检（构建前失败，而不是构建到一半失败）

- `docker` 命令存在、`docker info` 可用（区分「没装 Docker」与「守护进程没起」）
- `runner.sh` 存在
- **`runner.sh` 不含 CRLF**。CRLF 会让容器内报 `bad interpreter: /bin/bash^M`，表现为所有提交都是系统错误。仓库 `.gitattributes` 已锁 LF，脚本再兜一层
- 五个 Dockerfile 存在

### 4.2 构建

构建上下文是 `judge-images/` 目录本身（`runner.sh` 在根上，被各 Dockerfile `COPY`）。tag 与 `oj-judge/.../core/strategy/*Strategy.java` 的 `image()` 返回值严格一致：

| 语言 | tag | 来源 |
|---|---|---|
| C | `codeoj/c:gcc13` | `CStrategy.image()` |
| C++ | `codeoj/cpp:gcc13` | `CppStrategy.image()` |
| Java | `codeoj/java:openjdk17` | `JavaStrategy.image()` |
| Python3 | `codeoj/python:3.12` | `Python3Strategy.image()` |
| JavaScript | `codeoj/node:20` | `JsStrategy.image()` |

tag 不一致时判题会报 `No such image`，全部落到 `status=8`。

### 4.3 自检（关键差异）

构建成功 ≠ 镜像可用。自检在**与 `DockerSandbox.buildHostConfig` 同构的限制集**下（只读根、`--network none`、`--cap-drop ALL` + `SETUID/SETGID`、`no-new-privileges`、pids/nofile/fsize 上限、`/run/codeoj` tmpfs），跑一次完整的：

```
写源码 → 编译（编译型语言）→ runner.sh 降权执行 → 解析统计协议行
```

断言三件事：

1. 标准输出等于期望值（探针是 `3 4` → `7`）
2. 统计行报告 `exit=0`
3. `###CODEOJ_STAT###` 协议行存在（验证 `runner.sh` 真的跑起来了，而不是绕过了它）

**只验证「runner.sh 与工具链共存」**，不验证恶意样本拦截 —— 那是 `judge-tests/security-sandbox-test.sh` 的职责，两者不重叠。

自检通过的实际输出（本机实测）：

```
自检通过（输出 7，exit=0 time_ms=10  cpu_ms=0  mem_kb=1584  trunc=0）   # c
自检通过（输出 7，exit=0 time_ms=20  cpu_ms=0  mem_kb=3312  trunc=0）   # cpp
自检通过（输出 7，exit=0 time_ms=80  cpu_ms=70 mem_kb=36000 trunc=0）   # java
自检通过（输出 7，exit=0 time_ms=50  cpu_ms=10 mem_kb=9360  trunc=0）   # python
自检通过（输出 7，exit=0 time_ms=210 cpu_ms=60 mem_kb=37728 trunc=0）   # node
```

## 5. 构建后必须做的事

镜像构建完只说明**镜像可用**，不等于**链路可用**。还需：

1. 启动 `oj-judge` 且 `ENGINE_MODE=real`（`local`/未知模式一律 `SYSTEM_ERROR`，`mock` 只供联调）
2. 确认 `oj.judge.work-dir-root`（默认 `/judge/work`）在**宿主**与判题服务容器内是同一绝对路径，否则沙箱兄弟容器挂载到错误目录
3. 用真实账号提交一道题，确认结果是 `AC` 而不是 `status=8`
4. 按这次真实提交的编号，核对 API 终态、用例明细与资源统计；秒级时间戳相等不能据此判断演示数据

验证 SQL：

```sql
-- 最近判题记录：字段只精确到秒，真实短程序也可能同秒提交与完成。
SELECT id, status, created_at, judged_at, TIMESTAMPDIFF(SECOND, created_at, judged_at) AS sec
FROM submission ORDER BY id DESC LIMIT 10;

-- 系统错误计数；正常环境应接近 0，缺镜像时会持续增长
SELECT status, COUNT(*) FROM submission GROUP BY status;
```

## 6. 常见故障

| 现象 | 原因 | 处理 |
|---|---|---|
| 提交全部 `status=8`，日志 `No such image` | 镜像没构建，或 tag 与 Strategy 不一致 | 跑 `./build.sh`；用 `docker images --filter reference=codeoj/*` 核对 tag |
| 容器内 `bad interpreter: /bin/bash^M` | `runner.sh` 被 CRLF 化 | `dos2unix judge-images/runner.sh`（或 `git config core.autocrlf false` 后重新 checkout） |
| `docker build` 卡在 `FROM` 拉取 | 直连 Docker Hub 超时 | 设 `BASE_REGISTRY_MIRROR` 用镜像站 |
| `apt-get update` 报 `Temporary failure resolving` | 镜像内 DNS/网络不通 | 设 `APT_MIRROR` 换国内源 |
| 自检报 `working directory 'C:/...' is invalid` | Git Bash 路径转换 | 脚本已处理；若自行调用 `docker run` 请设 `MSYS_NO_PATHCONV=1` |
| 自检报「未输出统计协议行」 | `runner.sh` 没跑起来或被 `set -e` 中断 | `docker run -it --entrypoint bash codeoj/c:gcc13` 进去手工执行 `/judge/runner.sh` 排查 |
| 编译型语言自检失败、解释型通过 | 编译器缺失或基础镜像下拉错版本 | `docker run --rm codeoj/c:gcc13 gcc --version` 核对 |

## 7. 与论文的对应

| 论文位置 | 本脚本的支撑 |
|---|---|
| 5.2 表 5-2（五语言编译/执行命令） | 自检探针复现同一组命令 |
| 5.4 内存测量口径 | 自检输出 `mem_kb`，验证 `runner.sh` 的 `/usr/bin/time -M` 统计可用 |
| 5.8 部署实现 | README 第 5 步 + 本文第 5 节 |
| 6.3 沙箱测试 | 自检用同构限制集，与 `judge-tests/security-sandbox-test.sh` 互补 |
