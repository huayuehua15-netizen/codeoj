# 2026-11-16 评测链路修复验证

基线为 `d9f0e2bd86e9fb7c36bcf6f62bb50765030b9ec9` 加本地修复，分支 `feat/judge-hardening`。本目录只保存脱敏测试结果，不包含 JWT、密钥或数据库凭据。

| 文件 | 验证内容 |
| --- | --- |
| five-languages.json | C/C++/Java/Python3/JavaScript 经真实 API，每条两用例 AC |
| short-programs.json | 修正 Docker attach 竞态后，12 条短 C 程序全部 AC |
| statuses.json | 8 组判定边界及 DB/Redis 去重排行榜 |
| sandbox-boundaries.json | 非 root、有效权限为 0、只读运行目录、可信统计目录不可访问 |
| recovery.json | 丢首发补发、过期恢复、第二实例不抢健康租约、错误 token 拒写和明细幂等 |
| websocket.json | 真实 SockJS/STOMP 三阶段收包、其他用户零匹配、无令牌及内部队列拒绝 |
| migration.json | 对原本地旧 schema 执行一次迁移，保留原记录 |
| environment.json | 测试运行环境及镜像标识 |

Java：`mvn -B -o package`，55 项通过；Python：`python3 -m unittest discover -s judge-tests/tests -v`，10 项通过。前端在 Linux Node 22 使用 `npm ci`、`npm run build` 成功，Windows 原生 esbuild 临时文件访问异常不作为前端代码错误。

所有数据库和服务均为此次新建的隔离测试容器，MQ 使用独立账号；评测工作目录使用 Linux 原生 Docker 卷。正确性样本与有界资源测试规模较小，不代表完整恶意样例测试或 100/300/500 并发性能验收。恢复证据在消费者/SQL修复完成后取得；后续仅调整沙箱日志读取，五语言、短程序、判定和 WS 证据在该调整后重新取得。

GNU time 的短时测量有约 10ms 粒度，正常极短 C 程序 `timeUsedMs=0` 合法；内存与用例明细仍应存在。环境清理后这些提交编号仅用于归档复核，不对应用户业务数据库。
