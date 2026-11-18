# CodeOJ 接口文档（前后端对接契约）

> 版本：v1.8（2026-11-19）｜ 个人中心功能扩展：LoginVO 增加 `avatar`/`signature` 字段；新增 `PUT /api/auth/profile`（签名）与 `PUT /api/auth/avatar`（头像，data URL 上传）；静态资源 `GET /uploads/**` 公开可读。v1.7：队列监控 `cachedAt` 更名为 `queriedAt`
> 适用：前端（oj-web）按此文档先行开发，后端按此文档补全实现。
> 标注 ⚠️ 的接口：骨架已定契约，业务实现于第二阶段完成（前端可先按 mock 开发）。

## 0. 通用约定

### 0.1 基础信息

| 项 | 值 |
|---|---|
| Base URL | 开发环境直接走 Vite 代理（`/api` → `http://localhost:8080`）；生产环境由 Nginx 反代 |
| 风格 | RESTful，前缀 `/api` |
| 认证 | JWT，请求头 `Authorization: Bearer <token>`，24 小时有效 |
| 内容类型 | `application/json; charset=utf-8` |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`，服务端统一 UTC 存储（论文 4.4），前端展示转本地时区 |

### 0.2 统一返回结构（论文 4.5）

```json
{ "code": 0, "message": "成功", "data": { ... } }
```

`code = 0` 表示成功；非 0 见下方错误码表。HTTP 状态码恒为 200（业务语义全部由 code 表达）。

### 0.3 错误码表

| code | 含义 | 触发场景 |
|---|---|---|
| 0 | 成功 | — |
| 40000 | 请求参数错误 | DTO 校验失败（字段格式、长度） |
| 40002 | 题目不存在或未上架 | 提交时题目不可用（论文 5.3） |
| 40003 | 提交参数不合法 | 语言不被题目允许 / 代码超过 64KB |
| 40100 | 未登录或登录已过期 | token 缺失/伪造/过期/已登出（黑名单） |
| 40101 | 账号锁定 | 连续 5 次密码错误，锁定 15 分钟 |
| 40300 | 无权限访问 | 普通用户访问管理员接口、查看他人代码 |
| 40400 | 资源不存在 | 题目/提交/课程不存在 |
| 42900 | 请求过于频繁 | 限流计数超窗口 |
| 42901 | 提交过于频繁 | 同用户同题目 2 秒内重复提交（幂等锁） |
| 50000 | 系统错误 | 服务端异常 |
| -1 | 接口尚未实现 ⚠️ | 第二阶段补全（仅占位接口） |

### 0.4 分页约定

分页参数：`page`（从 1 开始）、`size`（默认 10）。分页响应结构（MyBatis-Plus Page）：

```json
{
  "code": 0, "message": "成功",
  "data": {
    "records": [ ... ],
    "total": 42,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

---

## 1. 认证模块 `/api/auth`

### 1.1 注册 `POST /api/auth/register`（公开）——成功返回 `data: null`

```json
{
  "username": "张三丰",
  "password": "123456",
  "email": "zhangsan@example.com"
}
```

| 字段 | 必填 | 校验 |
|---|---|---|
| username | ✅ | 2-50 位，支持中文/字母/数字/下划线，全局唯一（`@Valid` 服务端校验） |
| password | ✅ | 6-32 位，服务端 BCrypt 加盐存储 |
| email | ❌ | 合法邮箱格式 |

> 昵称字段已移除：`user.nickname` 列保留（论文表 4-1），业务上默认与用户名一致，展示统一使用 username。

限流：按客户端 IP 滑动窗口 **10 次/小时**，防脚本批量刷号（正常人工注册远用不完；超限返回 42900。计数与过期由 Redis Lua 原子执行，机制同论文 4.7）。

响应 `data`：`null`（**注册不自动登录**——成功后前端跳转登录页，携带用户名预填）。

> 设计说明：注册与登录分离，注册接口不签发 JWT。

### 1.2 登录 `POST /api/auth/login`（公开）

请求体：`{ "username": "zhangsan", "password": "123456" }`

响应 `data`：同 LoginVO。

限流：按客户端 IP 滑动窗口 **30 次/小时**（超限返回 42900，论文 4.7）。

失败：
- 密码错误 → `{code: 40100, message: "用户名或密码错误"}`（累计失败 5 次后 15 分钟内返回 `40101`，论文 4.7 / 测试 F-02）

### 1.3 登出 `POST /api/auth/logout`（登录）

无需请求体，携带当前 token 即可。效果：该 token 的 jti 进入 Redis 黑名单直至自然过期，再访问任何需登录接口返回 40100。

响应 `data`：`null`

### 1.4 当前用户信息 `GET /api/auth/info`（登录）

响应 `data`（LoginVO）：

```json
{
  "token": null,
  "userId": 1,
  "username": "张三丰",
  "email": "zhangsan@example.com",
  "role": 0,
  "avatar": "/uploads/avatars/3-1763500000000.webp",
  "signature": "每天进步一点点。",
  "createdAt": "2026-11-15 08:00:00"
}
```

`avatar` 为站点内相对路径（前端直接用于 `<img src>`，dev 由 Vite 代理、生产由 Nginx 反代 `/uploads`）；未设置头像时为 `null`，前端回退到首字母占位头像。`signature` 未设置时为 `null`。

### 1.5 邮箱修改 `PUT /api/auth/info`（登录）

请求体：`{ "email": "new@example.com" }`（@Valid 校验邮箱格式）。响应 `data: null`。

> 个人信息维护（论文表 3-1）：用户名与角色不可变更，仅邮箱可改。

### 1.5a 签名修改 `PUT /api/auth/profile`（登录，v1.8 新增）

请求体：`{ "signature": "每天进步一点点。" }`。响应 `data: null`。

| 校验 | 说明 |
|---|---|
| signature | trim 后 1-200 字，超长或全空返回 40000 |

### 1.5b 头像上传 `PUT /api/auth/avatar`（登录，v1.8 新增）

请求体：`{ "dataUrl": "data:image/webp;base64,UklGRh..." }`（前端裁剪压缩后的 data URL）。

响应 `data: { "avatar": "/uploads/avatars/3-1763500000000.webp" }`。

| 校验 | 说明 |
|---|---|
| 格式 | 仅 `data:image/png`、`image/jpeg`、`image/webp`，且按魔数二次校验（防止改后缀绕过） |
| 大小 | 解码后 ≤ 300 KB |
| 存储 | 服务端生成文件名 `{userId}-{时间戳}.{ext}` 落盘到 `oj.upload.avatar-dir`，旧头像文件删除 |
| 安全 | 客户端文件名永不参与落盘路径（防路径穿越）；目录由服务端创建 |

头像静态读取：`GET /uploads/avatars/{file}` **无需登录**（公开可读），由 oj-server 静态映射到磁盘目录；部署时该目录需要持久卷（见 docker-compose 的 `data/server-uploads` 挂载）。

### 1.6 密码修改 `PUT /api/auth/password`（登录）

请求体：`{ "oldPassword": "…", "newPassword": "…" }`

| 校验 | 说明 |
|---|---|
| oldPassword | 与库中 BCrypt 哈希比对，不符返回 40000"原密码错误" |
| newPassword | 6-32 位 |

**安全语义**：修改成功后当前令牌立即加入 Redis 黑名单失效（论文 4.7），前端应跳转重新登录。

---

## 2. 题目模块 `/api/problems`

### 2.1 分页列表 `GET /api/problems`（公开）

Query 参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| keyword | ❌ | 标题模糊搜索 |
| difficulty | ❌ | 1=简单 2=中等 3=困难 |
| tag | ❌ | 标签精确筛选（`JSON_CONTAINS` 参数化） |
| page / size | ❌ | 默认 1 / 10（size 上限 100） |

响应 `data.records[]`（未登录游客仅见已上架题目）：

```json
{
  "id": 1,
  "title": "A+B Problem",
  "tags": ["模拟"],
  "difficulty": 1,
  "timeLimitMs": 1000,
  "memoryLimitMb": 256,
  "submitCount": 12,
  "acceptedCount": 8,
  "enabled": true,
  "createdAt": "2026-11-13 08:00:00",
  "updatedAt": "2026-11-13 08:00:00"
}
```

> **列表接口不返回 `description` / `samples` / `languages`**（v1.3 起后端已按此实现，
> 只查轻量列）。`description` 是 LONGTEXT、`samples` 是 JSON，一页 10 条会把
> 整份题面塞进响应体。
>
> ⚠️ **管理端编辑表单必须调 2.2 详情接口回填**，不能复用列表行数据——否则
> description/samples/languages 为空，保存即静默清空题面。

### 2.1.1 题库概览 `GET /api/problems/overview`（公开）

响应 `data`：`{ "totalProblems": 50, "tags": ["二分查找", "动态规划", "图论"] }`。
仅汇总所有上架题目，标签去重排序；不随列表翻页或关键词/标签筛选变化，不泄露下架题目的元信息。题库横幅的全站提交数取排行榜 `summary.totalSubmit`。

### 2.2 题目详情 `GET /api/problems/{id}`（公开）

响应 `data`：

```json
{
  "id": 1,
  "title": "A+B Problem",
  "description": "## 题目描述\n\n输入两个整数 a 和 b…",
  "samples": [ { "input": "1 2", "output": "3" } ],
  "difficulty": 1,
  "timeLimitMs": 1000,
  "memoryLimitMb": 256,
  "languages": ["c", "cpp", "java", "python3", "js"],
  "submitCount": 12,
  "acceptedCount": 8,
  "enabled": true
}
```

> **安全约定（论文 4.7）**：详情接口只返回题面样例（samples），**绝不返回判题测试用例**。

### 2.3 创建题目 `POST /api/problems`（管理员）

请求体（ProblemFormDTO）：

```json
{
  "title": "两数之和",
  "description": "## 题目描述…（Markdown）",
  "samples": [ { "input": "…", "output": "…" } ],
  "difficulty": 2,
  "timeLimitMs": 1000,
  "memoryLimitMb": 256,
  "languages": ["c", "cpp", "java", "python3", "js"],
  "enabled": true
}
```

响应 `data`：新题目 id（Long）。

### 2.4 更新题目 `PUT /api/problems/{id}`（管理员）

请求体同 2.3。响应 `data: null`。

### 2.5 删除题目 `DELETE /api/problems/{id}`（管理员）

响应 `data: null`。

若题目已有提交历史，或被课程/竞赛引用，返回 `40000`，请使用编辑接口下架。仅无引用题目可删除，避免破坏历史记录与排名。

---

## 3. 测试用例模块 `/api/problems/{problemId}/testcases`（管理员）

### 3.1 批量导入 `POST /api/problems/{problemId}/testcases`

请求体（TestCaseImportDTO）：

```json
{
  "cases": [
    { "input": "1 2",     "expectedOutput": "3",  "score": 1, "isSample": true  },
    { "input": "10 20",   "expectedOutput": "30", "score": 1, "isSample": false }
  ]
}
```

响应 `data`：成功导入的用例数量（Integer）。

`cases` 必填且为 1–200 个非 null 用例，分值须非负；输入、期望输出允许空字符串。导入或查询不存在题目的用例返回 `40400`。

### 3.2 用例列表 `GET /api/problems/{problemId}/testcases`

响应 `data[]`：

```json
[
  { "id": 1, "input": "1 2", "expectedOutput": "3", "score": 1, "isSample": true }
]
```

### 3.3 删除用例 `DELETE /api/problems/{problemId}/testcases/{caseId}`

响应 `data: null`。

---

## 4. 提交模块 `/api/submissions`

### 4.1 提交代码 `POST /api/submissions`（登录）

请求体（SubmitDTO）：

```json
{
  "problemId": 1,
  "language": "cpp",
  "code": "#include <iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<a+b;}"
}
```

| 字段 | 校验 |
|---|---|
| language | 必须为 `c` / `cpp` / `java` / `python3` 且在该题 languages 内 |
| code | 非空，UTF-8 ≤ 65536 字节（64KB，论文 4.4） |

链路语义（论文 5.3）：限流（6 次/分/用户）→ 幂等锁（同人同题 2s）→ 校验 → 事务写库 `status=0` → 事务后发 MQ 消息 → **立即返回排队中**。

响应 `data`（SubmitVO）：

```json
{ "submissionId": 100, "status": 0 }
```

> `status` 是**数字状态码**（0=排队中），与 [CONTRACT.md §2 状态机](./CONTRACT.md#2-判题状态机论文表-4-8) 一致；
> 不是枚举名字符串（早期实现曾输出 `"QUEUING"`，前后端口径不一致，v1.3 修正）。

失败：`42901` 提交过于频繁 / `40002` 题目不可用 / `40003` 语言或代码不合法。

### 4.2 判题详情（用例级）`GET /api/submissions/{id}`（登录）⚠️ 实现中

权限：**仅本人与管理员**（他人访问返回 40300，论文 4.7 / 测试 F-13）。

响应 `data`（SubmissionDetailVO）：

```json
{
  "id": 100,
  "userId": 1,
  "problemId": 1,
  "problemTitle": "A+B Problem",
  "language": "cpp",
  "code": "#include …",
  "status": 2,
  "statusDesc": "通过",
  "timeUsedMs": 34,
  "memoryUsedKb": 3200,
  "errorInfo": null,
  "judgedAt": "2026-11-13 08:05:10",
  "createdAt": "2026-11-13 08:05:08",
  "cases": [
    { "caseIndex": 0, "status": 2, "statusDesc": "通过", "timeUsedMs": 34, "memoryUsedKb": 3200 },
    { "caseIndex": 1, "status": 2, "statusDesc": "通过", "timeUsedMs": 30, "memoryUsedKb": 3196 }
  ]
}
```

> `code` 字段仅本人/管理员响应中返回；`status` 取值见 [CONTRACT.md 状态机](./CONTRACT.md#2-判题状态机论文表-4-8)。

### 4.3 提交记录分页 `GET /api/submissions`（登录）

Query 参数：`problemId`、`status`（0-8）、`begin`/`end`（ISO 时间范围，按提交时间筛选，论文 3.3"按题目/状态/时间筛选"）、`page`、`size`。

响应 `data.records[]`（SubmissionListItemVO，不含 code）：

```json
{
  "id": 100, "problemId": 1, "problemTitle": "A+B Problem",
  "language": "cpp", "status": 2, "statusDesc": "通过",
  "timeUsedMs": 34, "memoryUsedKb": 3200,
  "createdAt": "2026-11-13 08:05:08"
}
```

### 4.4 个人通过率统计 `GET /api/submissions/statistics`（登录）

响应 `data`（StatisticsVO，按难度分组，论文 5.6）：

```json
{
  "byDifficulty": [
    { "difficulty": 1, "difficultyLabel": "简单", "submit": 8, "accepted": 6, "acceptRate": 75.0, "solvedCount": 3, "availableSolvedCount": 3, "totalCount": 10 },
    { "difficulty": 2, "difficultyLabel": "中等", "submit": 3, "accepted": 1, "acceptRate": 33.3, "solvedCount": 1, "availableSolvedCount": 1, "totalCount": 5 },
    { "difficulty": 3, "difficultyLabel": "困难", "submit": 0, "accepted": 0, "acceptRate": 0, "solvedCount": 0, "availableSolvedCount": 0, "totalCount": 2 }
  ],
  "byStatus": [
    { "status": 2, "statusDesc": "通过", "count": 7, "percent": 63.6 },
    { "status": 3, "statusDesc": "答案错误", "count": 4, "percent": 36.4 }
  ],
  "totalSubmit": 11,
  "totalAccepted": 7,
  "acceptRate": 63.6,
  "solvedCount": 4,
  "availableSolvedCount": 4,
  "attemptedCount": 6
}
```

`byDifficulty` 始终包含简单、中等、困难三行，包括尚未提交的难度。`totalCount` 统计该难度全部上架题目；新增的 `availableSolvedCount` 只计已 AC 且仍在架的不同题目，用作题库覆盖率分子。原 `solvedCount`、提交数和通过数保留含下架题目的历史口径，重复 AC 同一道题不会增加解题数。

---

## 5. 排行榜 `/api/leaderboard`

### 5.1 排行榜分页 `GET /api/leaderboard`（公开）

Query 参数：`page`（默认 1，最小 1）、`size`（默认 20，限制在 1–100）。

数据源：Redis ZSet `oj:leaderboard:ac`（score=去重 AC 题数）；同分按首次达到该解题数的时间、用户 ID 排序。概览取全站数据，不随翻页变化。

响应 `data`（LeaderboardVO）：

```json
{
  "total": 25,
  "items": [
    { "userId": 3, "username": "lisi", "avatar": null, "acCount": 12, "rank": 1, "totalSubmit": 30, "acceptedSubmit": 20, "acceptRate": 66.7, "lastSubmitAt": "2026-11-17 15:00:00" }
  ],
  "summary": { "solverCount": 25, "maxAcCount": 12, "totalSubmit": 200, "overallAcceptRate": 60.0 }
}
```

> 已删除用户的历史记录仍可能只显示编号；接口不再提供 nickname 字段。

---

## 6. 管理端 `/api/admin`（管理员）

### 6.1 判题队列状态 `GET /api/admin/queue`

响应 `data`（QueueStatusVO，论文表 3-3 运维监控）：

```json
{ "backlog": 5, "judgedToday": 128, "judgeInstances": 2, "queriedAt": "2026-11-13 08:10:00" }
```

`backlog` 为 RabbitMQ `oj.judge.queue` 积压量（Redis 缓存 30s，命中缓存时返回的是上一次探询结果）；`judgeInstances` 为在线判题实例数——oj-judge 每 20s 向 Redis 写心跳键（TTL 30s，掉线自动消失），`--scale oj-judge=4` 扩容后自动可见；`queriedAt` 为本次查询时刻（§0.1 格式），与 `backlog` 的缓存时间解耦，避免误读为"积压量的采集时间"。

### 6.2 课程实验 `/api/admin/courses`（管理员）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/courses` | 创建课程 `{name, description, problemIds[]}`，返回 id |
| PUT | `/api/admin/courses/{id}` | 更新课程（全量替换题目分组） |
| DELETE | `/api/admin/courses/{id}` | 删除课程 |
| GET | `/api/admin/courses` | 课程列表 |
| GET | `/api/admin/courses/{id}/problems` | 课程内题目（按 sortOrder） |
| GET | `/api/admin/courses/{id}/completion` | 班级完成情况（每题提交人数/通过人数） |

课程与竞赛的 `problemIds` 允许空题组，非空时必须是互不重复、存在的正整数题目编号，否则返回 `40000`。无效编辑不会替换原题组；不存在课程的题组查询返回 `40400`。

---

## 7. WebSocket 推送协议（论文 4.5 / 5.5）

### 7.1 连接

| 项 | 值 |
|---|---|
| 端点 | `/ws`（SockJS 升级，STOMP 子协议） |
| 客户端库 | `@stomp/stompjs` + `sockjs-client` |
| 心跳/重连 | 客户端 `reconnectDelay: 3000` 自动重连 |
| **鉴权** | **STOMP `CONNECT` 帧原生头 `Authorization: Bearer <token>`**，服务端在入站通道拦截器校验并绑定会话 Principal |

> **鉴权为什么在 STOMP 层而不是握手层**：SockJS 的 `/ws/info`、`/ws/xhr` 是普通 HTTP 请求，
> 浏览器握手阶段无法携带自定义头，因此 `SecurityConfig` 对 `/ws/**` 保持 `permitAll`；
> 真正的身份校验发生在 STOMP `CONNECT` 帧——解析 `Authorization` 后 `setUser(LoginUser)`。
> v1.4 安全补充：未通过签名、黑名单或数据库用户状态校验的 CONNECT 会被明确拒绝；通过后绑定 `LoginUser`，角色使用数据库当前值。只允许订阅 `/user/queue/judge`，客户端不能 SEND 判题事件。

```ts
// 前端连接方式（oj-web/src/api/ws.ts）
new Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: { Authorization: `Bearer ${token}` },  // ← 必需
  reconnectDelay: 3000,
})
```

> ⚠️ **契约要点**：`convertAndSendToUser(String.valueOf(userId), ...)` 是按
> `Principal#getName()` 匹配会话的，服务端 `LoginUser.getName()` 必须返回 **userId 字符串**
> （不是 username）。改这里会让所有推送静默丢失（没有报错，只有 `log.debug`）。

### 7.2 订阅目标

```
/user/queue/judge     ← 用户级定向推送（服务端 convertAndSendToUser）
```

### 7.3 消息结构

统一信封 `{ type, data }`，三种消息类型按判题状态机阶段推送：

**① JUDGE_QUEUED（入队）**

```json
{ "type": "JUDGE_QUEUED", "data": { "submissionId": 100 } }
```

**② JUDGING（判题中）**

```json
{ "type": "JUDGING", "data": { "submissionId": 100 } }
```

**③ JUDGE_FINISHED（判题完成）**（data = JudgePushEvent）

```json
{
  "type": "JUDGE_FINISHED",
  "data": {
    "submissionId": 100,
    "userId": 1,
    "problemId": 1,
    "status": 3,
    "timeUsedMs": 28,
    "memoryUsedKb": 3100,
    "passedCases": 1,
    "totalCases": 4,
    "errorInfo": null
  }
}
```

### 7.4 前端处理约定（论文 5.7）

1. 状态机 `IDLE → SUBMITTING → QUEUED → JUDGING → FINISHED`，WS 消息与 HTTP 响应**任一先到即更新**，另一通道到达时幂等忽略；
2. **兜底**：连接断开重连后，或 2s 内未收到推送时，按 `submissionId` 轮询 `GET /api/submissions/{id}`（2s 间隔），`status >= 2` 即终止轮询；
3. 收到 `JUDGE_FINISHED` 后拉取 4.2 详情渲染用例级表格。

---

## 8. 前后端联调清单

| # | 场景 | 依赖接口/消息 | 验收标准 |
|---|---|---|---|
| L1 | 注册→登录→登出 | 1.1~1.3 | 登出后旧 token 访问 1.4 返回 40100 |
| L2 | 游客浏览题目 | 2.1/2.2 | 种子题「A+B Problem」可见，通过率进度条正确 |
| L3 | 提交→推送→详情 | 4.1 + WS③ + 4.2 | 无轮询收到 JUDGE_FINISHED，详情表格用例级展示 |
| L4 | 2 秒重复提交 | 4.1 | 第二次返回 42901 |
| L5 | 他人代码隔离 | 4.2 | 非本人访问返回 40300 |
| L6 | 排行榜 | 5.1 | AC 后排名实时更新（测试 F-14） |
| L7 | 队列监控 | 6.1 | backlog 与 RabbitMQ 控制台一致 |

---

## 8.1 竞赛模块 `/api/contests`（论文 7.2 展望落地）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/contests` | 公开 | 竞赛列表（附状态与题数） |
| GET | `/api/contests/{id}` | 公开 | 详情（字母题号题目集 A/B/C…） |
| GET | `/api/contests/{id}/ranking` | 公开 | ACM 简化排名（解题数降序→罚时升序；罚时=首次AC分钟+错误×20） |
| POST/PUT/DELETE | `/api/admin/contests[/{id}]` | 管理员 | 创建/编辑/删除（时间窗 + 题目集） |

排名明细字段 `detail`：每题 `A ✅10min`（✅=AC，分钟=距开赛；`-2`=该题错误 2 次计罚时）。

## 8.2 用户管理 `/api/admin/users`（管理员）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/users?keyword=&page=&size=` | 分页列表（不含密码哈希） |
| PUT | `/api/admin/users/{id}/role?role=0|1` | 提权/降权 |
| PUT | `/api/admin/users/{id}/status?status=0|1` | 封禁/解封——**封禁后既有 token 立即失效（40100），登录返回 40300** |
| PUT | `/api/admin/users/{id}/password` | 重置密码，返回临时密码 |

## 8.3 公告模块

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/announcements` | 公开 | 公告列表（倒序），题目列表页轮播展示 |
| POST | `/api/admin/announcements` | 管理员 | 发布 `{title, content}` |
| DELETE | `/api/admin/announcements/{id}` | 管理员 | 删除 |

## 8.4 题目标签与收藏

- **标签**：`problem.tags` JSON 数组；创建/编辑题目时可多选或自建；列表筛选 `GET /api/problems?tag=模拟`（`JSON_CONTAINS` 参数化，防注入）。
- **收藏**：
  - `POST /api/problems/{id}/favorite`：toggle，返回当前是否收藏；
  - `GET /api/problems/{id}/favorite`：当前状态（详情页按钮初始态）；
  - `GET /api/favorites`：我的收藏列表（个人中心）。

## 9. 变更管理

- 接口契约以本文档为准；**任何字段/路径变更必须先改本文档再改代码**（双方评审）；
- 每次变更在下方登记：

| 日期 | 版本 | 变更内容 | 提出人 |
|---|---|---|---|
| 2026-11-13 | v1.0 | 骨架阶段定稿 | — |
| 2026-11-15 | v1.1 | 个人信息维护接口（1.5/1.6）、提交记录时间筛选（4.3）、注册/登录 IP 限流、昵称字段移除、样例编辑器与 XSS 净化 | 成员 B |
| 2026-11-15 | v1.2 | 竞赛模式（8.1）、用户管理（8.2）、公告（8.3）、题目标签与收藏（8.4） | 成员 B |
| 2026-11-16 | v1.3 | **审查修复批次**：① 4.1 `SubmitVO.status` 明确为数字码（原输出枚举名）；② 2.1 列表接口明确不返回 description/samples/languages（后端已按此瘦身），管理端编辑须走 2.2；③ 7.1 WS 鉴权改为 STOMP CONNECT 帧 `Authorization` 头（原「无独立 token 校验」不可投递，属文档与实现双缺失）；④ 收藏接口未登录返回 40100；⑤ 全局时间序列化统一 `yyyy-MM-dd HH:mm:ss`（`spring.jackson.date-format` 对 `LocalDateTime` 无效，需注册序列化器）；⑥ 补登记 `GET /api/submissions/trend` | 成员 B |
| 2026-11-16 | v1.4 | 保留 v1.3 接口及 LoginUser Principal；补充 STOMP 黑名单/数据库状态鉴权、订阅白名单与 SEND 限制。提交事务及时间字段保持 B 契约，内部租约/补发采用 UTC | A 评测修复，待 B 评审 |
| 2026-11-17 | v1.5 | 补齐当前统计/排行榜响应；课程/竞赛题组与用例导入校验；有历史或引用的题目禁止物理删除；无效分页处理 | 项目完整性改善，待评审 |
| 2026-11-17 | v1.6 | 统计固定返回三个难度，新增在架解题数；新增题库概览与全站标签；题库横幅和筛选翻页口径修正，原响应字段保留 | 题库完善，待评审 |
