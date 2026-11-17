# 判题安全测试（论文 6.3 / 表 6-2）

恶意代码样例库与两套自动化测试脚本，对应论文第 6 章安全测试：

| 脚本 | 依赖 | 作用 |
| --- | --- | --- |
| `security-sandbox-test.sh` | Linux + Docker Engine + 最新判题镜像 | **沙箱直测**：按当前 DockerSandbox 的 supervisor/降权/只读策略运行样例；断言资源限制与隔离 |
| `security-e2e-test.sh` | compose 栈运行（`ENGINE_MODE=real`）+ 判题镜像 + curl + python3 | **全链路测试**：经真实提交接口提交恶意代码，断言判题状态机映射正确 |

## 预期结果矩阵

| 编号 | 样例 | 沙箱直测断言 | 全链路预期状态 |
| --- | --- | --- | --- |
| S-01 | fork 炸弹（`c`） | `exit=124`（pids-limit 拦截后空转至超时） | TLE (4) |
| S-02 | 死循环（`c`） | `exit=124` 且程序墙钟时间 ≈ 1000ms | TLE (4) |
| S-03 | 内存炸弹（`c` / `python3`） | Docker `OOMKilled=true`，或有可信统计的内存超过题目限制 | MLE (5) |
| S-04 | 磁盘写入（`c`） | 运行阶段 `/work` 只读，`fopen` 失败返回 2；可写区另有 tmpfs/文件上限 | RE (6) |
| S-05 | 网络访问（`c`） | `exit=0`（样例自报告：公网/内网连接全部失败） | WA (3) ① |
| S-06 | 读取宿主机（`c`） | `exit=0`（/etc/shadow、docker.sock 等全部不可读） | WA (3) ① |
| S-07 | 提权尝试（`c`） | `exit=0`（setuid(0)/setgid(0) 被 EPERM 拒绝） | WA (3) ① |
| S-08 | 输出轰炸（`c`） | `exit=153`（SIGXFSZ）或 `trunc=1`（runner 16MB 截断） | RE (6) |
| S-09 | SQL 注入（应用层） | 不适用（沙箱层无意义） | 注入载荷代码正常判题 ∈ {AC,WA,CE}；搜索接口正常返回不泄露 |
| S-10 | 编译期攻击（`cpp`） | 可信统计超时，或 Docker 报告 OOM（编译限制） | CE (7) |

> ① S-05/06/07 样例采用"自报告"语义：隔离生效则程序正常跑完、输出与题面样例不匹配 → WA；
> 若隔离失效（网络连通/敏感文件可读/提权成功），程序 exit 1 → RE，测试立即暴露问题。

## 运行步骤

```bash
# 1. 在 judge-images/ 目录构建最新五语言判题镜像（Linux 环境）
docker build -f c/Dockerfile      -t codeoj/c:gcc13      .
docker build -f cpp/Dockerfile    -t codeoj/cpp:gcc13    .
docker build -f java/Dockerfile   -t codeoj/java:openjdk17 .
docker build -f python/Dockerfile -t codeoj/python:3.12  .
docker build -f node/Dockerfile   -t codeoj/node:20 .

# 2. 沙箱直测（不需要业务栈，先跑这个）
cd ../judge-tests
EVIDENCE_DIR=/tmp/codeoj-security-evidence bash security-sandbox-test.sh

# 3. 全链路测试（compose 栈 + ENGINE_MODE=real + 已导入题目）
PROBLEM_ID=1 BASE_URL=http://localhost:18080/api bash security-e2e-test.sh
```

## 环境变量

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `IMG_C/IMG_CPP/IMG_JAVA/IMG_PY` | `codeoj/c:gcc13` 等 | 沙箱直测使用的判题镜像 tag |
| `BASE_URL` | `http://localhost:8080/api` | 全链路测试的服务地址 |
| `PROBLEM_ID` | 自动取第一道题 | 全链路测试提交的目标题目（需允许对应语言且已配置用例） |
| `TEST_TOKEN` | 空 | 预发的独立测试学生 token，可跳过注册登录；不要写入日志 |
| `TEST_USERNAME` / `TEST_PASSWORD` | 自动新账号 | 复用已有测试账号时显式登录；注册与登录仍遵守正常 IP 配额 |
| `EVIDENCE_DIR` | 临时目录 | 沙箱直测日志存放处；指定外部目录时保留 stdout/stderr 便于复核 |

## 说明与已知边界

1. 直测验证资源与隔离，接口测试验证提交及终态映射。它们均不验证 MQ 重复投递、恢复、排行榜或 WS 收包，这些需要单独测试。
2. supervisor 保持 root 并仅有 SETUID/SETGID，用 `setpriv` 将用户程序和 timeout 降为 1000:1000。统计文件在独立 root tmpfs；编译工作目录可写、逐用例运行目录只读。`time_ms` 为程序墙钟时间，`cpu_ms` 仅辅助诊断。
3. 恶意样例仅在独立环境执行，准备步骤见 [独立复测](perf/README.md)。固定测试题须允许对应语言，网络/提权样例的自报告输出须与测试题期望不同。
4. 先运行正常程序冒烟：`TEST_TOKEN=... python3 smoke-normal.py --problem-id <每用例期望OK的题目ID>`，五语言应均为 AC，耗时/内存及用例明细应存在。此脚本不执行恶意程序。
5. 全部脚本为 bash + Python 标准库，不依赖 jq。测试脚本的存在不代表当前版本已经执行通过，见 [验证记录](联调报告.md)。
