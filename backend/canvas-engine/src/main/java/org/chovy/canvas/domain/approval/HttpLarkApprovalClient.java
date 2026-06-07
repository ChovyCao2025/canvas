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

@Service
public class HttpLarkApprovalClient implements LarkApprovalClient {

    private final WebClient webClient;
    private final MarketingMonitorProviderCredentialResolver credentialResolver;
    private final String userTokenReference;
    private final String tenantTokenReference;

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

    @Override
    public String createInstance(LarkApprovalCreateInstanceRequest request) {
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
        JsonNode response = webClient.post()
                .uri("/open-apis/approval/v4/instances")
                .headers(headers -> headers.setBearerAuth(tenantToken(request.tenantId())))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(10));
        JsonNode data = dataOrThrow(response, "Lark approval instance create failed");
        return trimToNull(data.path("instance_code").asText(null));
    }

    @Override
    public LarkApprovalInstanceSnapshot getInstance(Long tenantId, String instanceCode) {
        String code = required(instanceCode, "Lark approval instance_code is required");
        String token = userToken(tenantId);
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
        for (JsonNode task : taskList) {
            tasks.add(new LarkApprovalTaskSnapshot(
                    firstPresent(task.path("id").asText(null), task.path("task_id").asText(null)),
                    trimToNull(task.path("status").asText(null)),
                    firstPresent(task.path("user_id").asText(null), task.path("open_id").asText(null))));
        }
        return new LarkApprovalInstanceSnapshot(
                trimToNull(data.path("instance_code").asText(code)),
                trimToNull(data.path("status").asText(null)),
                tasks);
    }

    @Override
    public void approveTask(LarkApprovalTaskActionRequest request) {
        execute("/open-apis/approval/v4/tasks/approve", request);
    }

    @Override
    public void rejectTask(LarkApprovalTaskActionRequest request) {
        execute("/open-apis/approval/v4/tasks/reject", request);
    }

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

    private JsonNode dataOrThrow(JsonNode response, String prefix) {
        int code = response == null ? -1 : response.path("code").asInt(-1);
        if (code != 0) {
            String message = response == null ? "empty response" : response.path("msg").asText("unknown error");
            throw new IllegalStateException(prefix + ": " + message);
        }
        return response.path("data");
    }

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

    private String required(String raw, String message) {
        String value = trimToNull(raw);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private void putIfPresent(Map<String, Object> body, String field, String raw) {
        String value = trimToNull(raw);
        if (value != null) {
            body.put(field, value);
        }
    }

    private String firstPresent(String primary, String fallback) {
        String value = trimToNull(primary);
        return value == null ? trimToNull(fallback) : value;
    }

    private static String trimTrailingSlash(String raw) {
        String value = raw;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
