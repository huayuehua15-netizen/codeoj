# 2026-11-17 项目完整性改善验证

基线 ecdc149 加本地修复。本目录记录本次独立测试结果，不代表完整压力与恶意样例验收。

- management.json：16 项真实管理接口检查，测试资源已清理。
- history-ranking.json：排行榜第二页概览、超大分页与历史删除保护；排行榜使用明确标注的合成读侧数据。
- five-languages.json：五语言经真实 API/Docker，每条两个用例 AC。
- websocket.json：三阶段推送、跨用户隔离与鉴权。
- verification.json：70 项 Java、19 项 Python、新依赖构建和当前依赖审计汇总。
- npm-audit.json：当前锁文件已知漏洞数量为 0；不是全部安全问题的保证。

完整日志在本次交付 outputs/项目完善验证。管理脚本位于 judge-tests/smoke-management.py，需独立测试管理员令牌与 --isolated-test-environment；真实凭据不归档。Windows 构建入口已将子进程临时目录设到仓库 .tmp，Linux 构建保持原环境。
