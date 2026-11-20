# CodeOJ 云服务器部署文档（阿里云 ECS · 2 核 2G · 新手向）

> 适用场景：把本仓库部署到一台**远程的阿里云 ECS**（2 核 2G 内存，Linux 系统），
> 部署完成后浏览器访问 `http://服务器公网IP` 即可使用。
> 全程只需要：你自己的 Windows 电脑 + ECS 的公网 IP + root 密码（或密钥）。
>
> **部署顺序总览**（每一步在后面都有详细说明）：
>
> 1. 本地确认代码已推送（`niuzj` 分支已推送，无需再做）
> 2. ECS 安全组放行端口（80 / 22）
> 3. SSH 登录服务器
> 4. 服务器装 Docker + 加 4G swap（2G 内存**必做**）
> 5. 克隆代码到服务器
> 6. 配置 `deploy/.env`
> 7. （可选）把本地演示数据导入服务器
> 8. 构建并启动业务服务
> 9. 构建判题镜像（**不建判题全部提交都是系统错误**）
> 10. 验收清单
>
> 预计总耗时：60~90 分钟（大部分时间在等构建，人不用守着）。

---

## 0. 部署后的架构长什么样（先建立全局认识）

| 容器 | 作用 | 对外端口 |
|---|---|---|
| oj-web | Nginx：托管前端页面，并把 `/api`、`/ws` 转发给后端 | **80**（唯一对外入口） |
| oj-server | 业务后端（Spring Boot，8080） | 不直接对外 |
| oj-judge | 判题服务（消费队列、调 Docker 沙箱跑用户代码） | 不直接对外 |
| oj-mysql | MySQL 8 数据库 | 不对外 |
| oj-redis | Redis 7 缓存/排行榜 | 不对外 |
| oj-rabbitmq | RabbitMQ 消息队列 | 15672 仅管理台，**安全组不要放行** |
| （动态沙箱容器） | 判题时临时启动的一次性容器，跑完即删 | 不对外 |

数据库、缓存、队列都只在 Docker 内部网络里互通，**公网一个都不通**，这是安全的。
你需要做的只是在阿里云安全组里放行 80（网站）和 22（SSH 登录）。

**2 核 2G 的现实约束**（文档后面所有步骤都据此调整）：
- 内存很紧：6 个常驻容器 + 判题时再起的沙箱容器，必须**加 4G swap**，判题并发压到 1；
- 首次构建慢：Maven/NPM 构建都在服务器上进行，2G 机器首次约 15~30 分钟，属于正常；
- 判题镜像约 4~6GB 磁盘：系统盘默认 40G 够用，低于这个数先扩盘。

---

## 1. 第一步（本地）：确认代码已推送

部署分支用 `niuzj`（当前最新工作都在这个分支上，且已推送到 GitHub）。
在你自己的电脑上确认一次：

```bash
cd C:/Users/ddntq/Desktop/code-king
git status          # 应显示 nothing to commit, working tree clean
git log origin/niuzj..niuzj --oneline   # 应该没有输出，说明全部已推送
```

如果以后本地有新改动，推完再按 §11 的更新流程操作即可。

---

## 2. 第二步（阿里云控制台）：安全组放行端口

1. 登录阿里云控制台 → 云服务器 ECS → 实例 → 点你的实例进入详情；
2. 左侧「安全组」→ 点安全组 ID → 「手动添加」入方向规则，加两条：

| 协议 | 端口范围 | 授权对象 | 用途 |
|---|---|---|---|
| TCP | 80 | 0.0.0.0/0 | 网站访问 |
| TCP | 22 | 0.0.0.0/0（或写你自己的 IP 更安全） | SSH 登录 |

> **不要放行** 3306 / 6379 / 5672 / 15672 / 8080 —— 这些都不该暴露公网。

---

## 3. 第三步（本地）：SSH 登录服务器

Windows 自带 OpenSSH，直接在 PowerShell 或 Git Bash 里：

```bash
ssh root@服务器公网IP
# 第一次会问是否信任，输 yes；然后输入你买服务器时设置的密码
```

看到类似 `Welcome to Ubuntu ...` / `Welcome to Alibaba Cloud Linux` 的欢迎语就登录成功了。

先看一眼系统是什么（后面装 Docker 的命令按这个选）：

```bash
cat /etc/os-release
```

---

## 4. 第四步（服务器）：装 Docker + 加 swap

### 4.1 加 4G swap（2G 内存必做，先做这个）

swap 是用磁盘顶内存的应急空间。2G 内存跑 6 个容器 + 编译判题，没有 swap 构建期就会被系统杀进程。

```bash
fallocate -l 4G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab   # 开机自动挂载
free -h    # 应看到 Swap 一行 total 为 4.0Gi
```

### 4.2 安装 Docker

**Ubuntu 22.04 / 24.04**（`cat /etc/os-release` 里看到 Ubuntu 就用这个）：

```bash
apt-get update && apt-get install -y ca-certificates curl
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://mirrors.aliyun.com/docker-ce/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
> /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

**Alibaba Cloud Linux 3**（看到 Alibaba Cloud Linux 就用这个）：

```bash
dnf config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

启动并验证：

```bash
systemctl enable --now docker
docker --version        # 应输出 Docker version 27.x 或更高
docker compose version  # 应输出 v2.x
```

### 4.3 配置国内镜像加速（不配的话拉镜像大概率超时）

国内直连 Docker Hub 基本拉不动，配置镜像站后 mysql/redis/编译器镜像都能秒拉：

```bash
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": ["https://docker.m.daocloud.io"]
}
EOF
systemctl restart docker
```

> 提示：公共镜像站偶尔失效。若后面拉镜像仍超时，把 `registry-mirrors` 换成
> 阿里云「容器镜像服务 → 镜像加速器」页面给你的**专属加速地址**（免费，登录阿里云就能看到）。

---

## 5. 第五步（服务器）：克隆代码

```bash
cd /opt
git clone -b niuzj https://github.com/huayuehua15-netizen/codeoj.git codeoj
cd /opt/codeoj && git log --oneline -1   # 应看到 ee225b0 或更新的提交
```

> GitHub 国内偶尔克隆慢或失败：重试一两次通常能过；始终不行的话，在 Gitee 上
> 「从 GitHub 导入仓库」建个镜像库，再 `git clone -b niuzj <gitee地址>`。
> 注意导入库要同步一次才算最新，或以后更新统一走你自己的 GitHub 远端。

---

## 6. 第六步（服务器）：配置 deploy/.env

```bash
cd /opt/codeoj/deploy
cp .env.example .env
openssl rand -base64 16   # 输出用作数据库密码，复制一下
openssl rand -base64 16   # 输出用作 RabbitMQ 密码
openssl rand -base64 36   # 输出用作 JWT 密钥
vim .env
```

改成下面这样（三个值换成你自己生成的，**别用示例里的原文**）：

```ini
MYSQL_ROOT_PASSWORD=替换成上面第一个openssl输出
RABBITMQ_USER=codeoj
RABBITMQ_PASSWORD=替换成上面第二个openssl输出
JWT_SECRET=替换成上面第三个openssl输出
ENGINE_MODE=real
JUDGE_CONCURRENCY=1
MAX_PARALLEL_CONTAINERS=1
```

说明：
- `JWT_SECRET` 必须 ≥32 字节（`openssl rand -base64 36` 有 48 字符，够）；
- **2G 内存把判题并发压到 1**（两个都写 1），并发高了编译器会同时抢内存，容易把机器打满；
- `.env` 已在 `.gitignore` 里，不会被提交，密码就只存在服务器上。

### 6.1 （推荐）限制两个 Java 服务的最大内存

在 `deploy/` 目录下新建一个 `docker-compose.override.yml`（compose 会自动合并它，不动仓库原文件）：

```bash
cd /opt/codeoj/deploy
cat > docker-compose.override.yml <<'EOF'
services:
  oj-server:
    environment:
      JAVA_TOOL_OPTIONS: "-Xms128m -Xmx384m -XX:MaxMetaspaceSize=160m"
  oj-judge:
    environment:
      JAVA_TOOL_OPTIONS: "-Xms96m -Xmx256m -XX:MaxMetaspaceSize=112m"
EOF
```

---

## 7. （可选）把本地演示数据导入服务器

全新部署只有 schema 自带的种子题目。如果你想让服务器上直接有**本机那套答辩演示数据**
（28 道题、40 个用户、1023 条提交、竞赛/公告/课程/判题明细），按这里做；不需要就跳过本节。

**在本地 Windows（Git Bash）导出**：

```bash
cd C:/Users/ddntq/Desktop/code-king
docker exec oj-mysql mysqldump -uroot -p123456 --databases codeoj --single-transaction \
  --default-character-set=utf8mb4 --set-gtid-purged=OFF > codeoj-demo.sql
scp codeoj-demo.sql root@服务器公网IP:/root/
```

**在服务器上**：先只启动 MySQL 并等它健康（这一步会用 schema.sql 初始化空库）：

```bash
cd /opt/codeoj/deploy
docker compose up -d mysql
docker compose ps   # 等 oj-mysql 状态变成 Up (healthy)，约半分钟
```

然后导入（dump 自带 DROP TABLE/CREATE TABLE，会整体覆盖成演示数据）：

```bash
docker exec -i oj-mysql mysql -uroot -p'你的MYSQL_ROOT_PASSWORD' < /root/codeoj-demo.sql
docker exec oj-mysql mysql -uroot -p'你的MYSQL_ROOT_PASSWORD' -e \
  "USE codeoj; SELECT COUNT(*) AS problems FROM problem; SELECT COUNT(*) AS users FROM user;"
# 应输出 problems=28、users=40（或与你本地一致的数字）
```

> ⚠️ 演示数据里所有账号密码都是 `123456`（含教师账号 `teacher_admin`）。
> 若服务器要长期对外使用，部署完在管理端「用户管理 → 重置密码」改掉关键账号。

---

## 8. 第七步（服务器）：构建并启动业务服务

2G 内存不要让 compose 同时构建三个镜像（并行构建会挤爆内存），**逐个构建**：

```bash
cd /opt/codeoj/deploy
docker compose build oj-server   # Maven 构建，2G 上约 5~10 分钟
docker compose build oj-judge
docker compose build oj-web      # npm ci + vite 构建，约 5~10 分钟
docker compose up -d
docker compose ps
```

`docker compose ps` 里 6 个服务应全部 `Up`（mysql/redis/rabbitmq 带 `(healthy)`）。
首次拉 mysql/redis/rabbitmq 镜像加启动约 1~3 分钟。

看日志确认没有报错（Ctrl+C 退出，不影响运行中的服务）：

```bash
docker compose logs --tail=30 oj-server   # 应看到 Started OjServerApplication
docker compose logs --tail=20 oj-judge    # 应看到 Started OjJudgeApplication
```

本机冒烟测试：

```bash
curl -I http://127.0.0.1/                              # HTTP/1.1 200 OK
curl http://127.0.0.1/api/problems | head -c 200       # 应返回 {"code":0,...,"records":[...]}
free -h                                                # 确认内存还有余量
```

---

## 9. 第八步（服务器）：构建判题镜像（关键，跳过=判题全挂）

> 为什么需要：五个语言沙箱镜像（c/cpp/java/python/js）**不随仓库分发**，必须在每台
> 部署机上构建一次。跳过的话平台能打开、能提交、消息能收发，但**所有提交都是
> status=8 系统错误**。详见 `docs/JUDGE-IMAGES.md`。

```bash
cd /opt/codeoj/judge-images
# APT_MIRROR 让镜像内的 apt 走阿里云源，构建提速很多
APT_MIRROR=mirrors.aliyun.com ./build.sh
```

- 构建 5 个镜像约 15~40 分钟（2G 机器偏慢属正常），期间会下载 gcc/openjdk/python/node 基础镜像；
- 脚本构建完会**逐个自检**（真的编译运行一遍五语言探针），最后输出每个镜像的验证结果；
- 基础镜像若拉取失败，改用：
  `BASE_REGISTRY_MIRROR=docker.m.daocloud.io APT_MIRROR=mirrors.aliyun.com ./build.sh`

自检通过后，重启判题服务让它加载沙箱：

```bash
cd /opt/codeoj/deploy
docker compose restart oj-judge
```

---

## 10. 第九步：验收清单

浏览器打开 `http://服务器公网IP`，逐项过：

1. **页面**：首页/题库/排行榜/公告正常打开，题库里有题目数据；
2. **注册登录**：注册一个新账号（记下用户名），登录成功；
3. **管理员提权**（本机执行）：

   ```bash
   docker exec oj-mysql mysql -uroot -p'你的MYSQL_ROOT_PASSWORD' -e \
     "USE codeoj; UPDATE user SET role=1 WHERE username='刚注册的用户名';"
   ```

   刷新页面，导航出现「管理」，能进管理台；

4. **真实判题**（核心验收）：进入「A+B Problem」，用 C++ 提交 `cin>>a>>b; cout<<a+b;`：
   - 页面不刷新的情况下状态从 排队中 → 判题中 → **通过**（WebSocket 推送）；
   - 管理端「队列监控」应显示 **在线实例 1**、累计完成 ≥1；
   - 如果结果是「系统错误(status=8)」→ 判题镜像没建好，回到 §9；
5. **判题耗时合理**：单用例 10~50ms 量级，整题 1~3 秒内返回；
6. **内存余量**：`free -h` 的 `available` 应保持 ≥200MB。

全部通过即部署完成 ✅

---

## 11. 以后有改动怎么更新（你说的“后面再推”）

本地改完代码后：

```bash
# 本地
cd C:/Users/ddntq/Desktop/code-king
git add -A && git commit -m "[xxx] 你的提交说明" && git push origin niuzj

# 服务器
cd /opt/codeoj
git pull
cd deploy
docker compose build oj-server   # 改了后端才需要
docker compose build oj-web      # 改了前端才需要
docker compose up -d             # 没变的容器不会重启
```

注意：
- 判题镜像（`judge-images/`）没改就不用重建，改了才重新跑 §9 的 `./build.sh`；
- 如果那次更新带了数据库变更，先看该变更对应的 `sql/migrations/*.sql` 说明再执行
  （新部署的库已含全部最新结构，只有旧库升级才需要迁移）；
- 本地开发环境用的是 Docker Desktop + `npm run dev`，**不影响服务器**。

---

## 12. 日常运维速查

```bash
# 看状态 / 日志
docker compose ps
docker compose logs --tail=50 -f oj-server     # Ctrl+C 退出
docker stats                                    # 实时各容器 CPU/内存，Ctrl+C 退出

# 重启单个服务
docker compose restart oj-server oj-judge

# 整组重启（服务器重启后 docker 会自动拉起容器，一般不需要手动）
cd /opt/codeoj/deploy && docker compose up -d

# 数据库备份（建议每周跑一次，可挂 crontab）
docker exec oj-mysql mysqldump -uroot -p'你的MYSQL_ROOT_PASSWORD' --databases codeoj \
  --single-transaction --default-character-set=utf8mb4 \
  > /root/codeoj-backup-$(date +%F).sql

# RabbitMQ 管理台（15672 不开公网，用 SSH 隧道本地看）
# 本地 Windows 执行：ssh -L 15672:127.0.0.1:15672 root@服务器IP
# 然后浏览器开 http://localhost:15672（账号 .env 里的 RABBITMQ_USER/PASSWORD）

# 磁盘不够时清理构建缓存
docker system prune -f && docker builder prune -f
```

---

## 13. 常见问题（FAQ）

| 现象 | 原因与处理 |
|---|---|
| 拉镜像超时 / `i/o timeout` | §4.3 的镜像加速没配好。换阿里云专属加速地址后 `systemctl restart docker` |
| 所有提交都是「系统错误」status=8 | 判题镜像没构建或 tag 不对。`cd judge-images && ./build.sh --verify-only` 自检，通过后 `docker compose restart oj-judge` |
| 队列监控显示「无在线实例」 | oj-judge 没起来。`docker compose logs oj-judge` 看报错；常见原因是磁盘满或 docker.sock 权限 |
| 页面 502 | oj-server 没起来或内存被杀。`docker compose ps` + `docker compose logs oj-server`；`free -h` 确认 swap 生效 |
| 构建时卡住后失败 / `Killed` | 2G 内存构建被杀。确认 §4.1 的 4G swap 已挂载（`free -h`）；还是不行就逐个 `docker compose build`，一个一个来 |
| `git clone` GitHub 超时 | 重试；或走 Gitee 导入镜像库克隆（见 §5 提示） |
| 数据库中文乱码 | 导入 dump 时漏了 `--default-character-set=utf8mb4`，重新按 §7 导入 |
| 判题报 `No such image` | 镜像 tag 与代码不一致。确认用的是仓库自带的 `judge-images/build.sh` 构建，不要手写 `docker build` |
| 忘记某个用户密码 | 管理端「用户管理 → 重置密码」会返回临时密码 |
| 想看实时内存压力 | `docker stats` + `free -h`；available 长期 <100MB 时说明并发或数据量超了 2G 的承载，优先降 JUDGE_CONCURRENCY |

---

## 14. 安全红线（部署完自查一遍）

- [ ] 安全组只放行了 80 和 22，3306/6379/5672/15672/8080 全部未放行；
- [ ] `.env` 里的数据库/MQ 密码不是默认值，JWT_SECRET 是本次新生成的 ≥32 字节随机串；
- [ ] 服务器上的代码目录不包含 `deploy/.env` 的备份副本（如 `.env.bak`）；
- [ ] 若导入了演示数据：`teacher_admin` 等关键账号密码已改掉 `123456`；
- [ ] `deploy/docker-compose.override.yml`（若创建）里的内容只是内存调优，不含密码。
