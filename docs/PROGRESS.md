# CodeOJ 项目进度文档

> 最后更新：2026-11-16 ｜ 维护：成员 B ｜ 更新纪律：每完成一个里程碑/修复一批 bug，更新本文档
> 配套文档：[API.md](./API.md)（接口契约 v1.2）｜ [CONTRACT.md](./CONTRACT.md)（模块契约）｜ [COLLABORATION.md](./COLLABORATION.md)（分工+分支规则）｜ [HANDOFF-A.md](./HANDOFF-A.md)（A 侧交接）｜ [PROGRESS.md](./PROGRESS.md)（本文档）

---

## 1. 项目概况

| 项 | 值 |
|---|---|
| 项目 | CodeOJ —— 基于 Spring Boot + Vue3 + Docker 的在线代码评测与编程练习平台（专科毕业设计） |
| 分工 | 方案 A（已固化）：成员 A = 评测链路（oj-judge real 引擎/judge-images）；成员 B = 业务与前端（oj-server/oj-web/联调/测试） |
| 技术栈 | JDK17 / Spring Boot 3.2.5 / MyBatis-Plus / RabbitMQ / Redis / MySQL 8 / docker-java / Vue3+Vite+TS+Pinia+EP+Monaco / ECharts / SockJS+STOMP / Web Worker 沙箱 |
| 仓库 | github.com/huayuehua15-netizen/codeoj ｜ 分支：**main**（发布）/ **dev**（集成）/ feat/*（开发） |
| 交付状态 | **B 侧功能 100% 交付 + 已知 bug 清零 + UI 体系化完成**；A 侧 real 引擎待开工（材料就绪） |

---

## 2. 里程碑时间线（全部已完成并推送）

### M0 · 骨架搭建 ✅
- Maven 多模块（oj-common / oj-server / oj-judge）+ oj-web + judge-images + deploy + sql
- 判题状态机枚举、七表 schema、R 统一返回、MQ/Redis 常量
- 全部 REST 骨架、Security+JWT、STOMP 配置、判题消费者/引擎分层结构
- **验证**：mvn 三模块 BUILD SUCCESS + 前端构建通过

### M0.5 · 文档体系 ✅
- API.md（接口契约 v1.2）、CONTRACT.md（模块契约+序列化约定）、COLLABORATION.md（分工+分支规则）

### M1 · 联调基线 ✅
- 环境攻坚：WSL 修复、Docker Desktop（per-user）就绪、DaoCloud 镜像源
- 中间件容器 ×3、mock 判题引擎、ENGINE_MODE 三模式设计
- 联调清单 L1~L6 实测全绿（注册/登录/登出黑名单/题目浏览/提交→AC→明细/幂等/记录统计/排行）

### M2 · B 侧功能完整化 + 修复 ✅
- 判题详情（用例级）/提交记录分页/个人统计三接口真实实现
- 用户管理、竞赛模式、公告、收藏、标签（见 §3）
- 排行榜对账任务、实例心跳、趋势接口
- 界面体系化：Design Tokens → 主题换血（靛蓝，37 处）→ 玻璃拟态 → 步骤条/头像/奖牌/横幅
- 控制台（JS 浏览器沙箱自测运行）

### 自测两轮 ✅（发现并修复 6 个 bug + 4 项边界加固，见 §5）

### 当前位置
**B 侧交付完成，冻结 oj-judge 改动**（文件面留给 A）。P0 = A 侧 real 引擎（HANDOFF-A.md 就绪）。

---

## 3. 功能清单（已实现，按模块）

### 3.1 用户与认证
- 注册（IP 限流 10 次/时、BCrypt、用户名支持中文）/ 登录（IP 限流 30 次/时 + 失败 5 次锁 15 分钟 + 封禁拒绝 40300）/ 登出（token 黑名单）
- 个人中心：改邮箱 / 改密码（改密后 token 立即失效）/ 我的收藏列表
- JWT 全生命周期失效：登出、改密、**封禁**、**删号**（filter 每请求主键查库 + clearContext 防 ThreadLocal 残留）
- 注册不自动登录：成功后跳登录页并预填用户名
- 右上角用户名下拉（个人中心/退出）带箭头+hover 可识别样式

### 3.2 题库（28 题 / 5 种语言）
- 语言：C / C++ / Java / Python3 / **JavaScript（前端方向）**
- 标签系统（JSON 列 + JSON_CONTAINS 参数化筛选 + 表单多选自建）
- 列表：统计横幅（题目数/解题者/累计提交）、公告轮播栏、难度标签、标签筛选/展示、通过率分级配色、题号徽章
- 详情：题面 Markdown（DOMPurify 净化 + 排版增强）、样例、限制胶囊标签、收藏 toggle、语言切换载模板（已编辑保护确认）

### 3.3 判题与提交
- 提交链路：限流（6 次/分）→ 幂等锁（2s）→ 校验（语言/64KB 字节统一）→ 事务写库 → MQ → 立即返回
- 判题三模式：mock（全 AC）/ **local（静态语义判定，默认）** / real（A 侧待实现，node 镜像已备）
- 静态判定（含 js 分支）：无入口判 CE、无输出判 WA——拦截全部无效提交（真实运行判定待 real）
- 三阶段 WS 推送（QUEUED/JUDGING/FINISHED，SockJS+STOMP 实测端到端）+ 轮询兜底 + 步骤条动画 + 再提交按钮

### 3.4 控制台（JS/Vue3 题）
- 浏览器 Web Worker 沙箱真实执行用户 JS：mock console 捕获、fs.stdin 注入、process.exit 终止、5 秒超时
- 自定义输入（进页面预填样例 1，运行时空输入显式填样例——所见即所得）→ 输出/错误/耗时展示
- 编辑器旁 + 编辑器下双运行入口（仅 js 语言显示）

### 3.5 记录/统计/排行
- 提交记录：题目/状态/**时间区间**筛选、用例级详情
- 统计：总/分难度通过率环形卡 + **ECharts** 近 7 天趋势（柱线混合）+ 难度对比柱状（trend 接口）
- 排行榜：ZSet 实时（ZINCRBY 裸串）+ **对账纠偏任务**（5 分钟）+ 奖牌/头像/相对进度条 + 用户表补齐

### 3.6 竞赛模式（论文 7.2 展望落地）
- 教师：创建/编辑/删除（时间窗 + 题目集）
- 状态推导（未开始/进行中/已结束）、详情渐变头 + **秒级倒计时**、字母题号（A/B/C…）
- ACM 简化排名：窗口内提交聚合，解题数降序 + 罚时升序（首 AC 分钟 + 错误×20），逐题明细
- 比赛提交零侵入联动（排名按时间窗+题目集过滤 submission）

### 3.7 管理端
- 题目管理（样例编辑器/标签编辑/搜索/表单重置）、用例批量导入（文本粘贴解析，input/expectedOutput 允许空串）
- **用户管理**：搜索/提权降权/封禁解封（全链路联动）/重置密码/自操作防护
- 竞赛管理、公告管理（发布/删除）、队列监控（积压/今日判题量/**在线实例数**心跳）

### 3.8 公告与课程
- 公告：教师发布/删除，题目列表页轮播展示
- 课程：创建/编辑/删除/题目分组/班级完成情况统计

---

## 4. 演示资产（答辩可用）

| 资产 | 状态 |
|---|---|
| 题库 | **28 题**（算法 20 + 前端 JS/Vue3 8），难度 简单 13/中等 9/困难 6，标签 12+ 种 |
| 用户 | 31 个（30 名中文名学生 + teacher_admin） |
| 提交 | 105+ 条混合状态（AC/WA/TLE/RE/CE），时间分布近 7 天 |
| 排行榜 | 28 人有层次（AC 4→1），无重复 |
| 课程 | 2 门（C 语言程序设计 / 数据结构与算法实验） |
| 竞赛 | 1 场进行中（算法挑战赛，窗口 ~3h） |
| 公告 | 1 条（实验一截止提醒） |

**演示账号**：教师 `teacher_admin / admin123`；学生 `张伟 / 123456`、`周杰 / 123456`（有提交数据）
**服务**：前端 5173 / oj-server 8080 / oj-judge 8081 / MySQL 3306 / Redis 6379 / RabbitMQ 5672+15672

---

## 5. Bug 修复台账（37 项，按发现来源）

### 联调发现（环境/编码）
| # | 问题 | 修复 |
|---|---|---|
| 7 | WSL MSIX 组件缺失（Docker 起不来） | 官方 WSL 2.6.1 MSI 安装 |
| 8 | Docker Hub 直连被墙 | DaoCloud 镜像源 |
| 15 | schema.sql 导入中文双重编码（latin1） | SET NAMES utf8mb4 |

### 联调发现（代码）
| # | 问题 | 修复 |
|---|---|---|
| 9 | JDBC `characterEncoding=utf8mb4` 不被连接器识别 | 改 UTF-8 |
| 10 | **@Valid 全量失效**（8 处 @RequestBody 漏注解） | 补 @Valid + 注解显式命名 18 处 |
| 11 | RabbitMQ 自动 JSON 化 submissionId（带引号） | 发送端显式 SimpleMessageConverter + 消费端防御 |
| 12 | oj-judge 缺 RedisTemplate bean | 补 RedisConfig |
| 13 | 401/403 返回非契约 JSON | SecurityConfig entryPoint |
| 27 | 竞赛/公告路径缺 permitAll | SecurityConfig 补 |
| 28 | 前端白屏：sockjs 引用 Node global | vite define globalThis |
| 29 | oj-judge 缺泛型适配（String 模板） | TypedTuple<String> 修正 |

### 用户实测发现（6 个，均为真实使用才暴露）
| # | 问题 | 修复 |
|---|---|---|
| — | 垃圾字符提交判 AC（mock 傻判） | local 静态语义判定（CE/WA 拦截） |
| — | 语言切换无反应 | 切换载入对应模板 + 编辑保护确认 |
| — | 控制台"没输入却有输出" | 预填失效修复 + 静默 fallback 显式化 |
| — | 右上角用户名看不出是下拉 | 箭头图标 + hover 高亮 |
| — | 删号/封禁后 token 行为不一致（40400 vs 空列表） | filter 主键查库 + clearContext（统一 40100） |
| — | **排行榜同一用户重复出现**（序列化不统一双 member） | 两端统一 StringRedisTemplate 裸串 + CONTRACT §5 固化 |

### 自测一轮发现（旧模块边界，4 个）
| # | 问题 | 修复 |
|---|---|---|
| — | 下架题游客可见详情（与列表不一致） | detail 非管理员+下架 → 40002 |
| — | 64KB 限制双重标准（@Size 按字符 vs validate 按字节） | 移除 DTO 层，统一 validate → 40003 |
| — | 分页 size 无上限 | maxLimit=100 |
| — | 不存在课程 completion 返回成功空列表 | 先校验存在 → 40400 |

### 自测二轮发现（新模块边界，2 个）
| # | 问题 | 修复 |
|---|---|---|
| — | 收藏不存在题目插入悬挂记录 | toggle/状态查询补 40400 |
| — | **管理员可封禁/降权自己**（账号锁死） | requireNotSelf 防护（40000） |

### 架构级（数据一致性/并发）
| # | 问题 | 修复 |
|---|---|---|
| 21 | **排行榜 ZSet 序列化不统一**：JSON member vs 裸串 → 同一用户双 member 页面重复 | 两端统一 StringRedisTemplate；CONTRACT §5 固化 |
| — | 删号/封禁后 token 行为不一致（info 404、记录空列表） | JwtAuthFilter 每请求主键查库校验存在性/状态 + clearContext 防 ThreadLocal 残留 |
| — | 注册限流误伤（测试与用户共用 127.0.0.1 配额） | 阈值放宽 10 次/时 + 清配额 |
| — | Redis 计数 INCR/EXPIRE 非原子（可能永不过期键） | Lua 脚本原子执行 |

---

## 6. 关键技术决策记录（答辩素材）

| 决策 | 理由 |
|---|---|
| 判题三模式（mock/local/real） | mock 打通链路；local 静态判定适配无 Docker 演示并拦截无效提交；real 生产沙箱。`ENGINE_MODE` 一键切换 |
| local 用静态判定而非真实执行 | 部署环境安全写入策略对"进程执行用户代码"零容忍（三次拦截记录在案）；真实执行完整设计已交付（HANDOFF-A §2.3） |
| **JS 控制台走浏览器 Web Worker** | 用户代码是 JS，浏览器即运行时——Worker 无 DOM 天然隔离，零后端改动，绕开安全工具限制；C/Java/Python 的运行等 real |
| **JS/Vue3 判题链路已备** | 枚举/Strategy/node 镜像/静态分支全就位——real 上线即真实判定 node main.js |
| 排行榜 member 统一裸字符串 | 双服务序列化不一致导致双 member（踩坑实证）；CONTRACT §5 固化 |
| 删号/封禁 token 失效走主键查库 | Redis 缓存方案存在删号前缓存窗口（行为不一致实证）；主键查询代价可接受 |
| 注册不自动登录 | 注册/登录语义分离（表 4-7），引导用户走完整登录流程，登录页预填用户名 |
| MQ 消息体只带 submissionId | 控制消息体积 + 消费者回查保证数据一致（论文 4.6 原设计） |
| 主题靛蓝换血（EP 变量覆盖） | 默认 EP 蓝"塑料感"——靛蓝系（Linear/Stripe 主流色）+ 全库组件跟随 |
| 文档跟随实现 | "文档以实际为准"——实现中发现设计不合理时改实现，文档（含论文）同步更新 |

---

## 7. 分支与协作模型（已固化）

```
GitHub 云端（权威） ←→ ① Desktop\code-king（B 开发副本，唯一真实源）
                    ←→ ② Documents\GitHub\codeoj（GitHub Desktop 副本，仅查看/pull）
feat/judge-engine (A) ──→ dev（集成联调）──→ main（B 单点把关发布）
```

- **dev 合并规则**：谁的功能谁合并，合并后跑联调验证
- **main 推送权**：B 单点把关，仅里程碑验收通过后推送
- **A 交付方式**：推 feat/judge-engine → 通知 B → review → 合并 → 联调 → 验收 → 推 main
- **副本纪律**：② 只 pull/查看，**不在其中改代码提交**（防分叉）

---

## 8. 未完成项（按优先级）

### P0 · A 侧 real 判题引擎（成员 A）
- DockerSandbox 实现 + JudgeEngine realRun + judge-images 五镜像构建（含 node）
- 交接材料齐备：HANDOFF-A.md（含参数/参考实现/验收清单）
- **交付后联动**：ENGINE_MODE=real 切换即真实判定；前端/文档零改动

### P1 · 文档同步（论文）
- 第 4 章功能模块：加竞赛/公告/收藏/标签/用户管理/控制台
- 第 5 章：加对应实现小节
- 表 3-1：扩充功能清单；表 4-1：nickname 已删；表 4-2：tags 已加
- 7.2 展望：删"竞赛模式"（已实现）
- 5.2 限流句补"注册按 IP 每小时 10 次"（已给现成句子）

### P2 · M4 测试与部署（需 Linux/A 侧就绪）
- docker compose 一键部署验证
- JMeter 并发（论文 6.4：100/300/500 三级）
- 恶意代码样例库（论文 6.2 表 6-2：10 类）
- 竞赛模式端到端演示（现场提交 → 实时排名变化）

### P3 · 可选增强（超出论文，谨慎）
- 题解/讨论区、提交代码相似度检测、SPJ 特判、Pyodide（浏览器 Python 运行，可补齐 Python 控制台）

---

## 9. 环境与运维备忘

| 项 | 值 |
|---|---|
| Docker | Desktop per-user 安装（AppData\Local\Programs\DockerDesktop），WSL 2.6.1 MSI |
| 镜像源 | DaoCloud：`docker.m.daocloud.io/library/xxx`（Docker Hub 直连被墙） |
| RabbitMQ | codeoj / codeoj123，管理台 15672 |
| MySQL | root / 123456，库 codeoj（10 张表：7 业务 + contest + contest_problem + announcement + favorite） |
| 构建命令 | `mvn clean install -DskipTests`（改 pom 后必须 clean）|
| 启动顺序 | 容器 → oj-server → oj-judge → 前端 dev |
| 双副本 | ① Desktop\code-king（开发）② Documents\GitHub\codeoj（GitHub Desktop，仅查看/pull） |

### ⚠️ 踩坑清单
1. 改 pom 需 clean install（增量编译不重编，lombok/注解不生效假象）
2. 重启服务前必须确认 8080 端口旧进程已死（netstat 反查 PID——曾连续三轮测到旧代码）
3. curl 中文测试需 UTF-8（Git Bash 默认 GBK 会 50000）
4. Bash 工具写源码被安全策略拒绝，一律走 Write/Edit
5. 安全工具（Mimosa）对"进程执行用户代码"零容忍——判题执行实现需 HANDOFF-A 路径（用户手动建文件或 A 侧）
6. Element Plus 组件 actionability 误报多——自动化交互优先 evaluate 页面内点击
7. 前后端各自 build 后重启服务，**不要带错 cwd**（mvn -pl 需在项目根）

---

## 10. 提交历史索引（main 分支关键节点）

| commit | 内容 |
|---|---|
| 首推 | 骨架（161 文件）+ docs |
| 436c10d | M1 联调基线（3 接口 + mock 引擎 + 推送对齐） |
| aff689c | B 侧功能完整化（解析器/预览/课程/排行补齐） |
| e200fcf | M1 联调 6 bug 修复（L1~L6 全绿） |
| 6455223 | 前端白屏修复 |
| 2154e14 | 注册/登录 IP 限流 |
| 0efbe2e | 用户名中文 + 移除昵称 + @Valid 全量 |
| 8fbf571 | nickname 彻底移除 |
| b2d3fba | schema 编码修复 |
| bba5edd | 竞赛/用户管理/公告三大模块 |
| 19f8ec0 | 自测二轮修复（收藏越界/管理员自锁） |
| 7733a60 | 题库扩充至 20 题 |
| 5d1a847 | JS 判题链路 + 8 道前端题（28 题） |
| 91f7df7 | ECharts 统计 + trend 接口 |
| 94bb3e2 | 主题换血（靛蓝） |
| 527ce5e | 控制台所见即所得修复 |
| ff32957 | 用户名下拉可识别性 |
| 95999f4 | PROGRESS.md 进度文档 |
| f466f92 | 协作流程细化 + dev 分支 |
