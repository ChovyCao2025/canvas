# Open Source Growth Success Metrics

日期：2026-06-08

## 1. 北极星指标

半年专项的北极星指标：

```text
Time to First Successful Journey
```

定义：

新用户从打开仓库到成功运行一个示例营销旅程 dry-run 并看到 trace 的时间。

目标：

- Month 1：小于 15 分钟。
- Month 3：小于 8 分钟。
- Month 6：小于 5 分钟。

## 2. 产品体验指标

| 指标 | Month 1 | Month 3 | Month 6 |
|---|---:|---:|---:|
| 一键 demo 成功率 | 70% | 85% | 90% |
| 默认模板数量 | 3 | 10 | 10+ |
| 可 dry-run 模板数量 | 3 | 6 | 10 |
| 官方插件数量 | 0-2 | 6 | 6+ |
| CLI 命令数量 | 0 | 0-2 | 5 |
| AI mock 能力 | 无 | 风险审计草稿 | 生成、审计、解释 |

## 3. 开发者体验指标

| 指标 | 目标 |
|---|---:|
| 新插件从脚手架到本地测试通过 | 30 分钟内 |
| 新模板从 YAML 到导入成功 | 15 分钟内 |
| CLI validate 错误可读性 | 错误包含字段路径和原因 |
| 插件文档覆盖率 | 每个官方插件 1 篇文档 |
| 模板文档覆盖率 | 每个官方模板 1 段业务说明 + 示例 payload |

## 4. 社区健康指标

公开发布后跟踪：

- GitHub stars。
- GitHub forks。
- Issues 数量和首次响应时间。
- PR 数量和合并周期。
- Discord/微信群/飞书群活跃度。
- 模板和插件贡献数量。
- README 快速启动反馈。

建议目标：

| 时间 | 目标 |
|---|---:|
| 发布后 1 个月 | 100+ stars，5+ issues，1+ 外部 PR |
| 发布后 3 个月 | 500+ stars，20+ issues，5+ 外部 PR |
| 发布后 6 个月 | 1500+ stars，50+ issues，10+ 外部 PR |

这些不是工程验收硬门槛，而是增长健康度观察指标。

## 5. 质量门禁

每个公开 milestone 必须满足：

```bash
cd backend && mvn test
cd frontend && npm run test -- --run
cd frontend && npm run build
docker compose -f docker-compose.demo.yml config
```

如果 CLI 已引入：

```bash
cd tools/canvas-cli && npm test
```

手工验证：

- README 快速启动命令可执行。
- demo 登录可用。
- 模板导入可用。
- dry-run 可用。
- trace 可见。
- 插件禁用后依赖模板会阻止导入。

## 6. 对外发布检查表

- [ ] README 首屏清楚。
- [ ] 截图/GIF 已补。
- [ ] quickstart 命令已在干净环境验证。
- [ ] demo 环境不依赖真实外部服务。
- [ ] 3 个核心模板可 dry-run。
- [ ] LICENSE 已明确。
- [ ] CONTRIBUTING 已明确测试命令。
- [ ] SECURITY 不暴露私人联系方式或敏感信息。
- [ ] issue/PR 模板可用。
- [ ] 英文文档没有明显过期命令。
- [ ] 对比竞品内容客观，不攻击竞品。

## 7. 风险指标

以下信号说明专项偏离目标：

- README 越写越长，首屏仍看不出项目价值。
- demo 需要配置真实短信、邮件、审批或 AI key。
- 插件体系开始引入运行时 classloader 热加载。
- DSL 首版试图覆盖所有历史节点。
- AI 功能没有 mock，导致普通用户无法体验。
- 模板只有技术 JSON，没有业务说明和示例 payload。
- 贡献者需要理解整个后端才能写一个新节点。

出现以上任一问题，应优先收敛范围，而不是继续叠加功能。

