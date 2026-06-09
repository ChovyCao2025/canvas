package org.chovy.canvas.domain.approval;

import com.fasterxml.jackson.databind.JsonNode;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HttpLarkApprovalClient 编排 domain.approval 场景的领域业务规则。
 */
@Service
public class HttpLarkApprovalClient implements LarkApprovalClient {

    private final WebClient webClient;
    private final MarketingMonitorProviderCredentialResolver credentialResolver;
    private final String userTokenReference;
    private final String tenantTokenReference;

    /**
     * 创建 HttpLarkApprovalClient 实例并注入 domain.approval 场景依赖。
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param baseUrl base url 参数，用于 HttpLarkApprovalClient 流程中的校验、计算或对象转换。
     * @param userTokenReference user token reference 参数，用于 HttpLarkApprovalClient 流程中的校验、计算或对象转换。
     * @param tenantTokenReference tenant token reference 参数，用于 HttpLarkApprovalClient 流程中的校验、计算或对象转换。
     */
    public HttpLarkApprovalClient(WebClient.Builder webClientBuilder,
                                  MarketingMonitorProviderCredentialResolver credentialResolver,
                                  @Value("${canvas.approval.lark.base-url:https://open.feishu.cn}") String baseUrl,
                                  @Value("${canvas.approval.lark.user-token-reference:}") String userTokenReference,
                                  @Value("${canvas.approval.lark.tenant-token-reference:}") String tenantTokenReference) {
        this.webClient = (webClientBuilder == null ? WebClient.builder() : webClientBuilder)
                .baseUrl(trimTrailingSlash(baseUrl == null || baseUrl.isBlank()
                        ? "https://open.feishu.cn"
                        : baseUrl.trim()))
                .build();
        this.credentialResolver = credentialResolver;
        this.userTokenReference = userTokenReference == null ? "" : userTokenReference.trim();
        this.tenantTokenReference = tenantTokenReference == null ? "" : tenantTokenReference.trim();
    }

    /**
     * 调用飞书审批实例创建接口并返回外部实例编码。
     *
     * <p>方法会校验 approval code、表单 JSON 以及发起人 open_id/user_id，使用租户访问令牌发起 HTTP 请求。
     * 调用成功时只提取 {@code instance_code}；飞书返回错误码、令牌未配置或必填参数缺失会抛出异常。</p>
     *
     * @param request 创建飞书审批实例所需的租户、审批定义、发起人和表单参数
     * @return 飞书返回的实例编码；响应未携带时返回 {@code null}
     */
    @Override
    public String createInstance(LarkApprovalCreateInstanceRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (request == null) {
            throw new IllegalArgumentException("Lark approval create instance request is required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("approval_code", required(request.approvalCode(), "Lark approval approval_code is required"));
        body.put("form", required(request.form(), "Lark approval form is required"));
        putIfPresent(body, "uuid", request.uuid());
        putIfPresent(body, "open_id", request.openId());
        putIfPresent(body, "user_id", request.userId());
        putIfPresent(body, "department_id", request.departmentId());
        if (!body.containsKey("open_id") && !body.containsKey("user_id")) {
            throw new IllegalArgumentException("Lark approval create instance requires open_id or user_id");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JsonNode response = webClient.post()
                .uri("/open-apis/approval/v4/instances")
                .headers(headers -> headers.setBearerAuth(tenantToken(request.tenantId())))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(10));
        JsonNode data = dataOrThrow(response, "Lark approval instance create failed");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return trimToNull(data.path("instance_code").asText(null));
    }

    /**
     * getInstance 查询 domain.approval 场景的业务数据。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param instanceCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 getInstance 流程生成的业务结果。
     */
    @Override
    public LarkApprovalInstanceSnapshot getInstance(Long tenantId, String instanceCode) {
        String code = required(instanceCode, "Lark approval instance_code is required");
        String token = userToken(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/open-apis/approval/v4/instances/{instanceCode}")
                        .queryParam("user_id_type", "open_id")
                        .build(code))
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(10));
        JsonNode data = dataOrThrow(response, "Lark approval instance get failed");
        List<LarkApprovalTaskSnapshot> tasks = new ArrayList<>();
        JsonNode taskList = data.has("tasks") ? data.path("tasks") : data.path("task_list");
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (JsonNode task : taskList) {
            tasks.add(new LarkApprovalTaskSnapshot(
                    firstPresent(task.path("id").asText(null), task.path("task_id").asText(null)),
                    trimToNull(task.path("status").asText(null)),
                    firstPresent(task.path("user_id").asText(null), task.path("open_id").asText(null))));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new LarkApprovalInstanceSnapshot(
                trimToNull(data.path("instance_code").asText(code)),
                trimToNull(data.path("status").asText(null)),
                tasks);
    }

    /**
     * 调用飞书审批任务通过接口。
     *
     * <p>方法使用用户访问令牌向飞书提交 instance_code、task_id 和可选审批意见；成功无返回值，
     * 飞书错误响应会转换为运行时异常。</p>
     *
     * @param request 需要通过的飞书审批任务及操作者上下文
     */
    @Override
    public void approveTask(LarkApprovalTaskActionRequest request) {
        execute("/open-apis/approval/v4/tasks/approve", request);
    }

    /**
     * 调用飞书审批任务拒绝接口。
     *
     * <p>方法使用用户访问令牌向飞书提交 instance_code、task_id 和可选拒绝意见；成功无返回值，
     * 飞书错误响应会转换为运行时异常。</p>
     *
     * @param request 需要拒绝的飞书审批任务及操作者上下文
     */
    @Override
    public void rejectTask(LarkApprovalTaskActionRequest request) {
        execute("/open-apis/approval/v4/tasks/reject", request);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param path path 参数，用于 execute 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     */
    private void execute(String path, LarkApprovalTaskActionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Lark approval task action request is required");
        }
        String token = userToken(request.tenantId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("instance_code", required(request.instanceCode(), "Lark approval instance_code is required"));
        body.put("task_id", required(request.taskId(), "Lark approval task_id is required"));
        String comment = trimToNull(request.comment());
        if (comment != null) {
            body.put("comment", comment);
        }
        JsonNode response = webClient.post()
                .uri(path)
                .headers(headers -> headers.setBearerAuth(token))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(10));
        dataOrThrow(response, "Lark approval task action failed");
    }

    /**
     * 执行 dataOrThrow 流程，围绕 data or throw 完成校验、计算或结果组装。
     *
     * @param response response 参数，用于 dataOrThrow 流程中的校验、计算或对象转换。
     * @param prefix prefix 参数，用于 dataOrThrow 流程中的校验、计算或对象转换。
     * @return 返回 dataOrThrow 流程生成的业务结果。
     */
    private JsonNode dataOrThrow(JsonNode response, String prefix) {
        int code = response == null ? -1 : response.path("code").asInt(-1);
        if (code != 0) {
            String message = response == null ? "empty response" : response.path("msg").asText("unknown error");
            throw new IllegalStateException(prefix + ": " + message);
        }
        return response.path("data");
    }

    /**
     * 执行 userToken 流程，围绕 user token 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 user token 生成的文本或业务键。
     */
    private String userToken(Long tenantId) {
        String reference = trimToNull(userTokenReference);
        if (reference == null) {
            throw new IllegalStateException("canvas.approval.lark.user-token-reference is required");
        }
        String token = credentialResolver == null ? null : credentialResolver.resolve(tenantId, reference);
        if (trimToNull(token) == null) {
            throw new IllegalStateException("Lark approval user token is not configured");
        }
        return token.trim();
    }

    /**
     * 执行 tenantToken 流程，围绕 tenant token 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant token 生成的文本或业务键。
     */
    private String tenantToken(Long tenantId) {
        String reference = trimToNull(tenantTokenReference);
        if (reference == null) {
            throw new IllegalStateException("canvas.approval.lark.tenant-token-reference is required");
        }
        String token = credentialResolver == null ? null : credentialResolver.resolve(tenantId, reference);
        if (trimToNull(token) == null) {
            throw new IllegalStateException("Lark approval tenant token is not configured");
        }
        return token.trim();
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param raw raw 参数，用于 required 流程中的校验、计算或对象转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String raw, String message) {
        String value = trimToNull(raw);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param raw raw 参数，用于 trimToNull 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @param raw raw 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     */
    private void putIfPresent(Map<String, Object> body, String field, String raw) {
        String value = trimToNull(raw);
        if (value != null) {
            body.put(field, value);
        }
    }

    /**
     * 执行 firstPresent 流程，围绕 first present 完成校验、计算或结果组装。
     *
     * @param primary primary 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @param fallback fallback 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @return 返回 first present 生成的文本或业务键。
     */
    private String firstPresent(String primary, String fallback) {
        String value = trimToNull(primary);
        return value == null ? trimToNull(fallback) : value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param raw raw 参数，用于 trimTrailingSlash 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String trimTrailingSlash(String raw) {
        String value = raw;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
