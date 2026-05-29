# 画布触发
## 1. 关键地址
- RocketMQ Dashboard：http://localhost:8081/
- 画布界面: http://localhost:3000/   User: admin  Password: Admin@123
## 2. 事件上报触发
```bash
bash scripts/report-order-complete.sh 
```
## 3. 直调触发
```bash
bash scripts/direct-call.sh {CANVAS_ID}
```
其中 CANVAS_ID 为画布 ID，默认值为 11。

## 4. 画布失效触发
```bash
curl -X POST http://localhost:8080/ops/cache/invalidate/{CANVAS_ID}
```
其中 CANVAS_ID 为画布 ID，默认值为 11。

## 5. Groovy 表达式提示
现在的 GROOVY 节点本质上执行一段受限 Groovy 脚本，最后只要 return Map，Map 里的字段会写入上下文，后续 API_CALL / IF / SEND_MQ 都能用 ${字段名} 引用。

当前可用绑定变量：
- input：来自节点 inputParams 的输入 Map
- ctx：执行上下文，可用 ctx.getContextValue("字段")
- userId
- canvasId
- executionId

支持常见类型/能力：Math、字符串、集合、BigDecimal、LocalDate/LocalDateTime、正则、Map/List 处理。禁用方向：Runtime、Process、Thread、ClassLoader、反射等，
不能拿它做系统命令、线程、反射绕过。

#### 例子 1：金额分层 + 风险标记

```groovy
def amount = new BigDecimal(String.valueOf(ctx.getContextValue("amount") ?: "0"))

def level = amount >= 1000 ? "HIGH" : amount >= 300 ? "MEDIUM" : "LOW"
def discount = amount >= 1000 ? 80 : amount >= 300 ? 30 : 10

return [
    amountLevel: level,
    couponAmount: discount,
    riskFlag: amount >= 2000
]
```

后续 API_CALL 可配：
```json
{
    "inputParams": {
        "userId": "${userId}",
        "couponAmount": "${couponAmount}",
        "amountLevel": "${amountLevel}"
    }
}
```


#### 例子 2：字符串和正则提取

```groovy
def orderNo = String.valueOf(ctx.getContextValue("orderId") ?: "")
def matcher = orderNo =~ /ORD-(\d{4})-(\d+)/

def year = matcher.matches() ? matcher[0][1] : "UNKNOWN"
def seq = matcher.matches() ? matcher[0][2] : "0"

return [
    orderYear: year,
    orderSeq: seq,
    normalizedOrderId: orderNo.trim().toUpperCase()
]

```
#### 例子 3：用输入参数计算输出

如果 Groovy 节点配置了 inputParams，比如 name=price、name=count，脚本里可以：

```groovy
def price = new BigDecimal(String.valueOf(input.price ?: "0"))
def count = Integer.parseInt(String.valueOf(input.count ?: "0"))
def total = price.multiply(new BigDecimal(count))

return [
    totalAmount: total,
    largeOrder: total >= 500
]

```
#### 例子 4：时间计算

```groovy
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

def now = LocalDateTime.now()
def hour = now.getHour()

return [
    executeTime: now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
    timeBucket: hour < 12 ? "AM" : "PM",
    nightUser: hour >= 22 || hour < 6
]

```
注意几个边界：

- 必须 return Map，否则输出会是空。
- 输出字段直接进入上下文，后执行节点可用 ${fieldKey}。
- 输出大小默认限制 64KB。
- 默认超时 5000ms。
- 适合做字段加工、规则派生、轻量计算；不适合做外部 HTTP、DB、文件、线程这类副作用操作。


## 6. DIRECT_RETURN 节点使用
DIRECT_RETURN 一般只用于“同步直调画布”的场景，也就是业务系统调用画布后，需要马上拿到一个业务结果。

#### 典型场景：

- 实时决策：业务下单、注册、开卡、授信时调用画布，画布返回 pass/riskLevel/offerCode。
- 同步推荐：业务请求“给这个用户推荐什么权益/内容/商品”，画布调用 API 或规则后返回推荐结果。
- 接口编排：画布里先 API_CALL 查用户、查订单、查风控，再把几个结果组合成统一响应返回给业务方。
- 同步校验：业务系统问“这个用户是否满足条件”，画布返回 eligible=true/false 和原因。
- 子流程式查询：画布作为一个可配置决策服务，不负责发券/发消息，只负责返回决策数据。

#### 它不适合这些场景：

- 事件触发、MQ 触发、定时任务这类异步流程，通常用 END。
- 流程后面还要继续触达、发券、等待审批、延迟执行，因为 DIRECT_RETURN 是终止节点。
- 业务方不关心同步返回，只需要流程后台跑完。

一句话：DIRECT_RETURN 是把画布当“同步决策接口”时的返回节点；如果画布只是后台流程，用 END。

#### 效果触发
```bash
curl -s -X POST "http://localhost:8080/canvas/execute/direct/16" \ 
    -H "Content-Type: application/json" \
    -d '{
      "inputParams": {
        "userId": "user_016"
      },
      "idempotencyKey": "direct-16-user-016-004"
    }'
```

## 7. 消息队列触发
```bash
sh scripts/trigger-canvas-14-mq.sh
```
