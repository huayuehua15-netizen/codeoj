# CodeOJ 接口文档（前后端对接契约）

> 版本：v1.0（骨架阶段定稿）｜ 与 `oj-server` Controller 代码严格一致
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
  "createdAt": "2026-11-15 08:00:00"
}
```

### 1.5 邮箱修改 `PUT /api/auth/info`（登录）

请求体：`{ "email": "new@example.com" }`（@Valid 校验邮箱格式）。响应 `data: null`。

> 个人信息维护（论文表 3-1）：用户名与角色不可变更，仅邮箱可改。

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
| page / size | ❌ | 默认 1 / 10 |

响应 `data.records[]`（未登录游客仅见已上架题目）：

```json
{
  "id": 1,
  "title": "A+B Problem",
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

> 列表接口不返回 description/samples/languages 大字段。

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
  "languages": ["c", "cpp", "java", "python3"],
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
  "languages": ["c", "cpp", "java", "python3"],
  "enabled": true
}
```

响应 `data`：新题目 id（Long）。

### 2.4 更新题目 `PUT /api/problems/{id}`（管理员）

请求体同 2.3。响应 `data: null`。

### 2.5 删除题目 `DELETE /api/problems/{id}`（管理员）

响应 `data: null`。

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

### 4.4 个人通过率统计 `GET /api/submissions/statistics`（登录）⚠️ 实现中

响应 `data`（StatisticsVO，按难度分组，论文 5.6）：

```json
{
  "byDifficulty": [
    { "difficulty": 1, "difficultyLabel": "简单", "submit": 8, "accepted": 6 },
    { "difficulty": 2, "difficultyLabel": "中等", "submit": 3, "accepted": 1 }
  ],
  "totalSubmit": 11,
  "totalAccepted": 7
}
```

---

## 5. 排行榜 `/api/leaderboard`

### 5.1 排行榜分页 `GET /api/leaderboard`（公开）

Query 参数：`page`（默认 1）、`size`（默认 20）。

数据源：Redis ZSet `oj:leaderboard:ac`（score=AC 题数）倒序（论文 5.6）。

响应 `data`（LeaderboardVO）：

```json
{
  "total": 25,
  "items": [
    { "userId": 3, "username": "lisi", "nickname": "李四", "acCount": 12, "rank": 1 },
    { "userId": 1, "username": "zhangsan", "nickname": "张三", "acCount": 7, "rank": 2 }
  ]
}
```

> 骨架阶段 nickname/username 可能暂为 null（用户表补齐逻辑第二阶段），前端需容错显示 `用户 {userId}`。

---

## 6. 管理端 `/api/admin`（管理员）

### 6.1 判题队列状态 `GET /api/admin/queue`

响应 `data`（QueueStatusVO，论文表 3-3 运维监控）：

```json
{ "backlog": 5, "judgedToday": 128, "judgeInstances": 2, "cachedAt": "2026-11-13T08:10:00Z" }
```

`backlog` 为 RabbitMQ `oj.judge.queue` 积压量（Redis 缓存 30s）；`judgeInstances` 为在线判题实例数——oj-judge 每 20s 向 Redis 写心跳键（TTL 30s，掉线自动消失），`--scale oj-judge=4` 扩容后自动可见。

### 6.2 课程实验 `/api/admin/courses`（管理员）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/courses` | 创建课程 `{name, description, problemIds[]}`，返回 id |
| PUT | `/api/admin/courses/{id}` | 更新课程（全量替换题目分组） |
| DELETE | `/api/admin/courses/{id}` | 删除课程 |
| GET | `/api/admin/courses` | 课程列表 |
| GET | `/api/admin/courses/{id}/problems` | 课程内题目（按 sortOrder） |
| GET | `/api/admin/courses/{id}/completion` ⚠️ | 班级完成情况（每题提交人数/通过人数） |

---

## 7. WebSocket 推送协议（论文 4.5 / 5.5）

### 7.1 连接

| 项 | 值 |
|---|---|
| 端点 | `/ws`（SockJS 升级，STOMP 子协议） |
| 客户端库 | `@stomp/stompjs` + `sockjs-client` |
| 心跳/重连 | 客户端 `reconnectDelay: 3000` 自动重连 |
| 鉴权 | SockJS 握手经 Nginx 转发；推送按 userId 定向，无独立 token 校验（STOMP 层） |

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

## 9. 变更管理

- 接口契约以本文档为准；**任何字段/路径变更必须先改本文档再改代码**（双方评审）；
- 每次变更在下方登记：

| 日期 | 版本 | 变更内容 | 提出人 |
|---|---|---|---|
| 2026-11-13 | v1.0 | 骨架阶段定稿 | — |
| 2026-11-15 | v1.1 | 个人信息维护接口（1.5/1.6）、提交记录时间筛选（4.3）、注册/登录 IP 限流、昵称字段移除、样例编辑器与 XSS 净化 | 成员 B |
