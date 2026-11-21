# CodeOJ 项目进度文档

> 最后更新：2026-11-19 ｜ 维护：成员 B ｜ 更新纪律：每完成一个里程碑/修复一批 bug，更新本文档
> 配套文档：[API.md](./API.md)（接口契约 v1.3）｜ [CONTRACT.md](./CONTRACT.md)（模块契约 v1.1）｜ [COLLABORATION.md](./COLLABORATION.md)（分工+分支规则，§3 已改为新手可跟做版）｜ [HANDOFF-A.md](./HANDOFF-A.md)（A 侧交接，新增 §0 上手第一步）｜ [PROGRESS.md](./PROGRESS.md)（本文档）

---

## 1. 项目概况

| 项 | 值 |
|---|---|
| 项目 | CodeOJ —— 基于 Spring Boot + Vue3 + Docker 的在线代码评测与编程练习平台（专科毕业设计） |
| 分工 | 方案 A（已固化）：成员 A = 评测链路（oj-judge real 引擎/judge-images）；成员 B = 业务与前端（oj-server/oj-web/联调/测试） |
| 技术栈 | JDK17 / Spring Boot 3.2.5 / MyBatis-Plus / RabbitMQ / Redis / MySQL 8 / docker-java / Vue3+Vite+TS+Pinia+EP+Monaco / ECharts / SockJS+STOMP / Web Worker 沙箱 |
| 仓库 | github.com/huayuehua15-netizen/codeoj ｜ 分支：**main**（发布快照）/ **dev**（集成）/ **niuzj**（成员 B 个人分支）/ feat/*（成员 A） |
| 交付状态 | **双侧齐备：A 侧 real 评测已实现——C/C++/Java/Python3/JavaScript 五语言全部通过实际判题（§14 与 CONTRACT）；B 侧功能 100% 交付 + 复审修复 + review 40 项建议落地（§19~20）+ 四个新页面（首页/公告中心/错题本/帮助，§22）+ 全站设计体系 v6「工作台」（§24）** |

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

### 交付复审 ✅（2026-11-16，B 侧全面审查 + 修复，见 §11）

### M2.5 · 专业化打磨 ✅（2026-11-16 下午，见 §12）
- 排行榜：同分排序修复、统计维度增强、顶部概览条（§12.1）
- 统计页：从三个环形图升级为数据分析面板（§12.2）
- 竞赛页：去掉 emoji 假图标与空洞卡片，改为状态分层布局（§12.3）
- 演示数据重造：1053 条提交、38 人上榜、三场竞赛三种状态（§12.4）
- 协作简化：`dev` / `main` 合并权统一收归成员 B；文档改为新手可跟做版（§12.5）

### M2.6 · 提交链路三页 UI 专业化 + 判题明细数据 ✅（2026-11-17 凌晨，见 §13）

### M2.7 · 判题镜像一键构建 ✅（2026-11-17 下午，见 §16）

### M2.8 · 提交规范落地 ✅（2026-11-17 晚，见 §18）
- 提交类型从 7 个补到 12 个，与实际使用对齐
- 新增 `.githooks/commit-msg`：拦截非 `[类型]` 格式、未知类型、无说明、以及**开头带 BOM**
- 25 个用例全部通过（12 类型放行 / 6 类非法拒绝 / BOM 拒绝 / 真实 commit 端到端）

### M2.9 · 全站设计体系 v6「工作台」✅（2026-11-19，见 §24）
- v5「校样」纸墨/宋体/朱砂叙事被否后，先出 A/B/C 三版 1440 首页样张；用户选定 **A · 工作台**
- 全站 22 页按 A 方向重构：冷灰白底 + 白色面板 + 蓝色操作信号 + 右侧判题终端
- 桌面 1440 / 平板 1024 / 手机 390 三档横向溢出检查通过；typecheck、25 项单测、build 全绿

### 当前位置
**双侧齐备**：A 侧 real 评测已实现，C/C++/Java/Python3/JavaScript 五语言全部通过实际判题（见 §14 与 CONTRACT）；B 侧功能面 + 复审修复（§19）+ 个人中心扩展（§20）+ 四个新页面（§22）+ 全站设计体系 v6「工作台」（§24；由 v4「墨夜」、v5「校样」迭代而来，v5 已弃用）。
真实判题链路已在本机打通：五语言镜像全部构建并自检通过，首次真实提交判定成功（见 §16）。
协作规范已落成机制：`pre-push` 保构建、`commit-msg` 保提交信息格式（见 §15、§18）。
剩余 P2 = M4 部署/并发复测（需 Linux/A 侧环境）。设计已通过全路由截图与三档响应式检查，待用户最终逐页确认。

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
- 判题三模式：mock（全 AC）/ **local（静态语义判定，默认）** / real（A 侧已实现，五语言真实判题通过）
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
| 题库 | **28 题**（算法 20 + 前端 JS/Vue3 8），难度 简单 11 / 中等 11 / 困难 6，标签 12+ 种 |
| 用户 | **39 个**（38 名学生 + teacher_admin），全部密码可用 |
| 提交 | **1053 条**：AC 350 / WA 612 / TLE 91，时间分布近 14 天（工作日 75~100 条/天、周末 50 条/天） |
| 排行榜 | **38 人上榜**，解题数 22 → 1 平滑递减；同分按「首次达成时间」排序 |
| 课程 | 3 门 |
| 竞赛 | **3 场，三种状态齐全**：进行中（6 题）/ 未开始（4 题）/ 已结束（5 题） |
| 判题明细 | **2451 行 judge_case**（1025 条提交带用例级明细；AC 全过 / WA·TLE 短路语义，`sql/seed-judge-case-demo.sql` 可重复执行） |
| 公告 | 1 条 |

**演示账号（2026-11-16 统一重置为 `123456`）**：

| 用途 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 教师/管理员 | `teacher_admin` | `123456` | role=1，可进管理端 |
| 学生（榜首，数据最丰富） | `胡军` | `123456` | 22 题 / 55 次提交，统计页效果最好 |
| 学生 | `张伟`、`周杰` | `123456` | 有提交数据 |
| 你自己的账号 | `等到你抬起头时` | `123456` | ⚠️ 见下方注意事项 |

> ⚠️ **密码注意**：造演示数据时批量 `UPDATE user SET password=BCrypt('123456') WHERE role=0`，
> 把**你自己注册的账号密码也一并改了**，且 Edge 记住了旧密码会自动填充。
> 登录时若报「用户名或密码错误」，**密码框 Ctrl+A 清空后手输 `123456`** 即可。
> 教师账号 `teacher_admin` 同样已重置为 `123456`（原先的 `admin123` 已失效）。

**服务**：前端 5173 / oj-server 8080 / oj-judge 8081 / MySQL 3306 / Redis 6379 / RabbitMQ 5672+15672

---

## 5. Bug 修复台账（按发现来源；本轮新增见 §11 / §12）

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

## 7. 分支与协作模型（2026-11-17 修订：双方均可合并 dev）

```
成员 A: feat/judge-engine ─┐
                           ├─→ 各自合并进 dev（谁合并谁跑通构建）
成员 B: niuzj ─────────────┘              │
                                          ▼
                                    dev（集成，随时可跑）
                                          │ 验收通过，仍只由成员 B 前进
                                          ▼
                                        main（答辩快照）
```

- **规则**：`dev` **双方都可以合并**；`main` 仍**只由成员 B**在验收通过后前进
- **为什么改**：原「只由 B 合并」会让 A 写完必须等人，B 不在线功能就一直挂着。
  双人毕设里多一道人工交接不划算，改为**谁合并谁负责跑通构建**
- **门禁（已机制化）**：`.githooks/pre-push` 在推送 `dev` 时自动跑
  `mvn -DskipTests package` + `npm run build`，**失败直接拒绝推送**。
  个人分支与 `main` 不触发。启用：`git config core.hooksPath .githooks`（每人一次）
- **兜底约定**：构建失败**不许 `--force`**；谁推坏了谁修，或用 `git revert` 回滚
- **提交格式**：`[新增]/[修复]/[bug]/[重构]/[样式]/[文档]/[杂务] 做了什么`
- **详细操作**：COLLABORATION.md §3 已重写为新手可跟做版（五步流程、冲突处理、命令速查）
- **副本纪律**：`Documents\GitHub\codeoj` 只 pull/查看，不在其中改代码提交（防分叉）

---
## 8. 未完成项（按优先级）

### P0 · A 侧 real 判题引擎（成员 A）✅
- DockerSandbox 实现 + JudgeEngine realRun + judge-images 五镜像构建（含 node）
- 五语言真实判题、旧库迁移和 100/300/500 用户单次突发均已验证，详见 §14 与 [评测链路验证记录](../judge-tests/联调报告.md)
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
| MySQL | root / 123456，库 codeoj。**容器时区已设为 `+08:00`**（原为 UTC，与 JVM 差 8 小时） |
| RabbitMQ 账号陷阱 | 代码默认 `guest/guest`，实际容器是 `codeoj/codeoj123`；两边不一致会 `ACCESS_REFUSED`。做法见 HANDOFF-A §0.3 |
| 构建命令 | `mvn clean install -DskipTests`（改 pom 后必须 clean）|
| 前端构建 | 本机沙箱会因 esbuild 报 `spawn EPERM`；把 `TEMP`/`TMP` 指到工作区内目录即可正常 build |
| 启动顺序 | 容器 → oj-server → oj-judge → 前端 dev |
| 双副本 | ① Desktop\code-king（开发）② Documents\GitHub\codeoj（GitHub Desktop，仅查看/pull） |

### ⚠️ 踩坑清单
1. 改 pom 需 clean install（增量编译不重编，lombok/注解不生效假象）
2. 重启服务前必须确认 8080 端口旧进程已死（netstat 反查 PID——曾连续三轮测到旧代码；2026-11-16 复现一次；**2026-11-17 打磨提交页时第三次复现**：统计接口缺 acceptRate/solvedCount 新字段，重启后恢复）
3. curl 中文测试需 UTF-8（Git Bash 默认 GBK 会 50000）
4. Bash 工具写源码被安全策略拒绝，一律走 Write/Edit
5. 安全工具（Mimosa）对"进程执行用户代码"零容忍——判题执行实现需 HANDOFF-A 路径
6. Element Plus 组件 actionability 误报多——自动化交互优先 evaluate 页面内点击
7. 前后端各自 build 后重启服务，**不要带错 cwd**（mvn -pl 需在项目根）
8. **PowerShell 追加中文会写成 GBK**：`Add-Content` / `Out-File` 按系统 ANSI 编码，混进 UTF-8 文件会乱码（`.gitignore` 踩过）。一律用 Write/Edit 工具，或 `UTF8Encoding($false)` 显式写
9. **commit message 用 PowerShell 写会带 BOM**（`[文档]` 前混进不可见字节）。用 `[System.IO.File]::WriteAllText(..., New-Object System.Text.UTF8Encoding $false)`
10. **MySQL 容器默认 UTC**：`serverTimezone=UTC` 只告诉驱动服务端时区，不会下发 `SET time_zone`。已在两个模块的连接串加 `sessionVariables=time_zone='+08:00'`，并把容器 global time_zone 设为 `+08:00`
11. **浏览器自动填充旧密码**：批量重置演示账号密码后，Edge 记住的旧密码会自动填入，报「用户名或密码错误」。登录前 Ctrl+A 清空手输
12. **GitHub 推送偶发 TLS/连接中断**：`git push` 失败重试即可（本轮最多重试 2 次成功），不是配置问题
13. **文档文件是 CRLF**：用 `edit` 工具按字面匹配大段文本会失败，需按索引位置替换或先统一行尾
14. **MySQL 对含 `RAND()` 的派生表可能不物化**：INSERT...SELECT 中 RAND() 逐行重算，同一行内多个 CASE 条件求值不一致（首版 judge_case 造数出现 243 条 WA 提交用例全过、失败行耗时落入通过区间）。改用 submission_id 取模的确定性伪随机

---

## 10. 提交历史索引（关键节点，`058b7c7` 起见 dev）

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
| 058b7c7 | **[bug] WebSocket 推送全链路根本不投递**（本轮 49 文件审查修复批次，2 个 P0 + 14 个 P1） |
| 36405ad | [文档] Git 协作规范与 A 侧交接改为新手可跟做版 |
| 60635f5 | [文档] 提交规范统一为方括号格式 |
| 2cfe134 | [文档] 明确 dev 与 main 只由成员 B 合并 |
| 4aecc62 | [优化] 排行榜专业化改造：同分排序修复 + 统计维度增强 |
| 26d7f92 | [优化] 统计页专业化改造：从三个环形图升级为数据分析面板 |
| 72a9c2e | [优化] 竞赛页重做：去掉 emoji 假图标与空洞卡片 |
| 5ce1b65 | [优化] 提交记录/判题结果/题目详情三页专业化打磨 |
| 30d941d | [新增] judge_case 演示数据补造脚本（用例级判题明细） |

> 说明：`058b7c7` 起在 main，`36405ad` 之后均在 **dev**（个人分支 `niuzj` 的提交已合入 dev）。
> 按 §7 新规则，main 只在验收通过后由成员 B 推进。

---

## 11. 交付复审（2026-11-16）：37 → 53 项

> 触发：B 侧自评"功能 100% + bug 清零"。经对 `oj-server` / `oj-common` / `oj-web` / `sql`
> 全量复审（含契约逐条比对、并发与越权路径推演），结论是**功能面确实完整，但存在
> 2 个 P0 级"静默失效"缺陷**——这类问题不报错、不打日志、页面看似正常，
> 只在特定路径暴露，自测很难命中。完整报告见 `review/backend.md`、`review/frontend.md`。
> **本轮另含 1 处跨边界改动（oj-judge 配置 2 行），见 §11.6 登记。**

### 11.1 P0（阻断级，已修复）

| # | 问题 | 本质 | 修复 |
|---|---|---|---|
| 1 | **WebSocket 推送全链路不投递**：`/ws/**` permitAll + 无 STOMP 拦截器 + `LoginUser` 非 `Principal` + 前端不带凭证 | `convertAndSendToUser` 需会话绑 Principal，四层缺一不可；缺了则 `UserDestinationMessageHandler` **静默丢弃**（仅 debug 日志）。§3.3「实测端到端」实为前端 2s 轮询兜底，L3「无轮询收到推送」**从未成立** | `LoginUser implements Principal`（`getName()`=userId）+ `WebSocketConfig` 加 CONNECT 帧拦截器验 JWT + `ws.ts` 加 `connectHeaders`；约定固化进 CONTRACT §5.1 |
| 2 | **收藏接口被路径级 permitAll 击穿**：匿名请求 `user.userId()` NPE → 50000（契约要 40100），且每请求打一条完整堆栈可刷日志 | `/api/problems/**` 对游客公开是为题目浏览，却把挂在同前缀下的收藏接口一并放行 | `FavoriteController` 显式 `requireLogin` 抛 40100 |

### 11.2 P1（重要，已修复）

| # | 问题 | 修复 |
|---|---|---|
| 1 | **角色取自 JWT claim**：管理员降权后旧 token 24h 内仍持 `ROLE_ADMIN`（垂直越权残留）。过滤器已每请求查库，却只用了 status 没用 role | `JwtAuthFilter` 角色改取数据库 |
| 2 | **时区三重混源**：`created_at` 走 JVM（UTC+8）、`judged_at` 走 SQL `NOW()`（容器 UTC）→ 同一行倒挂 8h；队列监控 00:00-08:00 统计错位。`serverTimezone=UTC` 不下发 `SET time_zone` | 两端连接串加 `sessionVariables=time_zone='+08:00'`，统一 Asia/Shanghai。⚠️ **其中 oj-judge 一侧属成员 A 范围，见 §11.6 跨边界登记** |
| 3 | **`LocalDateTime` 输出 ISO `T` 格式**，违反 §0.1。`spring.jackson.date-format` 对 java.time **无效** | 新增 `JacksonConfig` 注册全局序列化器；前端 3 处 `.replace('T',' ')` 删除 |
| 4 | **`SubmitVO.status` 输出枚举名 `"QUEUING"`**，契约要数字 0（WS 侧已转数字，HTTP 侧漏了） | 改 `Integer` + `status.getCode()` |
| 5 | **`submit()` 自调用导致 `@Transactional` 失效**（`this.createQueued()` 绕过代理）；注释与 Javadoc 三处说谎 | 改 `TransactionTemplate` 显式事务，消息严格在 commit 后发 |
| 6 | **MQ 可靠投递只有一半**：手工 `new RabbitTemplate` 覆盖自动配置 → 丢 `mandatory`；传了 `CorrelationData` 但全仓零 `ConfirmCallback` → 队列抖动时产生 `status=0` 僵尸单，前端轮询永不终止且日志全绿 | 补 `setMandatory(true)` + `setReturnsCallback` + `setConfirmCallback` |
| 7 | **登录失败锁按 username 加锁**，任何人可连发 5 次错密码锁死教师 15 分钟（未授权 DoS），且 `INCR`+`EXPIRE` 非原子 | 键加 IP；改 Lua 原子计数 |
| 8 | **重置密码"临时密码"可预测** = `"oj"` + 当天秒数，同一秒重置的账号密码还完全相同 | 改 `SecureRandom` + Base64 |
| 9 | `judge_case.status` 可为 NULL（schema 允许），`JudgeCaseVO.of` 拆箱 NPE → 提交详情 50000 | 判空降级 |
| 10 | **题目列表返回 description/samples/languages 全量大字段**，一页塞整份题面 | 新增 `ProblemListItemVO` + `select` 轻量列 |
| 11 | `/ws` `setAllowedOriginPatterns("*")` | 已保留但鉴权上移 STOMP 层（见 P0-1 修复说明） |
| 12 | 配置硬编码凭证 + JWT 默认密钥随仓库分发 | 保留 dev 默认值但加注释警告；生产必须 `JWT_SECRET` |
| 13 | **零单元测试**（`src/test` 不存在） | 待补（见 §11.5） |
| 14 | 公告 `Map` 入参绕过 `@Valid`，超 VARCHAR(2000) 直接 50000 | 补长度校验对齐库表 |

### 11.3 一并修复（审查中追加发现）

| # | 问题 | 修复 |
|---|---|---|
| 1 | **用例删除 IDOR**：`DELETE /api/problems/{A}/testcases/{caseId}` 只用 caseId 删除，可借 A 题路径删掉 B 题用例（直接改变判题结果） | 校验用例归属 problemId |
| 2 | 题目删除留下孤儿用例 | 级联清理 `test_case` |
| 3 | 缺三组复合索引：提交记录页「过滤+排序」、竞赛排名「题目集+时间窗」、趋势聚合 | `schema.sql` 补索引 + `migration-20261116-indexes.sql` 供已建库升级 |
| 4 | `SubmissionMapper` 重复 `import java.util.List`（真的两行） | 删除 |

### 11.4 复查确认**没有问题**的点（可放心答辩）

`RateLimiter` Lua 原子、幂等锁 `SET NX PX`、排行榜两端 `StringRedisTemplate` 统一、
`JwtAuthFilter` `clearContext` 防 ThreadLocal 残留、登出/改密真写黑名单、
状态机两条条件更新与 CONTRACT §3 **逐字符一致**、`JSON_CONTAINS` 参数化无注入面、
XSS 面干净（全仓 `v-html` 仅 1 处且过 DOMPurify，公告走 `{{ }}` 文本插值）、
垂直越权无路径级遗漏（18 处 `@PreAuthorize` 逐一核对）、
水平越权核心链路正确、分页 `maxLimit=100` 且 total 正确、
N+1 已处理 3 处（提交记录/排行榜/竞赛排名批次补用户名）、
前端 20+ 接口路径与字段**全部一致**、前端权限判定与后端完全对齐（篡改 localStorage 拿不到数据）、
Web Worker 沙箱同步死循环确实能被 `terminate()` 杀掉。

### 11.5 仍未完成（诚实定级）

| 级别 | 项 | 说明 |
|---|---|---|
| **P0（A 侧）** | real 判题引擎 | 唯一大缺口，`ENGINE_MODE=local` 目前是静态语义判定（无入口判 CE、无输出判 WA），**不是真实运行判定**。答辩若被问"判题是不是真跑代码"需如实说明 |
| P1 | 零单元测试 | `spring-boot-starter-test` 已引但 `src/test` 不存在。建议至少补：状态机条件更新幂等、限流窗口、`OutputComparator`、越权判定 |
| P2 | 题目列表 `submitCount` 是题目维度的，横幅"累计提交"只累加当前页 | 演示级可接受，量级大了要单独统计接口 |
| P2 | 前端按需导入 | `manualChunks` 已拆包（monaco/echarts/element），但 Element Plus 仍是全量注册；再压体积需 `unplugin-vue-components` |

> 已修复并验证：包体拆分（`ProblemDetailView` 3.27MB → 11.4KB，monaco 移入独立 vendor chunk）、
> Monaco worker（新增 `src/monacoWorker.ts`）。

### 11.6 ⚠️ 跨边界改动登记（需知会成员 A）

**纪律依据**：`COLLABORATION.md` §1 ——「成员 A 只动 oj-judge、judge-images、deploy 中判题相关配置；
oj-common 改动仍需双方同意」。本轮审查**有 1 处越界改动，仅 2 行，不涉及任何判题逻辑**：

| 项 | 内容 |
|---|---|
| 文件 | `oj-judge/src/main/resources/application.yml` |
| 改动 | ① `serverTimezone=UTC` → `Asia/Shanghai` + 追加 `sessionVariables=time_zone='%2B08:00'`；② `spring.jackson.time-zone: UTC` → `Asia/Shanghai` |
| 为什么必须动 A 的地盘 | `judged_at` 由 **oj-judge 的 `SubmissionMapper.updateFinalResult` 用 SQL `NOW()`** 写入。只改 oj-server 的时区，`created_at`（JVM 写）与 `judged_at`（judge 写）仍差 8 小时——实测确认过，见 §11.1 时效验证 |
| 是否碰逻辑 | **否**。未改任何 `.java`、`judge-images/`、`deploy/`、`oj-common/`；仅连接串与会话时区 |
| 对 A 的影响 | 无行为变化。A 若在本地用 UTC 容器跑，改后 judge 与 server 时间口径一致（原本就是 bug） |
| 回滚方式 | `git checkout -- oj-judge/src/main/resources/application.yml`（但会重新引入 8 小时倒挂） |
| 待办 | **需成员 A 确认**；确认后本条可转为常规变更记录 |

> 另：`oj-common` 本轮**零改动**（`LoginUser` 在 `oj-server` 内，不在公共区），不触发"双人同意"条款。


---

## 12. 专业化打磨（2026-11-16 下午）

> 触发：B 侧反馈「看着简陋 / 一股 AI 味」。核查后结论是**功能量对专科毕设本就超标，
> 问题在于数据没铺开、页面信息密度低、emoji 当图标**。本轮按"答辩能讲、经得起追问"
> 的标准重做三个核心数据页，并重建演示数据。

### 12.1 排行榜（commit `4aecc62`）

**修掉一个真 bug**：6 人并列 4 题时，排名顺序是 `40,36,34,26,25,17`，即 **userId 字典序**，
没有任何业务含义。老师问「都做 4 题凭什么他第一」无从回答。

改为专业 OJ 规则：**解题数降序 → 首次达成该题数的时间升序 → userId 兜底**。
实测 3 个并列 19 题的用户，首次 AC 时间 17:20 / 18:01 / 18:41，返回顺序完全一致。

| 改动 | 内容 |
|---|---|
| 新增 | `LeaderboardStatsMapper`：批量聚合总提交 / AC 提交 / 最后活跃 / 首次 AC 时间 |
| 修复 | `overview()` 原带 `WHERE status=2`，导致全站通过率**恒为 100%** |
| 后端 | `LeaderboardVO` 增加 `acceptRate` / `totalSubmit` / `lastSubmitAt` 与全站 `summary` |
| 前端 | 顶部概览条（上榜人数 / 最高解题数 / 全站提交 / 整体通过率） |
| 前端 | 表格 3 列 → 5 列；通过率分级配色；进度条基准改为**全站最高**而非当前页第一条（翻页不再跳变） |
| 前端 | 每页条数可选 10/20/50；新增 `relativeTime` 工具 |

### 12.2 统计页（commit `26d7f92`）

从「三个环形图 + 两个 ECharts」升级为四层数据分析面板：

1. **顶部四卡**：总通过率 / 已解决题目 / 题目覆盖率 / 平均每解一题提交数
2. **难度攻克卡片**：从只看通过率改为「已解 N / 共 M 题」覆盖度进度条
3. **状态分布环形图**（新增）：WA 多 → 逻辑问题，TLE 多 → 算法复杂度问题，可直接得出结论
4. **难度对比明细表** + 趋势图左右分栏

后端 `StatisticsVO` 增加状态维度 `byStatus` 与汇总字段；新增
`statusCountByUser` / `attemptedCountByUser` 聚合查询，难度维度补 `solvedCount` / `totalCount`。

实测（胡军，22 题）：55 次提交、通过率 40.0%、三难度全覆盖、状态分布正确。

### 12.3 竞赛页（commit `72a9c2e`）

问题定位：两场竞赛**名称完全相同**、时间只差 1 分钟、都显示「已结束」；
emoji（`⏰ 📋 🚀 ⏳`）当图标；`justify-content: space-between` 导致卡片中间 2000px 空白。

| 改动 | 内容 |
|---|---|
| 列表页 | 进行中单独用深色强调卡（呼吸灯 + 秒级倒计时 + 柔光/网格纹理） |
| 列表页 | 其余改为紧凑行式列表（状态徽标/标题/描述/时间/题数/倒计时/箭头），无空白 |
| 列表页 | 页头计数 + 「全部 / 未开始 / 已结束」筛选 |
| 两页 | emoji 全部替换为内联 SVG；详情页头部同步重做，题号徽标降为浅底主色 |
| 数据 | 两场重复竞赛 → 三场真实竞赛（进行中 6 题 / 未开始 4 题 / 已结束 5 题），描述写真实内容 |

**顺带修复**：MySQL 容器是 UTC、系统是 UTC+8，导致竞赛状态判错、倒计时错误。
已把容器 `global time_zone` 设为 `+08:00`，两个模块连接串也加了 `sessionVariables`。

### 12.4 演示数据重造

| 项 | 原状 | 现在 |
|---|---|---|
| 提交 | 105 条，状态混杂但最高仅 4 题 | **1053 条**（AC 350 / WA 612 / TLE 91） |
| 排行榜 | 28 人，解题数 4→1 | **38 人，22→1 平滑递减**（强手/中上/中等/入门/新手五档） |
| 时间分布 | 全挤在一天 | 近 14 天，工作日 75~100 条/天、周末 50 条/天 |
| 题目统计 | 与实际提交脱节 | 已 `UPDATE problem` 同步 submit_count / accepted_count |
| Redis 榜 | 与 DB 不一致 | 已按 DB AC 数重建 |
| 测试账号 | 6 个 smoke/ad 账号混在榜里 | 已清理 |

> ⚠️ **副作用（已登记）**：批量 `UPDATE user SET password=BCrypt('123456') WHERE role=0`
> 把用户自己注册的账号和 `teacher_admin` 密码都改了。全部账号现统一 `123456`，
> 旧密码失效；Edge 自动填充旧密码会报「用户名或密码错误」，清空手输即可。见 §4。
>
> 另：竞赛时间窗是相对 `NOW()` 生成的，**过几小时「进行中」会自然变成「已结束」**。
> 答辩前若需演示实时排名，执行：
> ```sql
> UPDATE contest SET start_time = DATE_SUB(NOW(), INTERVAL 50 MINUTE),
>                    end_time   = DATE_ADD(NOW(), INTERVAL 130 MINUTE) WHERE id = 4;
> ```

### 12.5 协作流程简化

| 改动 | 内容 |
|---|---|
| 分支规则 | ~~`dev` / `main` 只由成员 B 合并~~ → **已于 2026-11-17 修订：`dev` 双方都可合并，`main` 仍只由 B**（见 §7） |
| 提交规范 | 统一为 `[新增]/[修复]/[bug]/[重构]/[样式]/[文档]/[杂务] 做了什么` |
| COLLABORATION §3 | 从 18 行结论重写为 177 行可跟做版：五步流程、冲突处理、目录归属表、命令速查、三条救命提示 |
| HANDOFF-A §0 | 新增 112 行上手引导：环境依赖、四步跑起来、curl 验证判题链路、故障对照表 |
| 事实核查 | 补上 RabbitMQ 账号陷阱：README 命令建的是 `guest/guest`，实际环境用 `codeoj/codeoj123` |

### 12.6 本轮验证

```
mvn clean package       BUILD SUCCESS（4 模块）
npx vue-tsc --noEmit    exit 0
npx vite build          ✓ built
接口实测                排行榜 / 统计 / 竞赛三页数据全部正确
构建产物                ProblemDetailView 3.27MB → 11.4KB（monaco 独立 vendor chunk）
```

### 12.7 仍未完成（更新 §11.5）

| 级别 | 项 | 状态 |
|---|---|---|
| **P0（A 侧）** | real 判题引擎 | 仍是唯一大缺口，`ENGINE_MODE=local` 为静态语义判定，**非真实运行** |
| P1 | 零单元测试 | `src/test` 仍不存在 |
| P2 | Element Plus 全量注册 | `manualChunks` 已拆包，按需导入未做 |

> ~~提交记录页 / 题目详情页仍是纯表格与基础布局~~ → 已在 §13（M2.6）完成。

---

## 13. 提交链路三页 UI 专业化（2026-11-17 凌晨，M2.6）

> 触发：§12.7 遗留——提交记录页 / 判题结果页 / 题目详情页仍是「纯表格与基础布局」，
> 与 M2.5 打磨过的排行榜/统计/竞赛三页不匹配。本轮只动 `oj-web` 呈现层 +
> `sql/` 演示数据，**未触碰 oj-server 接口契约**（概览条复用现成统计接口）。

### 13.1 页面改动（commit `5ce1b65`）

| 页面 | 改动 |
|---|---|
| 提交记录页 | 顶部个人概览条（累计提交/判题通过/通过率/已解题目，数据来自现成 `GET /submissions/statistics`，靛蓝渐变与排行榜/题库同款）；筛选区卡片化 + 快捷时间范围（今天/近 7 天/近 30 天，与日期选择器联动）；表格专业化：状态彩色圆点、语言徽章、题号徽章、耗时/内存等宽格式化（≥1s 转 s、≥1MB 转 MB）、提交时间相对化（悬浮显绝对时间）、分页带 total 与条数切换 |
| 判题结果页 | 状态同族配色判定横幅（绿=通过/红=错误/橙=超限/灰=等待，SVG 图标 + 浅底色）；元信息胶囊（题目链接/语言/耗时/内存）；用例结果卡：通过 N/M 进度条 + 用例级表格；深色代码查看器（`#0f172a`，与控制台同风格）+ 语言徽章 + 一键复制；errorInfo 红色告警条；底部返回/查看题目 |
| 题目详情页 | 头部重排：题号徽章 + 标题 + 难度软色 chip + 收藏右对齐；限制胶囊换内联 SVG（时钟/芯片），时间限制 ms→s 智能格式化；新增题目维度通过率胶囊（分级配色）；样例改深色代码卡 + 复制按钮；编辑器列 sticky（长题面滚动时编辑器保持可见）；控制台 🖥/✖/▶ emoji 全部换内联 SVG |
| 题库页 | 公告栏 📢 emoji 换内联 SVG 喇叭（延续 M2.5「去 emoji 当图标」标准） |
| 共享工具 | `utils/status.ts` 新增 `statusColor` / `statusTint`（状态同族主色与浅底色）、`formatTimeUsed` / `formatMemory`（耗时内存格式化），两个提交页共用 |

**保持不变**：判题状态机逻辑、WebSocket 订阅、轮询兜底、语言切换模板保护、收藏逻辑——全部逐行保留，只动模板与样式。

### 13.2 判题明细演示数据（commit `30d941d`）

判题结果页的「用例级结果」卡片（论文 5.7 验收点）在演示库中无数据可展示——
1053 条演示提交由脚本直插（§12.4），从未生成 judge_case 行。补造脚本
`sql/seed-judge-case-demo.sql`（可重复执行，自带 DELETE）：

| 项 | 结果 |
|---|---|
| 生成规模 | 2451 行，1025/1053 条提交带明细（缺的 28 条属于唯一没有 test_case 的题） |
| 语义 | AC 全用例通过；WA/TLE 短路到首个失败用例；每题用例数取 test_case 实际行数（2~5） |
| 一致性校验 | WA/TLE 提交均恰有一条失败行；失败行耗时全部 ≥600ms（首版两类缺陷已修复，见踩坑 #14） |

### 13.3 环境处置（顺手修复）

- **第三次踩中「旧进程」坑（踩坑 #2）**：打磨期间发现运行中的 oj-server 是旧实例，
  统计接口缺 `acceptRate`/`solvedCount`/`byStatus` 字段，统计页与新概览条都会开天窗。
  已杀旧进程并从当前源码重启（RabbitMQ 需带 `RABBITMQ_USER=codeoj RABBITMQ_PASSWORD=codeoj123`，
  见踩坑清单 §9 与 HANDOFF-A §0.3）。
- 已知瑕疵（登记不修）：演示提交 id 倒序但 created_at 非单调（§12.4 造数产物），
  提交列表出现「#1255 五天前、#1246 十一小时前」。修复需重排全部提交时间，
  会牵连竞赛窗口排名/趋势聚合/首次 AC 排序，风险大于收益。

### 13.4 本轮验证

```
npx vue-tsc --noEmit    exit 0
npx vite build          ✓ built in 1m10s（分包结构不变，ProblemDetailView 11.4KB→14.3KB）
浏览器逐页验收          提交记录（概览条数据正确）/ 判题结果 AC+WA（横幅/用例表/代码卡）/ 题目详情（头部/样例卡/sticky）
SQL 一致性              WA/TLE 失败行校验 0 缺失、0 越界
```

### 13.5 仍未完成（同 §12.7，无新增）

| 级别 | 项 | 状态 |
|---|---|---|
| **P0（A 侧）** | real 判题引擎 | 唯一大缺口，`ENGINE_MODE=local` 为静态语义判定，**非真实运行** |
| P1 | 零单元测试 | `src/test` 仍不存在 |
| P2 | Element Plus 全量注册 | `manualChunks` 已拆包，按需导入未做 |

### 13.6 追加打磨（用户反馈「UI 还是普通、图标全部不要 emoji」，commit `083b2c9`）

| 改动 | 内容 |
|---|---|
| 排行榜前三名 | 🥇🥈🥉 emoji → **金/银/铜渐变名次徽章**（圆角方块 + 白字，与前三行高亮、进度条配色呼应） |
| 题目详情收藏 | ★/☆ 文字符号 → **SVG 星星胶囊按钮**（已收藏态琥珀色填充） |
| 题目详情编辑器 | 工具条改**深色 IDE 风格**：左侧「代码编辑」标签 + 半透明语言选择器，与 Monaco 深色编辑区、控制台连成一体 |
| 样例节标题 | 加主题色强调竖条（与题面 Markdown 标题同款） |
| 全仓扫描 | emoji 清零（`oj-web/src` 仅剩注释符号与文案排版箭头 `→`，非图标） |

## 14. A 评测链路本地修复与兼容复核（2026-11-16；已并入 dev）

2026-11-18 复测补充：修复确认后重复补发、Docker 瞬时故障终结、超时误判和语言低内存配置。新提交固定判题用例与资源限制，详情区分总用例数与已执行数。五语言真实判题、旧库迁移和 100/300/500 用户单次突发均已验证，分档数据与完整验收范围见 [评测链路验证记录](../judge-tests/联调报告.md)。

此前章节保留 B 的原始交付记录。A 已实现五语言真实沙箱、可信统计与工作目录隔离、租约恢复和持久补发；已完成的验证见 judge-tests/联调报告.md。当前本地基于 dev `72a9c2e` 完成兼容复核，保留 B 新增的统计、排行榜、竞赛、WS 和时区能力；61 项 Java 回归、前端构建、五语言/WS、11 项时钟和 8 项业务接口检查通过。CONTRACT 升级 v1.2，跨成员改动集中登记于 docs/JUDGE-COMPATIBILITY.md。dev/main 仍仅由 B 合并。

> ⚠️ 上面这句「dev/main 仍仅由 B 合并」**已于 2026-11-17 失效**，最新规则见 §7 与 §15。
> §14 正文保留 A 提交时的原话不改。

---

## 15. 协作规则修订：dev 合并权放开（2026-11-17）

### 15.1 改了什么

| 项 | 原规则 | 新规则 |
|---|---|---|
| `dev` 合并权 | 只由成员 B | **双方都可合并** |
| `main` 合并权 | 只由成员 B | **不变**，仍只在验收通过后由 B 前进 |
| 门禁方式 | 人工纪律：合并前自己记得跑构建 | **机制：`.githooks/pre-push` 自动构建，失败拒绝推送** |

### 15.2 为什么改

原规则的理由是「合并权收在一人手上，`dev` 才能保证随时能跑」。这理由本身成立，
但它**依赖人工纪律**，代价是 A 写完必须等 B 在线，B 不在功能就一直挂着——
双人毕设里多一道交接不划算。

核查后确认：仓库**没有 CI、没有 git hook**（`.github/` 不存在，`.git/hooks` 只有 `.sample`），
也就是说「`dev` 随时能跑」当时完全靠自觉。放开合并权而不加机制，风险反而更大。

所以这次是**配套改的**：放开权限的同时把门禁变成自动的。

### 15.3 新增机制：`.githooks/pre-push`

| 项 | 说明 |
|---|---|
| 触发条件 | 仅推送 `refs/heads/dev` 时构建；个人分支不拦，`main` 只提示 |
| 检查内容 | ① `mvn -q -DskipTests package`（优先用 `./mvnw`）② `oj-web` 的 `npm run build` |
| 失败行为 | 打印错误摘要 + 提示，**退出码 1 拒绝推送** |
| 环境缺失处理 | 找不到 `mvn` / `node_modules` 时**警告并跳过**该项，不阻塞 |
| 跳过方式 | `git push --no-verify` 或 `SKIP_BUILD_CHECK=1 git push` |
| 启用方式 | `git config core.hooksPath .githooks`（每人克隆后一次） |
| 行尾处理 | 已加入 `.gitattributes`：`.githooks/* text eol=lf`，防 Windows CRLF 导致 `bad interpreter` |

### 15.4 验证结果（本机实测，Git 自带 `sh`）

| 用例 | 期望 | 实际 |
|---|---|---|
| 推 `niuzj` 个人分支 | 静默放行 | 退出码 0，无输出 ✓ |
| 推 `main` | 提示但不拦 | 输出提醒，退出码 0 ✓ |
| `SKIP_BUILD_CHECK=1` 推 `dev` | 跳过构建 | 输出跳过提示，退出码 0 ✓ |
| 推 `dev` 且构建失败 | 拒绝推送 | 输出错误摘要，**退出码 1** ✓ |

### 15.5 兜底约定

- 构建失败**不许用 `--force` 绕**；谁推坏谁修，或 `git revert`
- 用了 `--no-verify` 必须在群里说明，并尽快补一次构建
- `dev` 一旦出现跑不起来的情况，**先 revert 恢复可用状态**，再讨论怎么修——
  不要两个人在坏掉的 dev 上继续叠加改动

### 15.6 仍未做

| 级别 | 项 | 说明 |
|---|---|---|
| P2 | 云端 CI | 当前是本地 hook，绕过方式存在（`--no-verify`）。要彻底杜绝需 GitHub Actions，双人毕设性价比不高，暂不做 |
| ~~P3~~ | ~~提交信息校验~~ | **已做**，见 §18 |

---

## 16. 判题镜像一键构建（2026-11-17 下午，M2.7）

### 16.1 起因：一个只在真实判题时才暴露的断链

现象是**五种语言提交全部失败**。排查后确认代码本身没有问题，问题在于**判题镜像从来没有随仓库分发过**，而这台机器也从没构建过：

```
codeoj/python:3.12      2026-11-17 12:58:20   ← 全是当天中午才第一次出现
codeoj/java:openjdk17   2026-11-17 12:59:10
codeoj/c:gcc13          2026-11-17 12:59:44
codeoj/cpp:gcc13        2026-11-17 12:59:44
codeoj/node:20          2026-11-17 13:00:33
```

`git log` 佐证：`judge-images/` 下从来没提交过任何构建脚本，只有 5 个 Dockerfile 和 `runner.sh`。`docs/HANDOFF-A.md:136` 也一直把「五镜像构建并验证」列在**待实现项**里。

为什么之前一直"看起来正常"——查数据库：

| 检查项 | 结果 |
|---|---|
| 有 `judged_at` 的提交总数 | 1060 |
| 其中 `created_at == judged_at` | **953 条** |
| 剩余 107 条中时间差恰好 86400 秒的 | **100 条** |
| 演示提交的实际代码 | `// wm`（5 个字符） |

真实判题不可能让创建时间和判题时间完全相等，也不可能恰好相差整数 24 小时。这些都是 `sql/seed-judge-case-demo.sql` 与 `tools/seed_demo_problems.py` 直接插库的产物。

还有一个决定性证据：**当天下班前全库没有一条 `status=8`（系统错误）记录**。真实沙箱只要跑过一次、缺镜像就必然报 8。说明 `real` 这条路径在本机一次都没真正执行过。

**结论**：演示数据会把真实断链完全掩盖。这是一个值得写进论文的现象——页面正常 ≠ 链路正常。

### 16.2 修复：`judge-images/build.sh`

新增一键构建脚本（266 行），补齐三件事：

| 能力 | 说明 |
|---|---|
| 预检 | Docker 可用性（区分"没装"与"守护进程没起"）、`runner.sh` 存在、**CRLF 检测**、Dockerfile 存在 |
| 构建 | 五个镜像，tag 与 `*Strategy.image()` 严格对齐；`--no-cache`、按语言选择性构建 |
| **自检** | 在**与 `DockerSandbox.buildHostConfig` 同构的限制集**下跑真实"编译→降权执行→解析统计协议"，断言输出正确 + `exit=0` + `###CODEOJ_STAT###` 存在 |

关键点：**构建成功 ≠ 镜像可用**。自检用只读根、`--network none`、`--cap-drop ALL` + `SETUID/SETGID`、`no-new-privileges`、pids/nofile/fsize 上限、`/run/codeoj` tmpfs，确保 `runner.sh` 能在这个组合下真的跑起来，而不是只在宽松容器里能跑。

国内网络支持通过环境变量覆盖，不改仓库里的 Dockerfile：

```bash
BASE_REGISTRY_MIRROR=registry.cn-hangzhou.aliyuncs.com \
APT_MIRROR=mirrors.aliyun.com ./build.sh
```

Windows + Git Bash 的两个坑已内置处理：`MSYS_NO_PATHCONV=1`（防 `/work` 被改写成 `C:/Program Files/Git/work`）与 `cygpath` 路径转换（防 `-v` 挂载路径被 dockerd 拒绝）。

### 16.3 验证结果

五个镜像全部构建并自检通过（本机实测）：

```
自检通过（输出 7，exit=0 time_ms=10  cpu_ms=0  mem_kb=1584  trunc=0）   # c
自检通过（输出 7，exit=0 time_ms=20  cpu_ms=0  mem_kb=3312  trunc=0）   # cpp
自检通过（输出 7，exit=0 time_ms=80  cpu_ms=70 mem_kb=36000 trunc=0）   # java
自检通过（输出 7，exit=0 time_ms=50  cpu_ms=10 mem_kb=9360  trunc=0）   # python
自检通过（输出 7，exit=0 time_ms=210 cpu_ms=60 mem_kb=37728 trunc=0）   # node
```

随后做**首次真实端到端判题**（此前从未跑通过），三种语言三种结果全部符合预期：

| 提交 | 语言 | 代码 | 结果 | 耗时 | 说明 |
|---|---|---|---|---|---|
| 1890 | C++ | `int main(){ this is not cpp }` | **CE(7)** | — | 编译器错误被正确捕获，`error_info` 含完整 gcc 诊断 |
| 1891 | Java | `Scanner` 读两数求和 | **AC(2)** | 110ms / 90ms | 两个用例均通过，内存 36MB |
| 1892 | JS | `split(/\\s+/)` 写错转义 | **WA(3)** | 50ms | 代码本身错误，判定正确 |

`judge_case` 明细表同步落库（用例号/状态/耗时/内存），证明「编译一次 + 逐用例隔离执行 + 用例级回写」整条链路是通的。

### 16.4 配套文档

| 文件 | 改动 |
|---|---|
| `judge-images/build.sh` | 新增 |
| `docs/JUDGE-IMAGES.md` | 新增，含前置条件、用法、镜像站、验证 SQL、7 类故障排查 |
| `README.md` | 第 5 步改为一行 `./build.sh`；补「镜像不进仓库」警示；目录树与功能清单同步 |
| `docs/JUDGE-HARDENING.md` §3 | 构建说明改为指向 `build.sh` 与 `JUDGE-IMAGES.md` |
| `docs/HANDOFF-A.md` §2.1 | 「四镜像」更正为「五镜像」，标注脚本已提供 |

### 16.5 答辩时可直接用的说法

> 判题镜像属于构建产物，不进版本库，每台部署机器必须执行 `judge-images/build.sh` 自行构建。
> 该脚本在构建后立刻用与沙箱同构的限制集跑一次真实编译执行，验证的是「镜像可用」而不是「构建成功」。
> 本机实测五语言自检全绿，并用 C++ 编译错误、Java 通过、JS 答案错误三种场景验证了真实判题链路。

### 16.6 仍未做

| 级别 | 项 | 说明 |
|---|---|---|
| P1 | 镜像预构建分发 | 若答辩机器与开发机不同，仍需现场构建（约 6.3GB 下载）。可提前 `docker save` 成 tar 备份 |
| P2 | 云端镜像仓库 | 需要私有 registry，双人毕设不划算 |

---
## 17. 完整性改善补充（2026-11-17，个人分支，待评审）

在 B 的 ecdc149 镜像构建交付之上，补齐题组/用例校验、引用与历史删除保护、排行榜分页概览、前端异步等待隔离、严格本地检查与云端 CI 配置。Java 回归已增至 70 项；Python 工具/门禁共 19 项。真实管理接口 16 项、五语言 AC、WS 定向推送和现有镜像自检通过；构建依赖升级后锁文件审计从 5 项降为 0。完整记录及未验收边界见 [DELIVERY-CHECKLIST.md](./DELIVERY-CHECKLIST.md)。这是一轮新增本地改善，不覆盖此前共同作者的历史进度。

---

## 18. 提交规范落地：类型补全 + commit-msg 门禁（2026-11-17 晚）

### 18.1 起因

提交判题镜像脚本时写成了 `feat(judge-images): ...`，而 `docs/COLLABORATION.md` 规定的是
`[类型] 做了什么`。同时该提交信息开头带了 UTF-8 BOM（`EF BB BF`），GitHub 上显示为
`﻿feat(judge-images): ...`，前面多一个空白方块。

两个问题都不是「不知道规范」那么简单：**规范写在文档里，但没有任何机制强制**，
靠人记就会漏。

### 18.2 全库实测数据

| 统计项 | 数值 |
|---|---|
| 全库提交总数 | 64 |
| 方括号格式 | 25 |
| 其他格式（`feat:` / `fix:` / `docs:` / `style:` / `chore:`） | 39 |

实际出现过的方括号类型（含次数）：

```
9 [文档]   5 [优化]   2 [新增]   2 [协同]   2 [判题]
1 [部署]   1 [测试]   1 [数据]   1 [修复]   1 [bug]
```

其中 `[判题]`、`[部署]`、`[测试]`、`[数据]`、`[优化]`、`[协同]` **六个类型文档里根本没写**，
是成员 A 与 B 各自临时造出来但语义确实合理的。`[修复]` 与 `[bug]` 同义却混用。

### 18.3 改了什么

**① 类型表从 7 个补到 12 个**（`docs/COLLABORATION.md` §提交信息格式）

新增 `[优化]` `[判题]` `[部署]` `[测试]` `[数据]` `[协同]`，并明确 `[修复]` 统一替代 `[bug]`。
理由：判题、部署在这个项目里是独立工作面，硬塞进 `[新增]` 会丢失信息；文档跟实际对齐，
比让人去猜「这个改动算不算新增」更实际。

**② 明确历史格式分界**

文档新增对照表，说明「Conventional Commits 是早期 20 条，方括号是近期」，
并写明**旧提交不改写**——改写已推送的历史会让所有人本地仓库错乱，收益远小于代价。

**③ 新增 `.githooks/commit-msg`**

| 项 | 说明 |
|---|---|
| 触发 | 每次 `git commit` |
| 与 `pre-push` 的关系 | 共用 `core.hooksPath .githooks`，一条配置命令同时启用两个 |
| 拒绝条件 | ① 非 `[类型]` 开头 ② 类型不在白名单 ③ 说明为空或少于 4 字 ④ **开头带 UTF-8 BOM** |
| 放行条件 | 空信息、`Merge ...`、`Revert ...` |
| 警告但不拦 | 首行超过约 70 中文字符 |
| 跳过 | `git commit --no-verify` |
| 行尾 | 已被 `.gitattributes` 的 `.githooks/* text eol=lf` 覆盖 |

BOM 检测**放在格式检查之前**：否则带 BOM 的提交只会报「格式不对」，
使用者根本想不到是 BOM 导致，会反复检查 `[类型]` 写没写错。

### 18.4 验证结果（25 个用例，全通过）

| 分组 | 用例数 | 结果 |
|---|---|---|
| 12 个合法类型 | 12 | 全部放行 ✓ |
| 多行信息 / `Merge` / `Revert` | 3 | 全部放行 ✓ |
| 空提交信息 | 1 | 放行（交给 git 处理）✓ |
| `feat(scope):` 格式 | 1 | 拒绝 ✓ |
| 无方括号 | 1 | 拒绝 ✓ |
| 未知类型 `[功能]` | 1 | 拒绝 ✓ |
| 只有类型无说明 `[新增]` | 1 | 拒绝 ✓ |
| 说明过短 `[新增] ok` | 1 | 拒绝 ✓ |
| `feat:` 纯英文冒号 | 1 | 拒绝 ✓ |
| **带 BOM** | 1 | 拒绝，且提示信息含 BOM 说明 ✓ |
| 真实 `git commit` 端到端 | 3 | 拦截/拦截/放行，行为一致 ✓ |

### 18.5 修掉的一个自身缺陷

`commit-msg` 第一版里，BOM 分支是：

```sh
if [ "$BOM" = "efbbbf" ]; then
  fail          # ← fail 内部 exit 1，后面的提示永远执行不到
  echo "[commit-msg] 额外提示：检测到 UTF-8 BOM ..." >&2
  exit 1
fi
```

实测发现带 BOM 的提交被拒了，但报错只说「格式不合规」——
**提示是死代码**。已改为 BOM 独立成段，先于格式检查输出专门的说明。

这个 bug 只有真跑一遍才会暴露：代码看起来完全正常，`exit` 在 `fail` 里面，肉眼很难发现。

### 18.6 答辩时可直接用的说法

> 团队协作规范不是写在文档里就生效的，必须落到工具里。项目用 `pre-push` 保证 `dev` 随时可构建，
> 用 `commit-msg` 保证提交历史可读、可追溯。规范与实现的一致性通过钩子强制，
> 而不是依赖成员自觉。

### 18.7 仍未做

| 级别 | 项 | 说明 |
|---|---|---|
| P2 | 云端校验 | 钩子是本地的，`--no-verify` 可绕过。要彻底杜绝需 GitHub Actions 或服务端钩子 |
| P3 | 历史提交规范化 | 39 条非方括号提交保持原样，改写公共历史风险大于收益 |

---

## 19. 复审修复轮：review 40 项建议落地 + 设计体系收口（2026-11-18）

> 依据 `review/backend.md`（P0-2 项 + P1-14 项 + P2-40 项）与 `review/frontend.md`
> （P0-6 + P1-12 + P2-20 项）做的一轮系统化修复。review 目录为本地工作材料，不入库。

### 19.1 后端修复（oj-server，本轮全部完成）

| 来源 | 修复 | 位置 |
|---|---|---|
| P2-1/4 | 登出拉黑下沉 service；注册不再白签 JWT | AuthController / AuthServiceImpl |
| P2-5 | 删除不生效的 MetaObjectHandler（实体无 fill 声明） | MybatisPlusConfig |
| P2-7 | `validate` 从接口收窄为 private | SubmissionService(Impl) |
| P2-10~12 | 未使用 import 清理；登录失败键走 `RedisKeys.rate`（CONTRACT §5） | SecurityConfig / AuthServiceImpl / QueueMonitorServiceImpl |
| P2-13/14 | 42901 与 40000 手写码改枚举 | SubmissionServiceImpl / AnnouncementController |
| P2-16 | 限流阈值全部走 `oj.rate-limit.*`，改配置不再重编译 | AuthServiceImpl / application.yml |
| P2-17/18 | 竞赛列表 N+1 → GROUP BY 一次取全；详情 N+1 → selectBatchIds | ContestMapper / ContestServiceImpl |
| P2-19 | 对账任务 N 次 score() → rangeWithScores 一次取全 | LeaderboardReconcileTask |
| P2-22~25 | 分页下限夹取：新增 PageUtils（含 5 个单测），排行榜脏 member 跳过 | LeaderboardServiceImpl / AdminUserController / ProblemServiceImpl / SubmissionServiceImpl |
| P2-29~31 | 参数类异常逐类兜 40000（JSON/类型/缺参/超限）；兜底打请求上下文；JwtUtil 失败 debug 日志 | GlobalExceptionHandler / JwtUtil |
| P2-32/33 | 无效配置删除（logic-delete-field、mapper-locations） | application.yml |
| P2-34 | 队列不存在降级 0 + warn（新部署常态，不再 50000） | QueueMonitorServiceImpl |
| P2-35 | `cachedAt` → `queriedAt`（API.md v1.7 先行），改 LocalDateTime 走全局序列化，修掉 UTC「Z」格式 | QueueStatusVO / QueueMonitorServiceImpl / admin.ts / QueueMonitorView |
| P2-36~38 | 收藏并发双击幂等成功；收藏/详情同口径校验上架；注册并发撞唯一键转 40000 | FavoriteController / AuthServiceImpl |
| P2-39 | 禁止降掉最后一个管理员（40300 + 4 个单测） | AdminUserController |
| P2-40 | 语言白名单以 Language 枚举为唯一权威（DTO 正则会漂移；5 个单测） | ProblemServiceImpl |

P2-3（`incr` 无调用点）**有意保留**：写入属 oj-judge 职责（CONTRACT §7），保留是为接口语义完整，
已加注释说明。P2-26/27/28、P2-6/9、P2-15 部分在早前提交已落地；P2-21 无选课表、语义为全站口径，
已在 CourseMapper 注释标明为已知限制。

### 19.2 前端修复与设计（oj-web，本轮全部完成）

| 来源 | 修复 |
|---|---|
| P2-1/2/3 | 越权重定向给 ElMessage 提示；`router.afterEach` 消费 meta.title；补 `scrollBehavior` |
| P2-4/9 | 详情页 watch 路由参数重载；路由 id 数字校验（题/竞赛/提交/用例四处） |
| P2-5 | 全部 `ElMessageBox.confirm` 吞取消分支 |
| P2-6/7/8 | 提交筛选 NaN 防御；标签选项全量来源；统计条口径改排行榜全表聚合 |
| P2-10~16 | size=200 截断、冗余动态 import、ws.ts 复用 userStore 取 token、未用导入清理、onLanguageChange 补确认与回退 |
| P2-17 | logo/竞赛标题改 router-link/真按钮 + 全站 aria/focus-visible |
| P2-18 | SFC 硬编码色值 117 处 → 6 处（余下为 mask 蒙版与 cssVar 兜底值，属合理用法）；token 体系扩充语义色/奖牌/代码面板/渐变/品牌前景 |
| P2-19 | `npm run typecheck`（vue-tsc）+ `npm test`（node:test 直跑，18 用例，覆盖状态映射与时间格式口径） |
| P2-20 | EP 全局 locale zh-cn |

**设计体系收口（对应「前端设计也要优化」）**：
- 登录/注册改用 `AuthShell` 双栏品牌页（渐变深底 + 网格纹理 + 特性列表，窄屏收成单栏）
- 404 从静默重定向改为 `NotFoundView`（`</>404` 渐变字 + 返回入口 + 显示原始地址）
- MainLayout：管理端五项收进二级菜单（一级 10 → 6 项）、用户区头像 + 角色徽章 + 真按钮、
  路由切换 page 过渡、页脚、窄屏降级
- 编辑器 IDE 化补完（等宽字体/行高/平滑光标）、Monaco worker、`prefers-reduced-motion`、
  favicon（内联 SVG，图形与 logo 同源）、`theme-color`、noscript
- 全部 18 个路由视图单根校验通过（v-if/v-else 链合法），page 过渡可正常工作

### 19.3 数据与文档

- `sql/schema.sql` + [20261118 覆盖索引迁移](../sql/migrations/20261118_submission_covering_indexes.sql)：
  `idx_submission_status_user(status,user_id,problem_id)`（对账聚合）、
  `idx_submission_problem_user(problem_id,user_id)`（课程统计去重），可重复执行；README 部署段已更新
- API.md v1.6 → **v1.7**：队列监控 `cachedAt` → `queriedAt`（语义修正 + 时间示例对齐 §0.1）
- 竞赛排行明细串去 emoji（`A AC 85min (-1)` 纯文本，前端 mono 原样渲染）

### 19.4 回归验证（全绿）

| 检查 | 结果 |
|---|---|
| `mvn -B verify`（含新增 11 个后端单测） | ✅ exit 0 |
| `oj-web npm run build` | ✅ 1m20s |
| `oj-web npm run typecheck`（vue-tsc） | ✅ exit 0 |
| `oj-web npm test`（18 用例） | ✅ 18 pass / 0 fail |

### 19.5 仍未做（继承自 §8，本轮未动）

| 级别 | 项 |
|---|---|
| P0 | A 侧 real 判题引擎（HANDOFF-A.md 就绪） |
| P1 | 论文文档同步（§8 列表） |
| P2 | JMeter 并发复测、恶意样例库 10 例、compose 部署验证（需 Linux/Docker 环境，本机 Docker 未启动） |
| P3 | ESLint/Prettier 接入（已有 typecheck/test 兜底）、Pyodide 控制台、题解区 |

---

## 20. 精密工具风换肤 + 个人中心扩展（2026-11-19）

### 20.1 全站换肤：AI 模板脸 → 精密工具风（`468032c`）

用户反馈旧皮肤"AI 生成痕迹重"（靛蓝渐变横幅 + 玻璃拟态 + 全站胶囊圆角是三大模板味来源）。
经三方向对齐后选定 **Linear/GitHub/Vercel 式精密工具风**，全站 14 文件重做皮肤：

| 维度 | 旧 | 新 |
|---|---|---|
| 强调色 | 靛蓝渐变满天飞 | 全站唯一彩色 `#0969da`，语义色只进 7px 状态点 |
| 层次 | 阴影 + hover 上浮 | 发丝边框深浅，卡片零阴影零动效 |
| 字重/圆角 | 800 满天飞 / 胶囊 999px | 统一 600 / 芯片 4px 直角 |
| 数据 | 不定 | 等宽字体 + tabular-nums（评测工具的基本素养） |
| 登录页 | 双栏渐变品牌页 | 居中一栏式（词标 + 表单卡） |
| 领奖台 | 渐变彩底行 | 2px 名次顶线 + 实色小方块名次 |

实现杠杆：改 token 值 + 共享类，20+ 视图自动换肤；再清扫 8 个视图残留。
期间用 node 脚本核对 71 处 token 引用零缺失（发现并修复头像取色引用已删 token）。
**净删 254 行**——装饰代码变少本身就是方向的证明。

### 20.2 个人中心扩展：自定义头像 + 签名（API.md v1.8）

| 项 | 说明 |
|---|---|
| 数据 | `user` 表加 `avatar VARCHAR(255)`、`signature VARCHAR(200)`（schema + [可重复迁移](../sql/migrations/20261119_user_profile.sql)，本机库已执行） |
| 契约 | API.md v1.8：LoginVO 加 `avatar`/`signature`；新增 `PUT /api/auth/profile`（签名）、`PUT /api/auth/avatar`（data URL 上传）；`GET /uploads/**` 公开静态读取；排行榜 Item 加 `avatar` |
| 头像链路 | 前端 canvas 居中裁剪 256px → webp(85%) ≤几十 KB → data URL 上传 → 后端**白名单 MIME + 魔数二次校验 + 300KB 上限** → 服务端生成文件名落盘 → 旧头像文件清理 → 返回相对 URL |
| 安全 | 客户端文件名永不参与路径（防穿越）；魔数防"改前缀冒充图片"；文件名 `{userId}-{ts}.{ext}` |
| 静态映射 | `UploadResourceConfig`：/uploads/** → 磁盘目录（oj.upload.root-dir）；compose 加 `data/server-uploads` 持久卷 |
| ⚠️ oj-common | `User` 实体加两字段（公共区）——**需知会成员 A**，代码为零行为变更，judge 侧不读这两列 |
| 前端 | ProfileView 重写（资料头卡：头像 hover 更换遮罩 + 签名就地编辑 + 等宽 meta；账号安全双列；收藏用状态点）；顶栏/排行榜/领奖台接自定义头像（未设置回退首字母方头像） |

### 20.3 验证（全绿，真实接口实测）

| 检查 | 结果 |
|---|---|
| `mvn -pl oj-server test`（含 AvatarDataUrlTest 5 例：MIME 冒充/损坏 base64/超限/边界） | ✅ 43 pass |
| 前端 build + typecheck + 18 单测 | ✅ |
| 端到端：登录 → 签名中文写入/空签名拒绝 40000 → 头像上传 → 假 PNG 魔数拒绝 → info 回读 → `GET /uploads/...` 200 | ✅ 全通过 |
| 排行榜响应含 `avatar` 字段 | ✅ |

踩坑复现：Git Bash curl 发中文 body 是 GBK → 恰好落进 P2-29 新增的"请求体格式错误 40000"分支（修复前是 50000），
用 `--data-binary @utf8.json` 复测通过；Windows 下运行中的 jar 被文件锁占用，重建前必须先停服务。

### 20.4 个人主页二期：从"设置页"到"个人主页"（同日晚，API.md v1.9）

用户反馈个人中心"还是简陋、功能少、布局一般"，于是把页面从资料设置升级为活动总览：

| 区块 | 内容 | 数据源 |
|---|---|---|
| 数据 hero | 解题数 / 提交数 / 通过率 / **连续刷题天数** 等宽大数字行 | statistics + trend 计算 |
| **刷题热力图** | 近 26 周×7 逐日提交密度（GitHub 式，月份/星期标尺 + 五档色阶 + 图例） | `trend?days=182` |
| 语言分布 | 各语言提交/AC 横条 | `GET /api/submissions/languages`（v1.9 新增） |
| 最近动态 | 最近 8 条提交：状态点 + 题目链接 + 语言/耗时 + 相对时间 | `pageSubmissions(page=1,size=8)` |
| 收藏 / 账号安全 | 保留，双列收纳 | — |

后端配套：`trend` 的 days 上限 30 → **366**（API.md §4.4a，30 天正是"个人页没有活动历史"的根因）；
新增语言分布聚合（§4.4b，`languageDistByUser`）。前端 `utils/heatmap.ts` 纯函数栅格构建
（周日对齐、稀疏补零、未来格恒 0、streak 端日无提交回退一天计），**7 个 node:test 钉死全部 off-by-one**。

验证：后端 43 测试 ✅；前端 25 测试（+7 热力图）✅ typecheck/build ✅；
聚合 SQL 在真实演示数据上核对（用户 31：55 次 C 提交/22 AC，逐日趋势行正确）✅。

### 20.5 布局三期 + 演示数据清理（用户反馈驱动，2026-11-19 深夜）

用户两条反馈：①"3 号就有记录"——查明**页面无任何 mock**，是 11-16 演示数据脚本（§12.4）把
11-03~11-16 窗口的历史提交铺到了用户 54 的账号（DB 实证：多个账号的提交首末日期恰好落在该窗口）；
②布局失衡（语言分布卡大片留白、热力图月份标签重叠、两列高度错配）。

**数据清理（用户确认后执行）**：删除用户 54 的 37 条演示提交 + 关联 judge_case，
Redis 榜单 ZREM 同步（对账任务 5 分钟内也会自愈）。其统计/热力图归零，此后积累皆真实记录；
排行榜仍有 37 个演示账号支撑答辩效果。

**布局重构（参照 LeetCode 个人页结构）**：
- 热力图月份标签改与格子**同栅格对齐**（原绝对定位不跟随网格列 → 全部叠在左端的 bug）；
  格子 11px 定宽 + 3px 间距，不再 1fr 拉伸，横向滚动
- 新增**解题进度卡**：简单/中等/困难三行 solved/totalCount 进度条（数据来自 statistics.byDifficulty），
  与语言分布叠放填满左列，右列最近动态 8 行 → 两列高度平衡
- 卡片顺序对齐 LeetCode：hero → 热力图（全宽）→ [进度+语言 | 动态] → [收藏 | 安全]

回归：前端 25 测试 ✅ typecheck ✅ build ✅；trend 接口对已清空账号返回空数组 ✅。

---

## 21. 全站设计重构 v3：「墨卷」体系（2026-11-19 深夜）

用户要求全站所有页面重构 UI 与布局，"好看精致、眼前一亮、不千篇一律"。
先检索确认了编辑风（Warm Editorial）的公认公式——暖纸底 + 近黑墨字 + 单一深色强调 + 衬线大标题
（[FreeDesignMD](https://freedesignmd.com/lexicon/editorial-web-design)、
[PageProofer 对 Every 的拆解](https://pageproofer.com/inspiration/warm-editorial/every)、
[Tubik 编辑设计集](https://tubikstudio.com/blog/media-editorial-website-design)），
再结合评测平台"作答—批改"的领域本质，自行设计出**「墨卷」**体系（不照搬任何一家）：

> 概念：判题如批改。平台是一张数字稿纸——学生落笔（提交代码），系统批卷（运行用例），
> AC 得绿批，错误落朱批。暖纸底、松绿强调、朱印点睛、宋体标题、等宽数据。

### 21.1 配色（自拟，全部脱离 Tailwind/GitHub 默认色号）

| 角色 | 色值 | 用途 |
|---|---|---|
| 纸 paper | `#f6f4ec` | 页面底（暖，与冷灰 SaaS 脸区分） |
| 卡 card | `#fffef9` | 暖白卡片 |
| 墨 ink | `#232d26` | 主文字（墨绿黑） |
| 松 pine | `#17694a` | 全站唯一强调色：链接/主按钮/选中 |
| 绿批 success | `#1d7a44` | AC/通过 |
| 朱批 danger | `#bc3f2c` | WA/CE/编译失败；`--oj-seal` 仅品牌与第一名小印 |
| 赭批 warning | `#a26a15` | TLE/MLE/进行中 |
| 青磷 info | `#2b7a8c` | 判题中 |
| 松烟墨代码面板 | `#15201a` 系 | 样例/编辑器/查看器（绿相深底） |

字体三层：宋体系 `--oj-font-display`（页面 h1/卡片 h2/词标/页脚格言，书卷气）
+ 系统黑体（正文）+ JetBrains Mono（数据/代码，工具感）。衬线与等宽各司其职。

### 21.2 品牌记号与组件

- **方印 logo**：朱底白「判」衬线字，MainLayout/AuthShell/favicon 三处同源
- **朱印句读**：全站页面级 h1 前置 8px 朱色小方块
- **笺纸统计条**：oj-banner 铺与行高呼应的极淡稿纸横线
- **稿纸登录页**：AuthShell 整页铺竖向稿纸线，表单卡如答题卡置于其上
- **名次印**：排行榜前三改印章式——状元朱印/榜眼墨方/探花赭方，衬线数字
- 页脚衬线格言："判题如批改，落笔即成长"
- 热力图绿阶整体转暖（`#cbe3cf`/`#7abf8b`/`#3d9463`）

### 21.3 落地方式与验证

token 层全量重映射（93 个变量，71 处引用经 node 脚本核对零缺失），
20+ 视图自动换肤；手工清扫：热力图绿阶、排行榜名次印、StatisticsView ECharts 兜底色。
回归：typecheck ✅ / 25 单测 ✅ / build 45s ✅。

### 21.4 v3.1「给墨卷加灵魂」：墨色舞台 + 纸张噪点（用户反馈"不丑但没感觉"后）

诊断：平面纸卡 + 均匀浅底 = 有质感没节奏。解法是给页面明暗节奏与肌理：

- **`.oj-stage` 墨色舞台**（index.css 共享组件）：每页顶部深松烟墨横幅——
  纸白衬线大标题（26px）+ 朱印 + 暗稿纸竖线 + 纸白等宽数据列。
  页面节奏变为 明（纸）→ 暗（墨）→ 明（纸卡）。
  落地页：题库/竞赛/提交记录/排行榜/统计（后三页借此补上了缺失的 h1，顺带修可访问性）
- **个人中心资料头转墨色舞台**：纸白首字母头像反转配色，签名/数据行纸上墨显
- **竞赛详情/题目详情头部转墨色**：倒计时纸白等宽 26px，状态呼吸点绿
- **登录页全暗稿纸**：深松烟墨整页 + 绿相稿纸竖线 + 噪点，纸卡浮于墨卷之上
- **纸张噪点**：body 背景叠加 feTurbulence 4% 透明度噪点层，给纸面"纤维"质感（纯 CSS，无图片资源）
- 清理 ContestListView 遗留死样式（page-head/hs-* 三段）

回归：typecheck ✅ / 25 单测 ✅ / build 45s ✅。

---

## 23. 设计 v4「墨夜」：全站切换 Linear 暗色系统（2026-11-19 深夜）

前三版（靛蓝渐变 / 平面灰 / 暖纸文房）均被否。本轮放弃自创，按用户要求直接采用被验证的设计系统：
[Linear 暗色 token 规格](https://open-design.ai/plugins/design-system-linear-app)（近黑 #08090a +
薰衣草靛蓝 #5e6ad2 单强调 + 亮度分层）忠实落地。

### 23.1 落地内容

| 层 | 改动 |
|---|---|
| token | 全量按 Linear 规格重写：bg #08090a、卡片 #101012 + 顶部内高光渐变、边框 rgba(255,255,255,0.08)、文字四级灰（#f7f8f8/#d0d6e0/#8a8f98/#62666d）、强调 #5e6ad2、语义色暗底亮档（#4cb782/#eab308/#f87171） |
| Element Plus | 引入 `dark/css-vars.css` + `html.dark` 常驻 + 12 个 --el-* 变量对齐（表格/输入/下拉/对话框全暗） |
| 舞台/hero | 暗稿纸线 → **品牌靛蓝径向辉光**（radial 28%→transparent），朱印改靛蓝印 |
| 字体 | 弃衬线（Linear 无衬线），Inter 系统栈 + ui-monospace；字重收敛 600 |
| 视图清扫 | 热力图暗底亮绿阶、名次印/头像反转、6 处白底残留→面板色、题面交互件暗化、404 提对比度 |
| 首页 hero | 双辉光（靛蓝+紫罗兰）+ 幽灵按钮 |

### 23.2 验证

typecheck ✅ / 25 单测 ✅ / build 1m23s ✅；grep 审计零浅底残留。
**注意**：本机 Edge headless 启动被环境拦截（多轮尝试失败），本轮无法截图自证——
需要用户在内置浏览器确认；已知未解问题：v3.1 时代首页曾出现约 3000px 的空白拖尾（见 §21.4 截图记录），暗色下是否复现待用户反馈。

---

## 22. 页面扩容：首页落地页 + 公告中心 + 错题本 + 帮助与关于（API.md v1.10，2026-11-19）

用户反馈"页面太少"。新增 4 个页面（全部走墨卷体系，数据来自真实接口）：

| 页面 | 路由 | 说明 |
|---|---|---|
| **首页落地页** | `/home`（`/` 重定向至此，logo 同步指向） | 全站最大排版：46px 宋体标语"落笔，即见分晓。"压在墨卷 hero 上（CTA 双按钮 + 四项全站数据）；四张能力卡；最新公告 ×3 + 榜单前五双列。数据全部来自公开接口（题库概览/榜单 summary/公告），未登录可完整展示 |
| **公告中心** | `/announcements` | 公告列表（"告"字朱印 + 标题 + 日期 + 正文），公开 |
| **错题本** | `/wrongbook`（登录） | 尝试过且未 AC 的题：题目/难度/最近状态点/尝试次数/最近提交/重做入口 |
| **帮助与关于** | `/about` | 四卡：四步上手指南、九态判题状态对照表（复用 STATUS_MAP + statusColor）、FAQ 折叠、技术栈清单——答辩讲解页 |

后端：新增 `GET /api/submissions/wrongbook`（§4.4c）——窗口函数一步完成"每题尝试次数 + 最近一次提交 + 是否 AC 过"
（ROW_NUMBER/COUNT/MAX OVER PARTITION BY），已 AC 的题自动移出错题本。
导航更新：+公告、+错题本（登录可见），logo 与 `/` 均指向 `/home`；页脚加"帮助与关于"。

验证：错题本聚合在真实数据上核对（用户 17：题 21 JavaScript 题 1 次尝试、最近状态 WA=3）✅；
wrongbook 空账号返回空数组 ✅；typecheck ✅ / 25 单测 ✅ / build 48s ✅。

---

## 24. 全站设计体系 v6「工作台」（2026-11-19）

### 24.1 方向确立

v5「校样」采用纸墨底、宋体标题、朱砂方印和报纸/书目式叙事，首页验收后被用户明确否定：
“看起来不像是做题的，像是作家助手”。本轮不再直接铺全站，而是先做 3 版 1440 宽首页样张：

| 方向 | 特征 | 结果 |
|---|---|---|
| **A · 工作台** | 冷灰白底、白色面板、蓝色只做操作信号、右侧判题终端 | **用户选定，作为 v6 全站基准** |
| B · 竞技场 | 深色底、荧光绿、实时榜单 | 未选 |
| C · 工程蓝图 | 浅蓝网格、判题管线、橙色测量标记 | 未选 |

设计原则：页面之间靠信息结构区分，不靠装饰母题；不用纸纹、宋体、印章、大圆角和重投影。

### 24.2 Design token 与组件

| 项 | v6 值 / 行为 |
|---|---|
| 画布 / 面板 | `--oj-bg #f4f6f9`、`--oj-card #ffffff` |
| 操作信号 | `--oj-primary #2855ff`（蓝只用于按钮、链接、选中态和实时状态） |
| 文字 | `--oj-text #101828` / `--oj-text-sub #667085` |
| 分隔线 | `--oj-border #e4e8ee`，一细线到底 |
| 代码面板 | `--oj-code-bg #0d1117`，代码与数据统一等宽 |
| 字体 | 标题改无衬线系统栈；等宽字体承担题号、用时、内存、通过率等数据 |
| 圆角 / 阴影 | `--oj-radius 10px`；面板以边框和底色分层，不使用重投影 |
| 表格 | 新增 `.admin-table-shell`，管理端表格与分页收进同一张卡；禁用主按钮补灰色 `is-disabled` 样式 |
| 导航 | 62px 白色吸顶导航；≤860px 改两行换行，全部导航项可见；管理台左轨 ≤960px 横向换行 |

### 24.3 页面结构（22 页）

| 页面组 | v6 结构 |
|---|---|
| 首页 | 终端英雄区（`submit --problem` 命令 + 判题输出）+ 平台读数 + 题目表 + 周榜 |
| 题库 / 题目详情 | 左侧筛选轨 + 题目列表；详情为题面/代码双栏，未提交时右栏展示“判题流程”四步卡（提交入队/编译构建/逐用例运行/实时判定），语言标签前补“支持语言” |
| 提交 / 判题结果 | 统计条 + 筛选 + 表格；结果页状态标题 38px，用例表 + 源码面板；46px 巨型红字压重问题已收掉 |
| 统计 / 排行榜 | 摘要 + 难度覆盖 + 环形图 + 七日趋势；排行榜领奖台 + 完整榜单 |
| 竞赛 | 深色 LIVE 面板 + 赛程卡；详情页概览 + 倒计时 + 题目 Tabs |
| 公告 / 错题本 / 关于 | 公告白卡列表（去掉每卡重复日期 footer）；错题本表格 + 规则提示；关于页文档导航 + 9 种判题状态表 + FAQ |
| 个人中心 | dossier 布局：资料卡 + 签名编辑 + 热力图（20px/4px，1024 下 16px/3px）+ 语言分布 + 最近动态 |
| 管理端 | 题目/竞赛/课程/用户/用例/队列六页；左轨 + 表格 shell；用例导入 0 组时按钮明确不可用 |
| 认证 / 404 | `AuthShell` 增加 `mode: 'login' \| 'register'`，注册页文案与终端命令改为 `auth --register`；404 补对比度 |

### 24.4 视觉核验修复

- `MarkdownPreview` 深色样例代码内 `<strong>` 继承浅色导致“输入/输出”标签几乎不可读，改为深底可读色。
- 题目详情未提交时右栏原本空白，新增四步判题流程卡。
- 判题结果状态标题 46px → 38px，收紧字距。
- 首页平台读数数字与单位间距 3px → 5px。
- 公告卡删除重复 footer，空态补白色卡。
- 个人中心“编辑”按钮改为“编辑签名”；热力图从 11px 格子放大到 20px/4px，1024 下切 16px/3px，消除横向滚动条。
- 管理端删除“自定义表头 + el-table 表头”重复；六页表格统一 `.admin-table-shell`。
- 禁用态主按钮补灰；题库四列读数 ≤700px 改 2×2；竞赛页 ≤720px 标题降 24px 并 `text-wrap: balance`。
- 移动端顶部导航从截断改为两行换行；管理台左轨 ≤960px 横向换行。
- 全路由 1440/1024/390 横向溢出检查通过；`page-enter` 过渡在 headless 截图下会卡 `opacity:0`，截图脚本强制清 class 规避。

### 24.5 验证

- `npm run typecheck` exit 0。
- `npm test` 25/25 通过。
- `npm run build` exit 0；产物 `oj-web/dist`，仍有 Monaco/ECharts 大 chunk 警告，属既有依赖体积。
- 22 页 1440 截图 + 11 页全页截图；1024/390 响应式抽检；全部检查通过。

### 24.6 本地预览身份事故与修正

本机 MySQL/Redis/Java 后端未运行，视觉验收临时在 8080 起 mock API。mock 的
`/api/auth/login`、`/api/auth/info` 一度返回硬编码假用户「预览账号」、`avatar:null`；
前端 `refreshUserInfo()` 会把接口返回的 `username` 回写 localStorage，`avatar` 为空时删除
`oj_avatar`，导致登录后的顶栏和个人中心被假身份覆盖。真实数据库未发生写入。

修正：mock 改为返回真实账号（昵称、userId、头像与角色按真实账号对齐），并补 `/uploads`
静态头像响应；`/api/admin/users` 的首条假用户同步移除。**后续所有本地 mock 不得再伪造用户身份**：
无真实后端时应返回空态/访客，或按真实账号返回，避免覆盖前端已登录资料。

### 24.7 已知项

- 真实后端未启动时，个人中心的邮箱、签名、刷题统计等仍来自预览数据，不能当作真实库数据。
- 设计已通过自动检查，仍待用户最终逐页确认；`oj-web/design/preview` 下旧 v3/v4/v5/v6 截图不是当前版本。

## 25. 账号设置页与编辑器偏好（2026-11-19）

### 25.1 问题

此前账号相关能力分散在个人中心：邮箱、密码、头像、签名各自成块，缺少统一入口，也没有可复用的
编辑器偏好。对毕业设计交付来说，“没有设置页”会直接削弱产品完整度，因此本轮把账号资料、
登录安全、编辑器偏好和本地数据收口到独立设置页，并同步接入题目详情页的在线编辑器。

### 25.2 页面与入口

| 项 | 实现 |
|---|---|
| 路由 | `GET /settings`（前端路由，`requiresAuth: true`，标题「账号设置」） |
| 顶栏入口 | 用户下拉菜单新增「账号设置」，命令 `settings` 跳转 `/settings` |
| 个人中心入口 | 原「账号安全」邮箱/密码表单改为设置入口卡，显示“打开账号设置”，避免两处维护同一套表单 |
| 页面结构 | 页头返回个人中心 → 当前账号身份条（头像、昵称、角色、UID、邮箱、登录状态）→ 左侧粘性分区导航 + 右侧四块面板 |
| 01 账号资料 | 用户名/角色只读；邮箱可编辑；签名 200 字计数；只提交实际变化的字段 |
| 02 登录安全 | 原密码、新密码、确认新密码；6–32 位与两次一致校验；成功后清本地登录态并跳转登录页 |
| 03 编辑器偏好 | 默认语言、字号 12–20、缩进 2/4、自动换行、代码地图；深色预览区实时反映字号/缩进/换行 |
| 04 本地数据 | 说明偏好仅存 localStorage；提供刷新账号资料与恢复默认偏好 |

### 25.3 偏好模型与编辑器接入

- 新增 `oj-web/src/utils/preferences.ts`：定义 `EditorPreferences`、默认值、字号范围 12–20、
  缩进白名单 2/4，并用 `normalizeEditorPreferences()` 收敛 localStorage 中的旧值/非法值。
- 新增 `oj-web/src/stores/preferences.ts`：Pinia store，key `oj_editor_prefs`；默认字号 14、
  缩进 4、自动换行关、代码地图关、默认语言 `cpp`；`reset()` 恢复默认，`watch` 自动持久化。
- `CodeEditor.vue` 创建 Monaco 时读取字号、行高（字号 × 1.55）、缩进、换行与 minimap；
  在编辑器挂载后监听偏好变化并调用 `updateOptions()`，设置页修改后当前页面即时生效。
- `ProblemDetailView.vue` 的语言初值改为偏好中的默认语言；若题目不支持该语言，仍回退到题目
  允许语言的第一项，不改变原有提交约束。

### 25.4 资料、安全与头像

- 邮箱/签名保存前在前端校验邮箱格式、禁止清空已有邮箱、禁止清空签名；按变化分别调用
  `PUT /api/auth/info` 与 `PUT /api/auth/profile`，成功后重新拉取 `GET /api/auth/info`。
- 头像选择限制 PNG/JPG/WebP、原图 ≤5MB；浏览器端居中裁成 256×256，优先 WebP（失败回退 JPEG），
  再调用 `PUT /api/auth/avatar`。裁剪/解码失败时给出明确提示，不把异常留成未处理的 Promise。
- 密码修改调用 `PUT /api/auth/password`；成功后清理本地 token、断开 WebSocket 并跳转
  `/login?redirect=/settings`，避免旧令牌在页面上继续使用。
- 设置页只读写既有认证 API，不新增后端字段；编辑器偏好不写账号数据库。

### 25.5 验证

- `npm run typecheck` exit 0。
- `npm test` 29/29 通过（新增 4 项偏好归一化用例：空值/非对象、字号越界与小数、缩进白名单、
  语言白名单与布尔类型）。
- `npm run build` exit 0；产物 `oj-web/dist`，Monaco/ECharts 大 chunk 警告仍是既有依赖体积。
- 运行时核验（本地 mock + 真实账号身份）：
  - 1440 / 390 设置页无横向溢出；390 下设置面板改为单列，保存/修改密码按钮铺满。
  - 非法邮箱触发「请输入合法的邮箱地址」；合法邮箱与签名保存后接口状态同步更新。
  - 偏好改动写入 `oj_editor_prefs`；恢复默认后回到 14px / TAB 4 / 不换行。
  - 题目详情页实测偏好生效：字号 18px、行高 28px、TAB 2、自动换行、minimap 开启，默认语言
    `js` 正确显示为 JavaScript。
- 新增预览资产：`v8-settings.png`、`v8-full-settings.png`、`v8-settings-390.png`、
  `v8-full-settings-390.png`、`v8-profile.png`、`v8-full-profile.png`、`v8-full-profile-390.png`。

### 25.6 边界与预览说明

- 本机没有真实后端，设置页写操作由临时 mock 承接；mock 的 PUT/注销处理只用于预览，不是项目源码，
  也不写入数据库。真实后端契约以 `docs/API.md` §1.4–1.6 为准。
- 设置页头像上传依赖浏览器 `createImageBitmap` 与 Canvas；现代 Chromium/Edge/Firefox 可用，
  不支持时提示图片处理失败。
- 偏好是浏览器本地数据，换设备或清理站点数据后回到默认；这是产品设计，不是同步缺陷。

---

## 26. 全站深审优化轮：3 安全 P0 + 体验批量修复 + 编辑器升级（2026-11-21）

本轮由三次并行代码审计驱动（前端逐文件 / oj-server 业务逐文件 / 前后端契约对齐），审计确认
整体契约对齐质量高（API 路径/参数/字段、WS 协议、路由守卫零不一致），在此基础上落地以下改动。

### 26.1 安全与正确性 P0（后端）

| # | 问题 | 修复 |
|---|---|---|
| 1 | `AuthController.clientIp` 无条件信任 `X-Forwarded-For` 首段：`$proxy_add_x_forwarded_for` 会把客户端自带的伪造头拼在前面，每次请求换个假 IP 即绕过注册/登录限流与失败锁定 | 只信任 Nginx 写入的 `X-Real-IP`（`$remote_addr`，不可伪造），直连回退 `getRemoteAddr()` |
| 2 | `docker-compose.yml` 的 `JWT_SECRET` 有仓库内默认值回退，生产忘设环境变量时任何拿到仓库的人可签发任意 userId/role 的令牌 | 改为 `${JWT_SECRET:?}` 强制必填，缺省直接启动失败并给出生成指引 |
| 3 | 竞赛排名罚时把「非 AC 且非 CE」一律计错误：排队中(0)/判题中(1)/系统错误(8) 也被罚 20 分钟，判题未完成时排名被污染 | 新增 `isWrongAttempt()` 口径：仅 WA/TLE/MLE/RE 计罚，CE 沿用惯例不计 |
| 4 | 时钟混用：compose 容器无 TZ（JVM 走 UTC），而 `createQueued` 硬编码 Asia/Shanghai，同一张表两种时钟 | compose 为 oj-server/oj-judge/mysql 统一 `TZ: Asia/Shanghai`（与 API.md §0.1「前端不换算」约定一致） |

### 26.2 前端 P0

- **TestCaseAdminView**：`watch(route.params.id)` 在离开本页时（params 变 undefined）触发「编号不合法」
  兜底，把用户正要去的导航劫持回题目列表——加 `if (!route.params.id) return` 守卫。
- **ProfileView**：`onMounted` 首行 `refreshUserInfo` 无 catch，非 40100 失败时 5 个数据请求全不发，
  整页假空态 + unhandled rejection——try/catch 后继续走 allSettled 块。
- **ProblemDetailView**：WS `JUDGE_FINISHED` 分支丢 `timeUsedMs/memoryUsedKb`（仅轮询路径才有副标题），
  推送帧字段补全。

### 26.3 编辑器与题目页升级（新功能）

- **代码草稿自动保存**（新增 `utils/draft.ts` + 7 个单测）：按「题目+语言」维度落 localStorage，
  600ms 防抖；模板原文/空串不落盘；卸载前同步落盘一次防抖尾不丢；恢复时编辑器脚注显示
  「已恢复草稿」、保存后显示「草稿已保存 HH:mm」；提交成功后保留草稿（LeetCode 同款行为）。
- **Ctrl/⌘ + Enter 快捷提交**：Monaco 未占用该组合键，window keydown 编辑器内外都生效。
- **未登录提交按钮可点击**：跳 `/login?redirect=/problems/:id`，不再是禁用死按钮。
- **判定卡家族色**：绿（AC）/橙（TLE·MLE·CE）/红（WA·RE·SE）三族配色 + `statusGlyph`（✓/!/✕），
  替换原二值 is-ok/is-bad；`utils/status.ts` 的 STATUS_COLOR 重构为 token 名 + `statusColor()` 统一解析，
  ECharts 环形图与全站同族同色（无 DOM 环境回退 `var(--oj-x)` 串，既有单测契约不变）。
- **轮询容错**：连续失败 3 次才中止（原单次网络抖动即清空判题进度）。

### 26.4 体验与一致性批量（P1）

- 开放重定向防护：LoginView 的 `redirect` 只放行站内路径（拦 `//evil.com`、反斜杠、绝对 URL）。
- 注册页前端校验对齐后端 @Valid（用户名 2-50 位字符集、邮箱格式、密码 6-32）。
- 题库筛选与 URL 双向同步：刷新/分享链接还原筛选与页码，浏览器前进/后退同样还原（带回环守卫）。
- 管理端搜索重置页码（题目/用户）；`edit()` 先取详情再开弹窗（题目/竞赛/课程，失败不开半空弹窗）；
  公告发布 loading 防双击、公告删除加确认、用户提权/降权加确认（与其余敏感操作同口径）。
- 提交记录时间范围改动即查（与状态选择器同口径）；竞赛已结束不再跑秒级定时器。
- ProfileView 语言标签统一走 `LANGUAGE_OPTIONS`（python3 → Python3，与提交记录页一致）。

### 26.5 工程卫生

- `.gitignore` 补 `deploy/web-dist/`（预构建产物此前处于未跟踪裸奔状态）。
- 删除 oj-server 空 `resources/mapper` 目录；AboutView 4 处 `chapter-line` 死节点与死样式；
  router 「meta.title 从未消费」的历史注释（早已消费）。
- index.css 清理 16 个零引用的 v4/v5 兼容别名 token（gradient-*/on-brand-*/glow/grid-line/code-inset/
  shadow-brand/paper/card-hl/gold-light/silver-light/primary-deep/radius-sm/shadow-lg/info-soft/
  success-border/accent/pulse-*），并做全量 token 引用完整性校验（67 个引用全部有定义）。
- 根目录 `.tmp-*.log` 调试残渣清理（2 个被运行中进程占用，属 gitignore 范围不影响仓库）。

### 26.6 验证

- 前端：`npm run typecheck` exit 0；36/36 单测（+7 草稿用例）；`npm run build` exit 0。
- 后端：`mvn verify` 通过（oj-server 43/43，oj-common/oj-judge 原量通过）。
- compose YAML 语法校验通过。

### 26.7 待办与知会（成员 A）

- deploy/docker-compose.yml 的 TZ 与 JWT_SECRET 变更属共用区改动，已按 §3.4-6 知会：
  **服务器 .env 已有 JWT_SECRET 时无需操作**；若 .env 缺该键，`docker compose up` 会启动失败，
  按 `.env.example` 补上即可。TZ 变更会让所有容器新写入的时间戳统一为北京时间。
- 下次部署按 DEPLOY-SERVER-RUNTIME.md §4.3 重传 web-dist（本轮前端有大量变更）。

---

## 27. 功能扩容轮 + 二次复查（2026-11-21）

### 27.1 新增四功能

| 功能 | 说明 |
|---|---|
| **全局命令面板 Ctrl/⌘+K**（`CommandPalette.vue` 新组件） | 顶栏搜索入口 + 快捷键呼出；无输入列页面清单，有输入防抖搜题（版本号防竞态），`#编号` 直达；↑↓/Enter/Esc 全键盘操作；上下文切换式列表避免键盘索引空间重叠 |
| **题库行内"我的状态"**（API.md v1.12 §4.4d） | 新端点 `GET /api/submissions/me/status-map`：一条聚合返回本人 AC 过/尝试过的题目 ID 集合（`MAX(status=2)`），题库页行首渲染绿勾 ✓（AC）/橙点（尝试过），替代逐题查询的 N+1。登录/登出切换与从题目页返回时自动刷新 |
| **题目页"我的提交历史"侧栏** | 复用 `pageSubmissions?problemId` 取最近 5 条，状态同族色圆点 + 语言 + 相对时间；进页/WS 判定完成/轮询终态三处刷新，对着上次的错误改代码不必切页 |
| **统计页能力雷达** | 三难度解题率 + 总体通过率的 ECharts 雷达（复用 statistics 数据零新请求），chart-grid 改 auto-fit 三列，实例随 unmounted dispose |

### 27.2 复查轮修复（复查 Agent 报告 9 项确信问题，全修）

- **P1**：Ctrl+Enter 绕过 submitting 互斥 + OS 键盘重复连环触发（补 `if (submitting.value) return` 与 `e.repeat` 守卫）；题库 URL 双向同步把 keyword v-model 变成逐键发请求（query watch 增加回声比对，与自身 replace 一致只刷标记不发请求）；index.css 清理 token 时漏掉自身 3 处消费（`--oj-paper`/`--oj-primary-deep`/`--oj-shadow-lg` 悬空 → EP 填充透明/主按钮按压透明/对话框阴影消失，改引现存等值）
- **P2**：切题后"草稿已保存"显示旧题时间戳（load 时重置）；切题 600ms 防抖尾丢失（route watch 先 flush 旧题再 load）；竞赛组件复用切赛后秒表冻结/空转（timer 启停决策挪进 load()）；公告删除失败未捕获；networkErrorMessage 对 401/403 的弱文案
- **P2-部署项**：TZ UTC→Asia/Shanghai 后存量时间戳一次性 +8h 修正（Redis 标记 `tz-fix-20261121` 幂等保护，生产受影响行极少）

### 27.3 协同

- A 侧在 dev 收口 API.md 至 v1.11（v1.7 totalCases/内存下限/queriedAt 更名 + B 侧 v1.8~v1.10 重编号），本轮合并冲突按"保留收口 + 新增 v1.12"解决；提交详情 `totalCases` 与内存下限说明保留。

### 27.4 验证与部署

- typecheck ✓ / 36 单测 ✓ / build ✓ / mvn verify BUILD SUCCESS ✓
- 生产部署 HEAD `5247d8d`：重建 oj-server（新端点）+ oj-web（`index-Djbwm5H6.js`）；验收：登录 → 提交 A+B → 9 秒 AC → `/me/status-map` 返回 `{"acProblemIds":[1],"attemptedProblemIds":[]}`；头像/题库/首页探针全过
