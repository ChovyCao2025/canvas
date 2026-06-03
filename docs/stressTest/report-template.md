# 容量报告模板

## 测试身份

- `perfRunId`：
- 场景：
- 资源规格：
- 后端镜像：
- 后端容器 CPU/内存：
- 执行人：
- 日期时间：

## 证据

- Runner summary 文件：
- Verifier 文件：
- Side-effect verifier 文件：
- Guide report 输出：
- 监控目录：
- 清理记录：

## 正确性

- Runner `sent`：
- Runner `success`：
- Runner `failed`：
- Runner `durationMs`：
- Verifier verdict：
- Unexpected loss：
- Duplicate execution：
- Retry pending：
- DLQ：
- Trace verdict：
- Trace mismatch：
- Trace failed：
- Trace duplicate success：
- Trace buffer pending：
- Side-effect verdict：
- Side-effect total mismatch：
- Side-effect branch mismatch：
- Duplicate side effects：
- Missing side-effect input ID：

如果 verifier verdict 不是 `PASS`，在这里停止，不得发布容量数字。
如果 side-effect verdict 不是 `PASS`，在这里停止，不得发布容量数字。

## 性能

- 请求总数：
- 并发数：
- 成功发送数：
- 失败发送数：
- 持续时间：
- QPS：
- p95：
- p99 来源：

## 瓶颈

- 应用 CPU：
- JVM/GC：
- MySQL：
- Redis：
- RocketMQ：
- 下游服务：

## 容量估算输入

- localStableQps：
- localAppCores：
- prodAppCoresTotal：
- writesPerEvent：
- prodDbSafeWriteQps：
- redisOpsPerEvent：
- prodRedisSafeOps：
- rocketmqCapacity：
- disruptorWorkerCapacity：
- downstreamRateLimitPerSec：
- downstreamCallsPerEvent：
- safetyFactor：

## 结论

- 推荐容量：
- 告警阈值：
- 限流阈值：
- 主要瓶颈：
- 证据缺口：
