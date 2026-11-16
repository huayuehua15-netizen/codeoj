# 最新 dev 兼容复核（2026-11-16）

基线 dev `72a9c2e82eb5610b810f2c739b206be57e23d9ef`，含 main `058b7c7`。相对初次修复重新合并 B 的最新业务代码，当前尚未提交/推送。

- Java 61 项测试通过，server 17 / judge 44。
- Linux Node22 的 TypeScript/Vite 构建通过，保留 B 新页面与拆包。
- five-languages.json：五语言真实 API/Docker，每条两用例 AC。
- websocket.json：LoginUser Principal、三阶段定向实收、用户隔离与拒绝无 token/内部队列。
- timezone.json：读取生产共享 SQL，在真实 MySQL +08 会话执行 11 项临时表检查，UTC 派发/租约与上海业务时间同时正确，A/B 五索引保留。
- business-api.json：8 项 B 业务契约检查，含新统计/排行榜字段、轻量题目列表、详情、时间、今日趋势、竞赛排名和匿名收藏。
- migration.json：原旧 schema 执行 B 索引及 A 租约迁移，业务记录保留，UTC 默认补丁可重复执行。
- coauthor-files.json：上游变动的 58 文件，47 个与 dev blob 一致，11 个重叠文件逐项合并（含 API 兼容登记）。

全部测试使用独立临时容器和数据库，不包含有效 JWT 或凭据。协作与升级约束见 docs/JUDGE-COMPATIBILITY.md。完整 Maven/前端日志在本次交付的 outputs/兼容验证，JSON 同步在此归档。
