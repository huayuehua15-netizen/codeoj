# 独立环境性能复测

脚本只使用 Python 标准库。负载阶段读取预发 token，不注册、不登录、不清限流键；正常业务的注册、登录及提交限流全部保留。

## 准备隔离环境

以下命令在仓库根目录执行。`docker-compose.perf.yml` 使用独立 MySQL 数据卷、Redis、RabbitMQ、队列、网络和 `/judge/perf-work`，API 只绑定本机 18080；默认判题并发为 1，先做单用户冒烟。不要让普通栈连接该测试库。

```bash
# 测试专用短期密钥，不复用开发/生产密钥；当前 shell 内保留即可。
export PERF_JWT_SECRET="perf-test-$(openssl rand -hex 24)"
export PERF_DATABASE=codeoj_perf_run
python3 judge-tests/perf/perf-fixtures.py --database "$PERF_DATABASE" --users 600 \
  --hours 2 --output-dir judge-tests/perf/.local-fixtures --isolated-test-environment

# 工具只生成 SQL，compose 的新测试数据卷首次启动时才导入它们。
docker compose -p codeoj-perf -f deploy/docker-compose.perf.yml up -d --build
export PERF_ADMIN_TOKEN=$(python3 -c 'import json;print(json.load(open("judge-tests/perf/.local-fixtures/perf-admin.json"))["token"])')
python3 judge-tests/perf/perf-prepare.py --base http://localhost:18080/api \
  --problems 100 --cases 2 --output judge-tests/perf/perf-problems.json --isolated-test-environment
```

首次启动前需构建 `judge-images` 五语言镜像，命令见仓库 README。token 包含 `sub/role/jti/iat/exp`，签名算法按 JJWT 的密钥长度规则选择 HS256/384/512；预建账号禁用密码登录，仅供该测试栈使用。fixture 文件带有效凭据，保存在被 Git 忽略的 `.local-fixtures/`，不要归档 token、密钥或账号文件。已存在的数据卷不会重新执行初始化 SQL；复测时保持原密钥及账号对应关系，或另建测试项目与数据库，不自动删除已有卷。

## 执行复测

```bash
# 先验证脚本、鉴权和真实判题。单次正常代码，不属于并发压测。
python3 judge-tests/perf/perf-load-test.py --base http://localhost:18080/api \
  --users 1 --submits 1 --think 15 --max-duration 120 \
  --tokens-csv judge-tests/perf/.local-fixtures/perf-tokens.csv \
  --problems-file judge-tests/perf/perf-problems.json \
  --output judge-tests/perf/perf-report-smoke.json --isolated-test-environment

# 确认宿主资源和并发配置后，分开运行 100/300/500 三档，每档等待队列清空。
python3 judge-tests/perf/perf-load-test.py --base http://localhost:18080/api \
  --users 100 --submits 6 --think 15 --max-duration 3600 \
  --tokens-csv judge-tests/perf/.local-fixtures/perf-tokens.csv \
  --problems-file judge-tests/perf/perf-problems.json \
  --output judge-tests/perf/perf-report-100u.json --isolated-test-environment
```

token 生命周期应覆盖整档时长。默认提交间隔 15 秒保留每用户 6 次/分的限制。若出现 429，应记录失败并调低测试速率，不删除限流逻辑。fixture 准备和题目创建不计入负载统计。

## 指标口径

| 字段 | 定义 |
| --- | --- |
| `submitAvgMs` / `submitP50Ms` / `submitP99Ms` | 全部已尝试提交的 HTTP 耗时；mean 为算术平均，分位用 `(n-1)*p` 的线性插值 |
| `turnaroundAvgMs` / `turnaroundP99Ms` | 已完成记录的 `judgedAt-createdAt`；缺时间戳不填 0，计入终态数但不计入该样本量 |
| `errorRatePct` | HTTP/业务拒绝的提交数 ÷ 已尝试提交数；与判题非 AC 率分开报告 |
| `acRatePct` / `statusCounts` | 预期输出 OK 的代码在已完成记录中的 AC 比例与全部终态分布 |
| `throughputPerSec` | 观测完成数 ÷ 整个测试墙钟时长，包含思考与轮询开销；不是纯引擎服务率 |
| `unfinishedIds` / `notAttempted` | 全局 deadline 后未终态的已受理 ID，以及尚未提交的计划数 |
| `queueSamples` | 可选管理员监控采样；无采样时峰值为 null，不能据此宣称积压为 0 |

报告包含逐请求 `samples`、队列采样、生成时间、提交号与工作区是否有修改，可重算统计。无样本的统计为 null。脚本在每次请求与等待前检查同一 deadline，并限制请求 socket timeout；卡住任务仍会产出未完成 ID，退出码为 1。非 AC 终态必须单独调查，不能以“HTTP 零错误”宣称判题完全正确。WS 延迟未纳入该脚本，需另外测量。

归档时保留脱敏报告、镜像 ID、Docker/内核版本、CPU/内存、并发槽位、提交代码及工作区 diff 的哈希；禁止将密钥和 token 入库。旧报告的统计口径与新版不同，不能直接作前后性能比较。

## 脚本单元测试

```bash
python3 -m unittest discover -s judge-tests/tests -v
bash -n judge-tests/security-sandbox-test.sh
bash -n judge-tests/security-e2e-test.sh
```
