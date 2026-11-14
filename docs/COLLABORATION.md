# CodeOJ 双人分工与协作规范

> 版本：v1.0 ｜ 配套文档：[API.md](./API.md)（前后端契约）、[CONTRACT.md](./CONTRACT.md)（后端两模块契约）

## 1. 分工方案（✅ 已确定：方案 A，按功能纵切）

> **决定记录（2026-11-14）**：本项目采用方案 A。
> 成员 B（huayuehua15-netizen）：**业务与前端**——oj-server 全部业务实现、oj-web 全部页面、联调、功能与性能测试。
> 成员 A：**评测链路**——oj-judge 判题引擎/沙箱/可靠性、judge-images、判题正确性与安全测试。
> 边界澄清：**oj-server 代码全部归成员 B**（含排行榜读侧用户信息补齐、队列监控读侧）；成员 A 只动 oj-judge、judge-images、deploy 中判题相关配置。oj-common 改动仍需双方同意。

### 方案 A：按功能纵切（已采用）

一人一条完整业务线，对接面小、两人都接触核心技术，答辩时各有可讲的内容。

| | 成员 A：**评测链路** | 成员 B：**业务与前端** |
|---|---|---|
| 负责 | oj-judge（判题引擎/沙箱/可靠性）、judge-images、docker-java、判题正确性与安全测试 | oj-server 业务实现（记录/统计/管理端）、oj-web 全部页面、联调、功能与性能测试 |
| 主战场 | 论文 §5.4、§6.3 安全测试 | 论文 §5.2/5.5/5.6/5.7、§6.2/6.4 |
| 对接依赖 | API.md §0 通用约定 | API.md §4/§7（提交与推送）、CONTRACT.md §1/§2（入队与状态机） |

### 方案 B：按层横切

| | 成员 A：后端 | 成员 B：前端 |
|---|---|---|
| 负责 | oj-server + oj-judge + SQL + 部署 + 测试 | oj-web 全部页面 + 联调 |
| 对接面 | —— | 唯一对接面 = API.md |

> 对接规则：**跨成员的接口以 docs/ 文档为准，改动必须先改文档并同步对方**（API.md §9 / CONTRACT.md §8 变更登记表）。

## 2. 里程碑（第二阶段，M1 先做通最小闭环）

### M1 · 联调基线（最小闭环，最先打通）

- A：数据库就绪、oj-server/oj-judge 本地可启动、MQ 拓扑自动声明、提交入队 + **假判题**（消费者直接回写 AC）打通全链路
- B：登录/注册页、题目列表/详情页、提交结果页（状态机 + 兜底轮询，不依赖 WS 也能走通）
- **验收**：提交一次代码，10 秒内看到"通过"；API.md 联调清单 L1~L4 通过

### M2 · 判题真实化

- A：DockerSandbox 实现（帧流解析、资源测量、双重超时）、JudgeEngine 编译/运行/比对编排、CE/WA/TLE/MLE/RE 判定、启动扫描验证
- B：WebSocket 实时推送接入、判题详情页用例级表格、提交记录页
- **验收**：四语言 AC 各 1 例；死循环→TLE、内存炸弹→MLE、语法错误→CE；WS 推送 <1s 到达

### M3 · 业务完整化

- A：排行榜 ZSet + 用户表补齐、队列监控心跳、限流/幂等/黑名单全量验证
- B：个人统计页、排行榜页、管理端四页（题目表单+Markdown 预览、用例批量导入文本解析器、课程、监控）
- **验收**：API.md L5~L7 全部通过；管理端可完整维护题目与用例

### M4 · 测试与部署（对应论文第 6 章）

- 共同：恶意代码样例库 10 例（论文表 6-2）、JMeter 并发（表 6-3 指标复测）
- A：judge-images 四镜像构建、`docker compose up` 一键部署、`--scale oj-judge` 扩容验证
- B：测试报告整理、截图与数据归档（论文图表素材）

## 3. Git 协作规范

```bash
main            # 可运行版本，只进经过验证的合并
├── dev         # 集成分支，两人各自分支向它合并
│   ├── feat/judge-engine    # 成员 A
│   └── feat/web-pages       # 成员 B
```

- **分支命名**：`feat/模块-功能`、`fix/模块-问题`、`docs/文档主题`
- **Commit 格式**：`<type>(<scope>): <一句话>`，如 `feat(judge): 沙箱帧流解析与16MB截断`
- **合并纪律**：不直接 push main/dev；跨契约改动（API.md/CONTRACT.md 涉及的代码）合并前必须让对方过一眼 diff
- **o-common 是公共区**：改实体/枚举/常量 = 改契约，**必须双人都同意**并更新 CONTRACT.md

## 4. 本地联调环境

### 4.1 端口分配（固定，勿改）

| 服务 | 端口 | 启动人 |
|---|---|---|
| oj-server | 8080 | 成员 A |
| oj-judge | 8081 | 成员 A |
| oj-web dev | 5173 | 成员 B（Vite 已代理 /api、/ws → 8080） |
| MySQL 8.0 | 3306 | 共用（docker 启动） |
| Redis 7 | 6379 | 共用 |
| RabbitMQ | 5672 / 15672 管理台 | 共用 |

### 4.2 启动顺序

```bash
# 中间件（各自机器相同）
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql:8.0
docker run -d -p 6379:6379 redis:7
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
mysql -uroot -p123456 < sql/schema.sql

# A 启动后端（先 server 后 judge）
mvn spring-boot:run -pl oj-server
mvn spring-boot:run -pl oj-judge

# B 启动前端
cd oj-web && npm install && npm run dev   # http://localhost:5173
```

### 4.3 环境变量约定

后端全部支持环境变量覆盖（见两模块 application.yml），本地默认值即上述端口与密码；**提交代码不写死机器特定配置**。

## 5. 里程碑验收用的通用测试账号

| 用途 | 操作 |
|---|---|
| 学生账号 | 注册接口创建（role=0） |
| 教师账号 | 注册后 `UPDATE user SET role = 1 WHERE username = 'xxx';` |
| 种子题目 | schema.sql 自带「A+B Problem」（含 2 组样例用例） |

## 6. 沟通纪律

1. **契约变更三步走**：改文档（登记版本）→ 知会对方 → 再改代码；
2. 每天下班前 dev 分支互相合并一次，冲突当天解决；
3. 联调问题按 API.md §8 联调清单编号报障（如"L3 推送不到"），不模糊描述；
4. 骨架中标注 ⚠️ 的接口是"契约已定、实现待补"——前端可直接按文档 mock 开发，不必等后端。
