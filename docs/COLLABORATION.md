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

### 3.0 一句话理解（新手先看这段）

> **`main` 和 `dev` 是"公共的"，谁都不要直接往上推。**
> 每个人都先在**自己的分支**上干活，干完了再合到 `dev`。等全部做完、验收通过了，才把 `dev` 合到 `main`。

为什么要这样？就为一个目的：**`dev` 任何时候拉下来都能跑起来**。
如果两个人都直接往 `dev` 推，一旦有人推了半成品，另一个人（和你答辩那天）就拉到一个跑不起来的版本。

### 3.1 分支结构

```
main                  ← 发布分支：答辩快照，只在验收通过后前进
└── dev               ← 集成分支：两人成果汇合处，任何时候都必须能跑
    ├── feat/judge-engine   ← 成员 A 的个人分支（判题链路）
    └── feat/web-pages      ← 成员 B 的个人分支（业务与前端）
```

下次再做新功能，就再开一个：`feat/新功能名`、`fix/要修的问题`。

### 3.2 日常操作流程（照着敲就行）

#### ① 第一次：创建自己的分支

```bash
# 先切到 dev，并确保是最新的
git checkout dev
git pull origin dev

# 创建自己的分支（名字按 §3.1）
git checkout -b feat/web-pages     # 成员 B
# git checkout -b feat/judge-engine  # 成员 A

# 推到远程，并建立跟踪关系（-u 只需第一次加）
git push -u origin feat/web-pages
```

> **GitHub Desktop 用户**：Branch → New Branch → Name 填 `feat/web-pages`，
> 「Create branch based on...」**选 `dev`**（不是 main）→ Create branch → 再点 **Publish branch**。

#### ② 每天动手前：先同步别人的改动

```bash
git checkout dev
git pull origin dev            # 拿到对方已合进 dev 的更新

git checkout feat/web-pages    # 回到自己分支
git merge dev                  # 把更新并进来（越早合并越不容易冲突）
```

> 养成习惯：**每天开工前同步一次**。这样冲突都是小冲突，拖久了会变成大冲突。

#### ③ 干活、提交、推送

```bash
git add .
git commit -m "feat(judge): 沙箱帧流解析与 16MB 截断"
git push                       # 已经设过 -u 就不用再写 origin/分支名
```

提交信息格式：`<type>(<scope>): <一句话>`

| type | 用在什么时候 |
|---|---|
| `feat` | 新增功能 |
| `fix` | 修 bug |
| `docs` | 只改文档 |
| `style` | 只改样式/格式（不影响功能） |
| `refactor` | 重构（不改行为） |
| `chore` | 杂务（改配置、删临时文件） |

例子：`fix(server): 收藏接口匿名访问返回 50000 改为 40100`

#### ④ 做完一个功能：合进 dev

```bash
git checkout dev
git pull origin dev                    # 先确保 dev 最新
git merge feat/web-pages               # 合并自己的分支
# 跑一遍：mvn clean install -DskipTests  +  cd oj-web && npm run build
git push origin dev                    # 验证通过再推
```

> **合并前必须先跑通构建**。`dev` 挂了，两个人都没法干活。

#### ⑤ 验收通过后：dev → main（由成员 B 执行）

```bash
git checkout main
git pull origin main
git merge dev
git push origin main
```

### 3.3 冲突了怎么办（新手最容易慌）

`git merge` 报 `CONFLICT` 不代表代码坏了，是**同一行被两个人改了**，Git 要你选一个。

1. 打开冲突文件，会看到这样的标记：

   ```java
   <<<<<<< HEAD
   我这一版的代码
   =======
   对方那一版的代码
   >>>>>>> dev
   ```

2. **手工把文件改成最终想要的样子**，把 `<<<<<<<`、`=======`、`>>>>>>>` 三行全部删掉
3. 保存后：

   ```bash
   git add 冲突的那个文件
   git commit          # 不用写 -m，Git 会自带合并信息
   ```

**彻底乱了想放弃重来**（还没 commit 时）：

```bash
git merge --abort      # 回到合并前的状态，什么都不丢
```

> 记住这句：**没 commit 之前，什么都救得回来。** 慌的时候先 `git merge --abort`。

### 3.4 几条硬规矩（违反会出事）

| # | 规则 | 违反的后果 |
|---|---|---|
| 1 | **不直接 push `main`** | 答辩快照被半成品污染 |
| 2 | **不直接 push `dev`**（只通过合并进） | 别人拉下来跑不起来 |
| 3 | **合并进 dev 前必须构建通过** | dev 挂掉，两人停工 |
| 4 | 改 `oj-common`（实体/枚举/常量）= 改契约 | **必须双方同意** + 更新 CONTRACT.md |
| 5 | 改 `API.md` / `CONTRACT.md` 涉及的接口 | 先改文档 → 知会对方 → 再改代码 |
| 6 | 动别人负责的目录（见 §1） | **先打招呼**，把 diff 发给对方过一眼 |

> 第 6 条不是形式主义：对方 pull 之后会看到莫名其妙的改动，排查半天。
> 一句话说清「我改了哪个文件、为什么、有没有碰逻辑」，就能省掉对方半小时。

### 3.5 谁负责什么（避免互相踩）

| 目录 | 负责人 | 别人能动吗 |
|---|---|---|
| `oj-server/`、`oj-web/` | 成员 B | 要动先说 |
| `oj-judge/`、`judge-images/` | 成员 A | 要动先说 |
| `deploy/` | 成员 A（判题相关）/ 共用 | 要动先说 |
| `oj-common/` | **公共区** | **必须双方同意** |
| `docs/`、`sql/` | 共用 | 改完知会 |

### 3.6 常用命令速查

| 我想…… | 命令 |
|---|---|
| 看我现在在哪个分支 | `git branch --show-current` |
| 看有哪些分支 | `git branch -a` |
| 看改了什么文件 | `git status` |
| 看具体改了什么内容 | `git diff` |
| 切分支 | `git checkout 分支名` |
| 建新分支并切过去 | `git checkout -b 新分支名` |
| 拉别人的更新 | `git pull origin dev` |
| 把某分支合进来 | `git merge 分支名` |
| 放弃合并 | `git merge --abort` |
| 撤销某个还没提交的改动 | `git checkout -- 文件名` |
| 看提交历史 | `git log --oneline -10` |

### 3.7 三条救命提示

1. **推错了分支**（比如推到了 main）—— 别慌，先告诉对方，一起处理。**不要自己 force push。**
2. **不确定现在什么状态** —— 运行 `git status` 和 `git branch --show-current`，把输出发给对方看。
3. **任何操作前不确定** —— 先 `git status`，确认干净再动手。看不懂就先停，问一句比事后回滚便宜。

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
