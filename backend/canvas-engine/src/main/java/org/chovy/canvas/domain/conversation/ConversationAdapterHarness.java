package org.chovy.canvas.domain.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
/**
 * ConversationAdapterHarness 承载对应领域的业务规则、流程编排和结果转换。
 */
public class ConversationAdapterHarness {

    private final ConversationIngressService ingressService;
    private final ConversationAdapterCatalog adapterCatalog;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 ConversationAdapterHarness 实例。
     *
     * @param ingressService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ConversationAdapterHarness(ConversationIngressService ingressService) {
        this(ingressService, null, new ObjectMapper());
    }

    /**
     * 初始化 ConversationAdapterHarness 实例。
     *
     * @param ingressService 依赖组件，用于完成数据访问或外部能力调用。
     * @param adapterCatalog adapter catalog 参数，用于 ConversationAdapterHarness 流程中的校验、计算或对象转换。
     */
    public ConversationAdapterHarness(ConversationIngressService ingressService,
                                      ConversationAdapterCatalog adapterCatalog) {
        this(ingressService, adapterCatalog, new ObjectMapper());
    }

    @Autowired
    /**
     * 初始化 ConversationAdapterHarness 实例。
     *
     * @param ingressService 依赖组件，用于完成数据访问或外部能力调用。
     * @param adapterCatalog adapter catalog 参数，用于 ConversationAdapterHarness 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ConversationAdapterHarness(ConversationIngressService ingressService,
                                      ConversationAdapterCatalog adapterCatalog,
                                      ObjectMapper objectMapper) {
        this.ingressService = ingressService;
        this.adapterCatalog = adapterCatalog;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param adapter adapter 参数，用于 ingest 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 ingest 流程生成的业务结果。
     */
    public <T> ConversationIngressResp ingest(Long tenantId,
                                              ConversationReplyAdapter<T> adapter,
                                              T payload,
                                              String operator) {
        if (adapter == null) {
            throw new IllegalArgumentException("conversation reply adapter is required");
        }
        ConversationIngressReq req = adapter.toIngress(payload, new ConversationAdapterContext(tenantId, operator));
        validate(req);
        return ingressService.ingest(tenantId, req);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param adapterKey 业务键，用于在同一租户下定位资源。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 ingest 流程生成的业务结果。
     */
    public <T> ConversationIngressResp ingest(Long tenantId,
                                              String adapterKey,
                                              T payload,
                                              String operator) {
        if (adapterCatalog == null) {
            throw new IllegalStateException("conversation adapter catalog is required for key-based ingest");
        }
        return ingest(tenantId, adapterCatalog.require(adapterKey), payload, operator);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param adapterKey 业务键，用于在同一租户下定位资源。
     * @param rawPayload raw payload 参数，用于 ingestRaw 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 ingestRaw 流程生成的业务结果。
     */
    public ConversationIngressResp ingestRaw(Long tenantId,
                                             String adapterKey,
                                             Map<String, Object> rawPayload,
                                             String operator) {
        if (adapterCatalog == null) {
            throw new IllegalStateException("conversation adapter catalog is required for raw payload ingest");
        }
        ConversationReplyAdapter<Object> adapter = adapterCatalog.require(adapterKey);
        Object payload = objectMapper.convertValue(rawPayload == null ? Map.of() : rawPayload, adapter.payloadType());
        return ingest(tenantId, adapter, payload, operator);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     */
    private static void validate(ConversationIngressReq req) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (req == null) {
            throw new IllegalArgumentException("conversation adapter must produce an ingress request");
        }
        if (isBlank(req.userId()) || isBlank(req.channel())) {
            throw new IllegalArgumentException("conversation adapter ingress must include userId and channel");
        }
        if (isBlank(req.provider()) || isBlank(req.externalMessageId()) || isBlank(req.eventId())) {
            throw new IllegalArgumentException(
                    "conversation adapter ingress must include provider, externalMessageId, and eventId");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
