package org.chovy.canvas.common;

/**
 * Map 字段键常量集合。
 *
 * <p>用途：
 * 1) 消除散落魔法字符串，避免拼写错误；
 * 2) 统一跨模块（controller / engine / handler）的字段命名；
 * 3) 支持 IDE 全局引用追踪与批量重构。
 *
 * <p>约定：
 * 1) 尽量按"业务语义"命名常量，而不是按使用方命名；
 * 2) 同义字段保留兼容键（如 camel/snake）时，使用注释明确；
 * 3) 新增键优先放到对应分组，避免无序增长。
 */
public final class MapFieldKeys {
    // ==================== Common Response / Envelope Keys ====================

    /** 单请求ID。 */
    public static final String REQUEST_ID = "requestId";

    /** 批量请求ID列表。 */
    public static final String REQUEST_IDS = "requestIds";

    /** 通用状态字段。 */
    public static final String STATUS = "status";

    /** 是否立即分发。 */
    public static final String IMMEDIATE_DISPATCH = "immediateDispatch";

    /** 数量。 */
    public static final String COUNT = "count";

    /** 限制值。 */
    public static final String LIMIT = "limit";

    /** 分发失败数量。 */
    public static final String DISPATCH_FAILURE_COUNT = "dispatchFailureCount";

    /** 分发失败的请求ID列表。 */
    public static final String DISPATCH_FAILED_REQUEST_IDS = "dispatchFailedRequestIds";

    /** 溢出标记。 */
    public static final String OVERFLOW = "overflow";

    /** 执行实例ID。 */
    public static final String EXECUTION_ID = "executionId";

    /** 错误信息字段。 */
    public static final String ERROR = "error";

    /** 激活标记。 */
    public static final String ACTIVE = "active";

    /** 去重标记。 */
    public static final String DEDUPLICATED = "deduplicated";

    /** 跳过标记。 */
    public static final String SKIPPED = "skipped";

    /** 事件日志ID。 */
    public static final String EVENT_LOG_ID = "eventLogId";

    /** 事件编码。 */
    public static final String EVENT_CODE = "eventCode";

    /** 用户ID。 */
    public static final String USER_ID = "userId";

    /** 触发画布标记。 */
    public static final String CANVAS_TRIGGERED = "canvasTriggered";

    /** 恢复等待数量。 */
    public static final String WAITS_RESUMED = "waitsResumed";

    /** 等待状态。 */
    public static final String WAIT_STATUS = "waitStatus";

    /** 转移后的旅程ID。 */
    public static final String TRANSFERRED_JOURNEY_ID = "transferredJourneyId";

    /** 分流路径标识。 */
    public static final String SPLIT_PATH = "splitPath";

    /** MQ发送结果标记。 */
    public static final String MQ_SENT = "mqSent";

    /** 标签值。 */
    public static final String TAG_VALUE = "tagValue";

    /** 模板展开标记。 */
    public static final String TEMPLATE_EXPANDED = "templateExpanded";

    /** 事件匹配标记。 */
    public static final String EVENT_MATCHED = "eventMatched";

    /** 推荐结果列表。 */
    public static final String RECOMMENDATIONS = "recommendations";

    /** 提交动作类型。 */
    public static final String ACTION_TYPE = "actionType";

    /** 积分台账ID。 */
    public static final String POINTS_LEDGER_ID = "pointsLedgerId";

    /** 重复标记。 */
    public static final String DUPLICATE = "duplicate";

    /** 策略放行标记。 */
    public static final String POLICY_ALLOWED = "policyAllowed";

    /** 分组通过标记。 */
    public static final String GROUP_PASSED = "groupPassed";

    /** AB分组标识。 */
    public static final String AB_GROUP = "abGroup";

    /** 汇聚完成标记。 */
    public static final String MERGED = "merged";

    /** 人群命中标记。 */
    public static final String AUDIENCE_MATCHED = "audienceMatched";

    /** 频控放行标记。 */
    public static final String FREQUENCY_ALLOWED = "frequencyAllowed";

    /** 任务ID。 */
    public static final String TASK_ID = "taskId";

    /** API触发标记。 */
    public static final String API_TRIGGERED = "apiTriggered";

    /** 标签变更标记。 */
    public static final String TAGS_CHANGED = "tagsChanged";

    /** 跳转计数。 */
    public static final String JUMP_COUNT = "jumpCount";

    /** 跳转超限标记。 */
    public static final String JUMP_EXCEEDED = "jumpExceeded";

    /** 渠道可用标记。 */
    public static final String CHANNEL_AVAILABLE = "channelAvailable";

    /** 静默时段命中标记。 */
    public static final String QUIET_HOURS_ACTIVE = "quietHoursActive";

    /** 画像更新标记。 */
    public static final String PROFILE_UPDATED = "profileUpdated";

    /** 目标达成标记。 */
    public static final String GOAL_MET = "goalMet";

    /** 评分值。 */
    public static final String SCORE = "score";

    /** 评分分段。 */
    public static final String SCORE_BAND = "scoreBand";

    /** 循环迭代次数。 */
    public static final String LOOP_ITERATIONS = "loopIterations";

    /** 循环退出标记。 */
    public static final String LOOP_EXITED = "loopExited";

    /** 循环超限标记。 */
    public static final String LOOP_EXCEEDED = "loopExceeded";

    /** 上下文对象键。 */
    public static final String CTX = "ctx";

    /** 图结构键。 */
    public static final String GRAPH = "graph";

    /** 触发节点ID。 */
    public static final String TRIGGER_NODE_ID = "triggerNodeId";

    /** 是否恢复执行。 */
    public static final String IS_RESUME = "isResume";

    /** 画布对象键。 */
    public static final String CANVAS = "canvas";

    /** 准入上限。 */
    public static final String ADMISSION_LIMIT = "admissionLimit";

    /** 执行 lane。 */
    public static final String EXECUTION_LANE = "executionLane";

    /** 准入拒绝原因。 */
    public static final String ADMISSION_REASON = "admissionReason";

    /** lane 活跃执行数。 */
    public static final String LANE_ACTIVE = "laneActive";

    /** 全局活跃执行数。 */
    public static final String GLOBAL_ACTIVE = "globalActive";

    /** 去重键。 */
    public static final String DEDUP_KEY = "dedupKey";

    /** 挂起标记。 */
    public static final String PENDING = "pending";

    /** 节点ID。 */
    public static final String NODE_ID = "nodeId";

    /** 节点类型。 */
    public static final String NODE_TYPE = "nodeType";

    /** 执行结果。 */
    public static final String OUTCOME = "outcome";

    /** 恢复时间戳(毫秒)。 */
    public static final String RESUME_AT_EPOCH_MS = "resumeAtEpochMs";

    /** 原因码。 */
    public static final String REASON_CODE = "reasonCode";

    /** 原因描述。 */
    public static final String REASON_MESSAGE = "reasonMessage";

    /** 源节点ID。 */
    public static final String SOURCE_NODE_ID = "sourceNodeId";

    /** 超时分支节点ID。 */
    public static final String TIMEOUT_NODE_ID = "timeoutNodeId";

    /** 特殊节点超时 timer key。 */
    public static final String TIMEOUT_TIMER_KEY = "__timeoutTimerKey";

    /** 特殊节点超时调度时间戳。 */
    public static final String TIMEOUT_SCHEDULED_AT_EPOCH_MS = "__timeoutScheduledAtEpochMs";

    /** 特殊节点超时触发时间戳。 */
    public static final String TIMEOUT_FIRE_AT_EPOCH_MS = "__timeoutFireAtEpochMs";

    /** 特殊节点等待超时秒数。 */
    public static final String TIMEOUT_SECONDS = "__timeoutSeconds";

    /** 等待订阅ID。 */
    public static final String WAIT_SUBSCRIPTION_ID = "waitSubscriptionId";

    /** 等待类型(camel)。 */
    public static final String WAIT_TYPE = "waitType";

    /** 事件ID。 */
    public static final String EVENT_ID = "eventId";

    /** 事件属性。 */
    public static final String EVENT_ATTRIBUTES = "eventAttributes";

    /** 目标恢复状态(内部键)。 */
    public static final String GOAL_RESUME_STATUS = "__goalResumeStatus";

    /** 等待恢复状态(内部键)。 */
    public static final String WAIT_RESUME_STATUS = "__waitResumeStatus";

    /** 目标达成分支节点ID。 */
    public static final String GOAL_MET_NODE_ID = "goalMetNodeId";

    /** 目标未达成分支节点ID。 */
    public static final String GOAL_NOT_MET_NODE_ID = "goalNotMetNodeId";

    /** 审批结果(内部键)。 */
    public static final String APPROVAL_RESULT = "__approvalResult";

    /** 通用类型字段。 */
    public static final String TYPE = "type";

    /** 通用名称字段。 */
    public static final String NAME = "name";

    /** 版本1快照键。 */
    public static final String V1 = "v1";

    /** 版本2快照键。 */
    public static final String V2 = "v2";

    /** 版本ID。 */
    public static final String VERSION_ID = "versionId";

    /** 版本号。 */
    public static final String VERSION = "version";

    /** 新增项集合。 */
    public static final String ADDED = "added";

    /** 删除项集合。 */
    public static final String REMOVED = "removed";

    /** 修改项集合。 */
    public static final String MODIFIED = "modified";

    /** 摘要。 */
    public static final String SUMMARY = "summary";

    /** 新增数量。 */
    public static final String ADDED_COUNT = "addedCount";

    /** 删除数量。 */
    public static final String REMOVED_COUNT = "removedCount";

    /** 修改数量。 */
    public static final String MODIFIED_COUNT = "modifiedCount";

    /** 不变项集合。 */
    public static final String UNCHANGED = "unchanged";

    /** 标签编码集合。 */
    public static final String TAG_CODES = "tagCodes";

    /** API定义键。 */
    public static final String API_KEY = "apiKey";

    /** 参数集合。 */
    public static final String PARAMS = "params";

    /** else分支(内部键)。 */
    public static final String ELSE = "__else";

    /** 来源执行ID。 */
    public static final String SOURCE_EXECUTION_ID = "sourceExecutionId";

    /** 渠道标识。 */
    public static final String CHANNEL = "channel";

    /** 模板ID。 */
    public static final String TEMPLATE_ID = "templateId";

    /** 内容字段。 */
    public static final String CONTENT = "content";

    /** 变量映射。 */
    public static final String VARIABLES = "variables";

    /** 幂等键。 */
    public static final String IDEMPOTENCY_KEY = "idempotencyKey";

    /** 消息ID。 */
    public static final String MESSAGE_ID = "messageId";

    /** 通用ID字段。 */
    public static final String ID = "id";

    /** 用户画像(snake)。 */
    public static final String USER_PROFILE = "user_profile";

    /** 回调参数(snake)。 */
    public static final String CALLBACK_PARAMS = "callback_params";

    /** 流程信息(snake)。 */
    public static final String PROCESS_INFO = "process_info";

    /** 目标类型(snake)。 */
    public static final String TARGET_TYPE = "target_type";

    /** 目标ID(snake)。 */
    public static final String TARGET_ID = "target_id";

    /** 客户ID(snake)。 */
    public static final String CUSTOMER_ID = "customer_id";

    /** webhook ID(snake)。 */
    public static final String WEBHOOK_ID = "webhook_id";

    /** 发送时间(snake)。 */
    public static final String SEND_TIME = "send_time";

    /** 实例ID。 */
    public static final String INSTANCE_ID = "instanceId";

    /** 批次ID。 */
    public static final String BATCH_ID = "batchId";

    /** 动作ID。 */
    public static final String ACTION_ID = "actionId";

    /** 客户ID(camel)。 */
    public static final String CUSTOMER_ID_CAMEL = "customerId";

    /** 流程实例ID。 */
    public static final String PROCESS_INSTANCE_ID = "processInstanceId";

    /** 流程实例开始时间。 */
    public static final String PROCESS_INSTANCE_START_TIME = "processInstanceStartTime";

    /** 流程节点实例ID。 */
    public static final String PROCESS_NODE_INSTANCE_ID = "processNodeInstanceId";

    /** 流程节点实例开始时间。 */
    public static final String PROCESS_NODE_INSTANCE_START_TIME = "processNodeInstanceStartTime";

    /** 分组名称。 */
    public static final String GROUP_NAME = "groupName";

    /** 输入参数映射。 */
    public static final String INPUT_PARAMS = "inputParams";

    /** 券类型键。 */
    public static final String COUPON_TYPE_KEY = "couponTypeKey";

    /** 触发类型。 */
    public static final String TRIGGER_TYPE = "triggerType";

    /** 创建时间。 */
    public static final String CREATED_AT = "createdAt";

    /** 通用值字段。 */
    public static final String VALUE = "value";

    /** 标签文案。 */
    public static final String LABEL = "label";

    /** 请求Schema。 */
    public static final String REQUEST_SCHEMA = "requestSchema";

    /** 是否附带上下文载荷。 */
    public static final String INCLUDE_CONTEXT_PAYLOAD = "includeContextPayload";

    /** 节点名称。 */
    public static final String NODE_NAME = "nodeName";

    /** 错误消息。 */
    public static final String ERROR_MSG = "errorMsg";

    /** 输出数据。 */
    public static final String OUTPUT_DATA = "outputData";

    /** 耗时(毫秒)。 */
    public static final String DURATION_MS = "durationMs";

    /** 总量。 */
    public static final String TOTAL = "total";

    /** 成功量/成功标记。 */
    public static final String SUCCESS = "success";

    /** 失败量/失败标记。 */
    public static final String FAILED = "failed";

    /** 暂停量/暂停标记。 */
    public static final String PAUSED = "paused";

    /** 成功率。 */
    public static final String SUCCESS_RATE = "successRate";

    /** 去重用户数。 */
    public static final String UNIQUE_USERS = "uniqueUsers";

    /** 日期字段。 */
    public static final String DATE = "date";

    // ==================== DAG Node Config / Routing Keys ====================

    /** 默认下一节点ID。 */
    public static final String NEXT_NODE_ID = "nextNodeId";

    /** 成功分支节点ID。 */
    public static final String SUCCESS_NODE_ID = "successNodeId";

    /** 失败分支节点ID。 */
    public static final String FAIL_NODE_ID = "failNodeId";

    /** else分支节点ID。 */
    public static final String ELSE_NODE_ID = "elseNodeId";

    /** 审批通过分支节点ID。 */
    public static final String APPROVE_NODE_ID = "approveNodeId";

    /** 审批拒绝分支节点ID。 */
    public static final String REJECT_NODE_ID = "rejectNodeId";

    /** 抑制分支节点ID。 */
    public static final String SUPPRESSED_NODE_ID = "suppressedNodeId";

    /** 放行分支节点ID。 */
    public static final String ALLOWED_NODE_ID = "allowedNodeId";

    /** 静默分支节点ID。 */
    public static final String QUIET_NODE_ID = "quietNodeId";

    /** 可达分支节点ID。 */
    public static final String AVAILABLE_NODE_ID = "availableNodeId";

    /** 不可达分支节点ID。 */
    public static final String UNAVAILABLE_NODE_ID = "unavailableNodeId";

    /** 通过分支节点ID。 */
    public static final String PASS_NODE_ID = "passNodeId";

    /** 频控触发分支节点ID。 */
    public static final String CAPPED_NODE_ID = "cappedNodeId";

    /** 跳过分支节点ID。 */
    public static final String SKIPPED_NODE_ID = "skippedNodeId";

    /** 超上限分支节点ID。 */
    public static final String MAX_EXCEEDED_NODE_ID = "maxExceededNodeId";

    /** 分支列表。 */
    public static final String BRANCHES = "branches";

    /** 优先级配置列表。 */
    public static final String PRIORITIES = "priorities";

    /** 组配置列表。 */
    public static final String GROUPS = "groups";

    /** 路径配置列表。 */
    public static final String PATHS = "paths";

    /** 变体配置列表。 */
    public static final String VARIANTS = "variants";

    /** 分段配置列表。 */
    public static final String BANDS = "bands";

    /** 条件列表。 */
    public static final String CONDITIONS = "conditions";

    /** 条件关系(AND/OR)。 */
    public static final String STRATEGY_RELATION = "strategyRelation";

    /** 分配策略。 */
    public static final String ALLOCATION_STRATEGY = "allocationStrategy";

    /** 实验键。 */
    public static final String EXPERIMENT_KEY = "experimentKey";

    /** 实验名称。 */
    public static final String EXPERIMENT_NAME = "experimentName";

    /** 变体ID。 */
    public static final String VARIANT_ID = "variantId";

    /** 路径ID。 */
    public static final String PATH_ID = "pathId";

    /** 是否对照组。 */
    public static final String IS_CONTROL = "isControl";

    /** 随机策略值。 */
    public static final String RANDOM = "RANDOM";

    /** 一致性策略值。 */
    public static final String CONSISTENT = "CONSISTENT";

    /** 与关系。 */
    public static final String AND = "AND";

    /** 或关系。 */
    public static final String OR = "OR";

    /** 等待类型(snake)。 */
    public static final String WAIT_TYPE_SNAKE = "wait_type";

    /** 超时状态值。 */
    public static final String TIMEOUT = "TIMEOUT";

    /** 过期状态值。 */
    public static final String EXPIRED = "EXPIRED";

    /** 完成状态值。 */
    public static final String COMPLETED = "COMPLETED";

    /** 时长配置。 */
    public static final String DURATION = "duration";

    /** 时长数值。 */
    public static final String DURATION_VALUE = "durationValue";

    /** 时长单位。 */
    public static final String DURATION_UNIT = "durationUnit";

    /** 单位字段。 */
    public static final String UNIT = "unit";

    /** 时间窗口配置。 */
    public static final String TIME_WINDOW = "timeWindow";

    /** 窗口开始时间。 */
    public static final String WINDOW_START = "windowStart";

    /** 窗口结束时间。 */
    public static final String WINDOW_END = "windowEnd";

    /** 开始时间。 */
    public static final String START = "start";

    /** 结束时间。 */
    public static final String END = "end";

    /** 截止日期时间。 */
    public static final String UNTIL_DATE = "untilDate";

    /** 时间字段。 */
    public static final String TIME = "time";

    /** 最大等待时长。 */
    public static final String MAX_WAIT = "maxWait";

    /** 事件过滤条件。 */
    public static final String EVENT_FILTERS = "eventFilters";

    /** 人群ID。 */
    public static final String AUDIENCE_ID = "audienceId";

    /** 人群命中标记。 */
    public static final String AUDIENCE_HIT = "audienceHit";

    /** 命中分支节点ID。 */
    public static final String HIT_NEXT_NODE_ID = "hitNextNodeId";

    /** 未命中分支节点ID。 */
    public static final String MISS_NEXT_NODE_ID = "missNextNodeId";

    /** 模式字段。 */
    public static final String MODE = "mode";

    /** 离线模式值。 */
    public static final String OFFLINE = "offline";

    /** 人群模式值。 */
    public static final String AUDIENCE = "audience";

    /** 实时模式值。 */
    public static final String REALTIME = "realtime";

    /** 是否校验开关。 */
    public static final String VALIDATE_RESULT = "validateResult";

    /** 校验规则列表。 */
    public static final String VALIDATE_RULES = "validateRules";

    /** 字段名。 */
    public static final String FIELD = "field";

    /** 消息编码键。 */
    public static final String MESSAGE_CODE_KEY = "messageCodeKey";

    /** Topic键(兼容旧配置)。 */
    public static final String TOPIC_KEY = "topicKey";

    /** 输出前缀。 */
    public static final String OUTPUT_PREFIX = "outputPrefix";

    /** HTTP状态码字段。 */
    public static final String HTTP_STATUS = "httpStatus";

    /** 是否JSON响应。 */
    public static final String IS_JSON = "isJson";

    /** 响应体字段。 */
    public static final String BODY = "body";

    // ==================== Value Resolution / Internal Meta Keys ====================
    // `VALUE` 与 `VALUE_KEY` 都是 "value"：前者用于通用字段，后者用于表达式/解析语义。

    /** 值来源类型。 */
    public static final String VALUE_TYPE = "valueType";

    /** 上下文来源枚举值。 */
    public static final String CONTEXT = "CONTEXT";

    /** 解析语义中的值键。 */
    public static final String VALUE_KEY = "value";

    /** 上游节点ID列表(内部键)。 */
    public static final String UPSTREAM_IDS = "__upstreamIds";

    /** 当前节点ID(内部注入键)。 */
    public static final String NODE_ID_INTERNAL = "__nodeId";

    /** 定时触发批处理执行标记。 */
    public static final String SCHEDULED_BATCH = "__scheduledBatch";

    /** 定时批处理对应的触发节点 ID。 */
    public static final String SCHEDULED_TRIGGER_NODE_ID = "__scheduledTriggerNodeId";

    /** 成功计数。 */
    public static final String SUCCESS_COUNT = "successCount";

    /** 完成计数。 */
    public static final String DONE_COUNT = "doneCount";

    /** 失败计数。 */
    public static final String FAIL_COUNT = "failCount";

    /** 总计数。 */
    public static final String TOTAL_COUNT = "totalCount";

    /** 通过标记。 */
    public static final String PASSED = "passed";

    /**
     * 构造 MapFieldKeys 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private MapFieldKeys() {
    }
}
