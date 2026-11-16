# CodeOJ 项目进度文档

> 最后更新：2026-11-17 00:50 ｜ 维护：成员 B ｜ 更新纪律：每完成一个里程碑/修复一批 bug，更新本文档
> 配套文档：[API.md](./API.md)（接口契约 v1.3）｜ [CONTRACT.md](./CONTRACT.md)（模块契约 v1.1）｜ [COLLABORATION.md](./COLLABORATION.md)（分工+分支规则，§3 已改为新手可跟做版）｜ [HANDOFF-A.md](./HANDOFF-A.md)（A 侧交接，新增 §0 上手第一步）｜ [PROGRESS.md](./PROGRESS.md)（本文档）

---

## 1. 项目概况

| 项 | 值 |
|---|---|
| 项目 | CodeOJ —— 基于 Spring Boot + Vue3 + Docker 的在线代码评测与编程练习平台（专科毕业设计） |
| 分工 | 方案 A（已固化）：成员 A = 评测链路（oj-judge real 引擎/judge-images）；成员 B = 业务与前端（oj-server/oj-web/联调/测试） |
| 技术栈 | JDK17 / Spring Boot 3.2.5 / MyBatis-Plus / RabbitMQ / Redis / MySQL 8 / docker-java / Vue3+Vite+TS+Pinia+EP+Monaco / ECharts / SockJS+STOMP / Web Worker 沙箱 |
| 仓库 | github.com/huayuehua15-netizen/codeoj ｜ 分支：**main**（发布快照）/ **dev**（集成）/ **niuzj**（成员 B 个人分支）/ feat/*（成员 A） |
| 交付状态 | **B 侧功能 100% 交付 + 复审修复 + 三大数据页专业化 + 提交链路三页 UI 专业化 + judge_case 演示明细**；A 侧 real 引擎待开工（材料就绪） |

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

### 当前位置
**B 侧全部完成**：功能面 + 复审修复 + 六个核心页面专业化（排行榜/统计/竞赛 + 提交记录/判题结果/题目详情）+ 演示数据（1053 条提交 + 2451 行判题明细）。
P0 = A 侧 real 引擎（HANDOFF-A.md 就绪，`ENGINE_MODE=local` 目前是静态语义判定）。

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

## 7. 分支与协作模型（2026-11-16 简化，见 §12.5）

```
成员 A: feat/judge-engine ─┐
                           ├─→ 推自己分支 + 群上说一声
成员 B: niuzj ─────────────┘              │
                                          ▼
                    成员 B 统一合并 → dev（集成，随时可跑）
                                          │ 验收通过
                                          ▼
                                        main（答辩快照）
```

- **规则**：`dev` 与 `main` **只由成员 B 合并**；成员 A 只推自己分支并知会
- **理由**：合并权收在一人手上，`dev` 才能随时可跑
- **门禁**：合并进 dev 前必须 `mvn clean install -DskipTests` 通过
- **提交格式**：`[新增]/[修复]/[bug]/[重构]/[样式]/[文档]/[杂务] 做了什么`
- **详细操作**：COLLABORATION.md §3 已重写为新手可跟做版（五步流程、冲突处理、命令速查）
- **副本纪律**：`Documents\GitHub\codeoj` 只 pull/查看，不在其中改代码提交（防分叉）

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
| 分支规则 | `dev` / `main` **只由成员 B 合并**；成员 A 只推自己分支并知会（§7） |
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