package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingFormDefinitionDO;
import org.chovy.canvas.dal.dataobject.MarketingFormSubmissionDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingFormDefinitionMapper;
import org.chovy.canvas.dal.mapper.MarketingFormSubmissionMapper;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
/**
 * MarketingFormService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class MarketingFormService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String DEFAULT_FIELD_SCHEMA = """
            [{"key":"email","label":"邮箱","type":"email","required":true},{"key":"name","label":"姓名","type":"text","required":false},{"key":"marketingConsent","label":"同意接收营销消息","type":"checkbox","required":false}]
            """;

    private final MarketingFormDefinitionMapper definitionMapper;
    private final MarketingFormSubmissionMapper submissionMapper;
    private final CdpUserService cdpUserService;
    private final CdpUserProfileMapper profileMapper;
    private final CustomerChannelMapper channelMapper;
    private final MarketingConsentMapper consentMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<CanvasDisruptorService> disruptorService;

    /**
     * 初始化 MarketingFormService 实例。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param submissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cdpUserService 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param channelMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingFormService(MarketingFormDefinitionMapper definitionMapper,
                                MarketingFormSubmissionMapper submissionMapper,
                                CdpUserService cdpUserService,
                                CdpUserProfileMapper profileMapper,
                                CustomerChannelMapper channelMapper,
                                MarketingConsentMapper consentMapper,
                                ObjectMapper objectMapper) {
        this(definitionMapper, submissionMapper, cdpUserService, profileMapper, channelMapper,
                consentMapper, objectMapper, null);
    }

    @Autowired
    /**
     * 初始化 MarketingFormService 实例。
     *
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param submissionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param cdpUserService 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param channelMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param disruptorService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MarketingFormService(MarketingFormDefinitionMapper definitionMapper,
                                MarketingFormSubmissionMapper submissionMapper,
                                CdpUserService cdpUserService,
                                CdpUserProfileMapper profileMapper,
                                CustomerChannelMapper channelMapper,
                                MarketingConsentMapper consentMapper,
                                ObjectMapper objectMapper,
                                ObjectProvider<CanvasDisruptorService> disruptorService) {
        this.definitionMapper = definitionMapper;
        this.submissionMapper = submissionMapper;
        this.cdpUserService = cdpUserService;
        this.profileMapper = profileMapper;
        this.channelMapper = channelMapper;
        this.consentMapper = consentMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.disruptorService = disruptorService;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<FormDefinitionView> list(Long tenantId) {
        return definitionMapper.selectList(new LambdaQueryWrapper<MarketingFormDefinitionDO>()
                        .eq(MarketingFormDefinitionDO::getTenantId, safeTenantId(tenantId))
                        .orderByDesc(MarketingFormDefinitionDO::getUpdatedAt)
                        .orderByDesc(MarketingFormDefinitionDO::getId))
                .stream()
                .map(this::toDefinitionView)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 get 流程生成的业务结果。
     */
    public FormDefinitionView get(Long tenantId, Long id) {
        return toDefinitionView(getRequiredById(safeTenantId(tenantId), id));
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public FormDefinitionView create(Long tenantId, FormDefinitionCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingFormDefinitionDO row = new MarketingFormDefinitionDO();
        row.setTenantId(scopedTenantId);
        row.setPublicKey(normalizePublicKey(command.publicKey()));
        row.setName(requireText(command.name(), "name"));
        row.setDescription(trimToLimit(command.description(), 500));
        row.setStatus(Boolean.FALSE.equals(command.active()) ? STATUS_INACTIVE : STATUS_ACTIVE);
        row.setFieldSchemaJson(normalizeJson(command.fieldSchemaJson(), DEFAULT_FIELD_SCHEMA, "fieldSchemaJson"));
        row.setSubmitActionJson(normalizeJson(command.submitActionJson(), "{}", "submitActionJson"));
        row.setSuccessMessage(defaultString(command.successMessage(), "提交成功"));
        row.setCreatedBy(defaultString(command.createdBy(), "operator"));
        definitionMapper.insert(row);
        return toDefinitionView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public FormDefinitionView update(Long tenantId, Long id, FormDefinitionCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingFormDefinitionDO row = getRequiredById(scopedTenantId, id);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(command.publicKey())) {
            row.setPublicKey(normalizePublicKey(command.publicKey()));
        }
        if (hasText(command.name())) {
            row.setName(requireText(command.name(), "name"));
        }
        row.setDescription(trimToLimit(command.description(), 500));
        row.setFieldSchemaJson(normalizeJson(command.fieldSchemaJson(), row.getFieldSchemaJson(), "fieldSchemaJson"));
        row.setSubmitActionJson(normalizeJson(command.submitActionJson(), row.getSubmitActionJson(), "submitActionJson"));
        row.setSuccessMessage(defaultString(command.successMessage(), row.getSuccessMessage()));
        if (command.active() != null) {
            row.setStatus(Boolean.TRUE.equals(command.active()) ? STATUS_ACTIVE : STATUS_INACTIVE);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        definitionMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toDefinitionView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param active active 参数，用于 setStatus 流程中的校验、计算或对象转换。
     * @return 返回 setStatus 流程生成的业务结果。
     */
    public FormDefinitionView setStatus(Long tenantId, Long id, boolean active) {
        MarketingFormDefinitionDO row = getRequiredById(safeTenantId(tenantId), id);
        row.setStatus(active ? STATUS_ACTIVE : STATUS_INACTIVE);
        definitionMapper.updateById(row);
        return toDefinitionView(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param formId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 submissions 汇总后的集合、分页或映射视图。
     */
    public List<SubmissionView> submissions(Long tenantId, Long formId, int limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        int pageSize = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 200));
        LambdaQueryWrapper<MarketingFormSubmissionDO> query = new LambdaQueryWrapper<MarketingFormSubmissionDO>()
                .eq(MarketingFormSubmissionDO::getTenantId, scopedTenantId)
                .orderByDesc(MarketingFormSubmissionDO::getCreatedAt)
                .orderByDesc(MarketingFormSubmissionDO::getId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (formId != null) {
            query.eq(MarketingFormSubmissionDO::getFormId, formId);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return submissionMapper.selectPage(new Page<>(1, pageSize), query)
                .getRecords()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(this::toSubmissionView)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param publicKey 业务键，用于在同一租户下定位资源。
     * @return 返回 publicForm 流程生成的业务结果。
     */
    public PublicFormView publicForm(String publicKey) {
        MarketingFormDefinitionDO row = getRequiredByPublicKey(publicKey);
        return new PublicFormView(
                row.getPublicKey(),
                row.getName(),
                row.getDescription(),
                row.getStatus(),
                row.getFieldSchemaJson(),
                row.getSuccessMessage());
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param publicKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 submit 流程生成的业务结果。
     */
    public SubmitResult submit(String publicKey, PublicSubmitCommand command) {
        MarketingFormDefinitionDO definition = getRequiredByPublicKey(publicKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!STATUS_ACTIVE.equalsIgnoreCase(definition.getStatus())) {
            throw new IllegalStateException("marketing form is not active: " + definition.getPublicKey());
        }
        Map<String, Object> response = command.response() == null ? Map.of() : command.response();
        Map<String, Object> utm = command.utm() == null ? Map.of() : command.utm();
        String email = normalizeEmail(firstText(response, "email", "mail"));
        String phone = firstText(response, "phone", "mobile", "mobilePhone", "tel");
        String name = firstText(response, "name", "fullName", "displayName");
        CdpUserProfileDO profile = resolveProfile(definition, command, response, email, phone);
        if (profile != null) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            updateProfile(definition, profile, response, email, phone, name);
            upsertChannels(definition, profile.getUserId(), email, phone);
        }
        ConsentDecision consent = resolveConsent(command, response, email, phone);
        if (profile != null && consent != null) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (String channel : consent.channels()) {
                upsertConsent(definition, profile.getUserId(), channel, consent.status());
            }
        }

        MarketingFormSubmissionDO row = new MarketingFormSubmissionDO();
        row.setTenantId(definition.getTenantId());
        row.setFormId(definition.getId());
        row.setPublicKey(definition.getPublicKey());
        row.setUserId(profile == null ? null : profile.getUserId());
        row.setAnonymousId(trimToLimit(command.anonymousId(), 128));
        row.setResponseJson(toJson(response));
        row.setUtmJson(utm.isEmpty() ? null : toJson(utm));
        row.setConsentChannel(consent == null ? null : String.join(",", consent.channels()));
        row.setConsentStatus(consent == null ? null : consent.status());
        row.setIdempotencyKey(defaultString(command.idempotencyKey(), "form:" + UUID.randomUUID()));
        row.setUserAgent(trimToLimit(command.userAgent(), 512));
        row.setSubmitIpHash(trimToLimit(command.submitIpHash(), 128));
        row.setTriggerEventCode(triggerEventCode(definition));
        try {
            submissionMapper.insert(row);
        } catch (DuplicateKeyException duplicate) {
            MarketingFormSubmissionDO existing = findSubmissionByIdempotency(
                    definition.getTenantId(), definition.getId(), row.getIdempotencyKey());
            if (existing != null) {
                return toSubmitResult(existing, definition, false);
            }
            throw duplicate;
        }
        boolean triggered = maybePublishTrigger(definition, row, response, utm);
        return toSubmitResult(row, definition, triggered);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param definition definition 参数，用于 resolveProfile 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param response response 参数，用于 resolveProfile 流程中的校验、计算或对象转换。
     * @param email email 参数，用于 resolveProfile 流程中的校验、计算或对象转换。
     * @param phone phone 参数，用于 resolveProfile 流程中的校验、计算或对象转换。
     * @return 返回 resolveProfile 流程生成的业务结果。
     */
    private CdpUserProfileDO resolveProfile(MarketingFormDefinitionDO definition,
                                            PublicSubmitCommand command,
                                            Map<String, Object> response,
                                            String email,
                                            String phone) {
        // 准备本次处理所需的上下文和中间变量。
        String userId = firstText(response, "userId", "user_id", "uid");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(userId)) {
            return cdpUserService.ensureUser(definition.getTenantId(), userId, "MARKETING_FORM", definition.getPublicKey());
        }
        if (hasText(email)) {
            return cdpUserService.ensureUserByIdentity(
                    definition.getTenantId(), "EMAIL", email, "MARKETING_FORM", definition.getPublicKey());
        }
        if (hasText(phone)) {
            return cdpUserService.ensureUserByIdentity(
                    definition.getTenantId(), "PHONE", phone, "MARKETING_FORM", definition.getPublicKey());
        }
        if (hasText(command.anonymousId())) {
            return cdpUserService.ensureUser(
                    definition.getTenantId(), "anonymous:" + command.anonymousId().trim(),
                    "MARKETING_FORM", definition.getPublicKey());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param definition definition 参数，用于 updateProfile 流程中的校验、计算或对象转换。
     * @param profile profile 参数，用于 updateProfile 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 updateProfile 流程中的校验、计算或对象转换。
     * @param email email 参数，用于 updateProfile 流程中的校验、计算或对象转换。
     * @param phone phone 参数，用于 updateProfile 流程中的校验、计算或对象转换。
     * @param name 名称文本，用于展示或唯一性校验。
     */
    private void updateProfile(MarketingFormDefinitionDO definition,
                               CdpUserProfileDO profile,
                               Map<String, Object> response,
                               String email,
                               String phone,
                               String name) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(name)) {
            profile.setDisplayName(name);
        } else if (!hasText(profile.getDisplayName()) && hasText(email)) {
            profile.setDisplayName(email);
        }
        if (hasText(email)) {
            profile.setEmail(email);
        }
        if (hasText(phone)) {
            profile.setPhone(phone.trim());
        }
        if (!hasText(profile.getStatus())) {
            profile.setStatus("ACTIVE");
        }
        profile.setLastSeenAt(LocalDateTime.now());
        profile.setPropertiesJson(mergeProfileProperties(profile.getPropertiesJson(), definition, response));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        profileMapper.updateById(profile);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param definition definition 参数，用于 upsertChannels 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param email email 参数，用于 upsertChannels 流程中的校验、计算或对象转换。
     * @param phone phone 参数，用于 upsertChannels 流程中的校验、计算或对象转换。
     */
    private void upsertChannels(MarketingFormDefinitionDO definition, String userId, String email, String phone) {
        if (hasText(email)) {
            upsertChannel(definition.getTenantId(), userId, "EMAIL", email,
                    Map.of("source", "marketing_form", "publicKey", definition.getPublicKey()));
        }
        if (hasText(phone)) {
            upsertChannel(definition.getTenantId(), userId, "SMS", phone.trim(),
                    Map.of("source", "marketing_form", "publicKey", definition.getPublicKey()));
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 upsertChannel 流程中的校验、计算或对象转换。
     * @param address address 参数，用于 upsertChannel 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 upsertChannel 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 upsertChannel 流程中的校验、计算或对象转换。
     */
    private void upsertChannel(Long tenantId, String userId, String channel, String address, Map<String, Object> metadata) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CustomerChannelDO row = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                .eq(CustomerChannelDO::getTenantId, tenantId)
                .eq(CustomerChannelDO::getUserId, userId)
                .eq(CustomerChannelDO::getChannel, channel)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new CustomerChannelDO();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setChannel(channel);
            row.setEnabled(1);
            row.setVerified(0);
            row.setAddress(address);
            row.setMetadata(toJson(metadata));
            channelMapper.insert(row);
        } else {
            row.setAddress(address);
            row.setEnabled(1);
            row.setMetadata(toJson(metadata));
            channelMapper.updateById(row);
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param response response 参数，用于 resolveConsent 流程中的校验、计算或对象转换。
     * @param email email 参数，用于 resolveConsent 流程中的校验、计算或对象转换。
     * @param phone phone 参数，用于 resolveConsent 流程中的校验、计算或对象转换。
     * @return 返回 resolveConsent 流程生成的业务结果。
     */
    private ConsentDecision resolveConsent(PublicSubmitCommand command,
                                           Map<String, Object> response,
                                           String email,
                                           String phone) {
        String explicitStatus = normalizeConsentStatusOrNull(command.consentStatus());
        String requestedChannel = normalizeChannelOrNull(command.consentChannel());
        if (explicitStatus != null) {
            return new ConsentDecision(channelsForConsent(requestedChannel, email, phone), explicitStatus);
        }
        Object rawConsent = firstRaw(response, "marketingConsent", "consent", "optIn", "subscribe");
        if (rawConsent == null) {
            return null;
        }
        String status = truthy(rawConsent) ? MarketingConsentDO.OPT_IN : MarketingConsentDO.OPT_OUT;
        return new ConsentDecision(channelsForConsent(requestedChannel, email, phone), status);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param requestedChannel requested channel 参数，用于 channelsForConsent 流程中的校验、计算或对象转换。
     * @param email email 参数，用于 channelsForConsent 流程中的校验、计算或对象转换。
     * @param phone phone 参数，用于 channelsForConsent 流程中的校验、计算或对象转换。
     * @return 返回 channels for consent 汇总后的集合、分页或映射视图。
     */
    private List<String> channelsForConsent(String requestedChannel, String email, String phone) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(requestedChannel) && !"ALL".equals(requestedChannel)) {
            return List.of(requestedChannel);
        }
        List<String> channels = new ArrayList<>();
        if (hasText(email)) {
            channels.add("EMAIL");
        }
        if (hasText(phone)) {
            channels.add("SMS");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return channels.isEmpty() ? List.of("ALL") : channels;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param definition definition 参数，用于 upsertConsent 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 upsertConsent 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     */
    private void upsertConsent(MarketingFormDefinitionDO definition, String userId, String channel, String status) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingConsentDO row = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                .eq(MarketingConsentDO::getTenantId, definition.getTenantId())
                .eq(MarketingConsentDO::getUserId, userId)
                .eq(MarketingConsentDO::getChannel, channel)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new MarketingConsentDO();
            row.setTenantId(definition.getTenantId());
            row.setUserId(userId);
            row.setChannel(channel);
            row.setConsentStatus(status);
            row.setSource("marketing_form:" + definition.getPublicKey());
            consentMapper.insert(row);
        } else {
            row.setConsentStatus(status);
            row.setSource("marketing_form:" + definition.getPublicKey());
            consentMapper.updateById(row);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param definition definition 参数，用于 maybePublishTrigger 流程中的校验、计算或对象转换。
     * @param submission submission 参数，用于 maybePublishTrigger 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 maybePublishTrigger 流程中的校验、计算或对象转换。
     * @param utm utm 参数，用于 maybePublishTrigger 流程中的校验、计算或对象转换。
     * @return 返回 maybe publish trigger 的布尔判断结果。
     */
    private boolean maybePublishTrigger(MarketingFormDefinitionDO definition,
                                        MarketingFormSubmissionDO submission,
                                        Map<String, Object> response,
                                        Map<String, Object> utm) {
        Map<String, Object> action = submitAction(definition);
        Long canvasId = longValue(action.get("canvasId"));
        String eventCode = cleanText(action.get("triggerEventCode"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (canvasId == null || !hasText(eventCode) || !hasText(submission.getUserId())) {
            return false;
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CanvasDisruptorService publisher = disruptorService == null ? null : disruptorService.getIfAvailable();
        if (publisher == null) {
            return false;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("formPublicKey", definition.getPublicKey());
        payload.put("submissionId", submission.getId());
        payload.put("response", response);
        payload.put("utm", utm);
        payload.put("anonymousId", submission.getAnonymousId());
        publisher.publish(canvasId, submission.getUserId(), "BEHAVIOR", NodeType.EVENT_TRIGGER,
                eventCode, payload, submission.getIdempotencyKey());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return true;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param definition definition 参数，用于 triggerEventCode 流程中的校验、计算或对象转换。
     * @return 返回 trigger event code 生成的文本或业务键。
     */
    private String triggerEventCode(MarketingFormDefinitionDO definition) {
        return cleanText(submitAction(definition).get("triggerEventCode"));
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param definition definition 参数，用于 submitAction 流程中的校验、计算或对象转换。
     * @return 返回 submitAction 流程生成的业务结果。
     */
    private Map<String, Object> submitAction(MarketingFormDefinitionDO definition) {
        String json = definition.getSubmitActionJson();
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 getRequiredById 流程生成的业务结果。
     */
    private MarketingFormDefinitionDO getRequiredById(Long tenantId, Long id) {
        MarketingFormDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<MarketingFormDefinitionDO>()
                .eq(MarketingFormDefinitionDO::getTenantId, tenantId)
                .eq(MarketingFormDefinitionDO::getId, id)
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("marketing form does not exist: " + id);
        }
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param publicKey 业务键，用于在同一租户下定位资源。
     * @return 返回 getRequiredByPublicKey 流程生成的业务结果。
     */
    private MarketingFormDefinitionDO getRequiredByPublicKey(String publicKey) {
        MarketingFormDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<MarketingFormDefinitionDO>()
                .eq(MarketingFormDefinitionDO::getPublicKey, normalizePublicKey(publicKey))
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("marketing form does not exist: " + publicKey);
        }
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param formId 业务对象 ID，用于定位具体记录。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private MarketingFormSubmissionDO findSubmissionByIdempotency(Long tenantId, Long formId, String idempotencyKey) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<MarketingFormSubmissionDO>()
                .eq(MarketingFormSubmissionDO::getTenantId, tenantId)
                .eq(MarketingFormSubmissionDO::getFormId, formId)
                .eq(MarketingFormSubmissionDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private FormDefinitionView toDefinitionView(MarketingFormDefinitionDO row) {
        return new FormDefinitionView(
                row.getId(),
                row.getPublicKey(),
                row.getName(),
                row.getDescription(),
                row.getStatus(),
                row.getFieldSchemaJson(),
                row.getSubmitActionJson(),
                row.getSuccessMessage(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SubmissionView toSubmissionView(MarketingFormSubmissionDO row) {
        return new SubmissionView(
                row.getId(),
                row.getFormId(),
                row.getPublicKey(),
                row.getUserId(),
                row.getAnonymousId(),
                row.getResponseJson(),
                row.getUtmJson(),
                row.getConsentChannel(),
                row.getConsentStatus(),
                row.getIdempotencyKey(),
                row.getTriggerEventCode(),
                row.getCreatedAt());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param definition definition 参数，用于 toSubmitResult 流程中的校验、计算或对象转换。
     * @param triggered triggered 参数，用于 toSubmitResult 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private SubmitResult toSubmitResult(MarketingFormSubmissionDO row, MarketingFormDefinitionDO definition, boolean triggered) {
        return new SubmitResult(
                row.getId(),
                row.getUserId(),
                definition.getSuccessMessage(),
                row.getTriggerEventCode(),
                triggered);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param existingJson JSON 字符串，承载结构化配置或明细。
     * @param definition definition 参数，用于 mergeProfileProperties 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 mergeProfileProperties 流程中的校验、计算或对象转换。
     * @return 返回 merge profile properties 生成的文本或业务键。
     */
    private String mergeProfileProperties(String existingJson,
                                          MarketingFormDefinitionDO definition,
                                          Map<String, Object> response) {
        Map<String, Object> properties = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(existingJson)) {
            try {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                JsonNode node = objectMapper.readTree(existingJson);
                if (node != null && node.isObject()) {
                    properties.putAll(objectMapper.convertValue(node, MAP_TYPE));
                }
            } catch (RuntimeException | JsonProcessingException ignored) {
                properties.put("previousPropertiesJson", existingJson);
            }
        }
        properties.put("lastMarketingFormKey", definition.getPublicKey());
        properties.put("lastMarketingFormSubmittedAt", LocalDateTime.now().toString());
        properties.put("lastMarketingFormFields", response);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toJson(properties);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeJson 流程中的校验、计算或对象转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeJson(String value, String fallback, String fieldName) {
        String json = defaultString(value, fallback);
        try {
            objectMapper.readTree(json);
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(fieldName + " must be valid JSON");
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON serialization failed");
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizePublicKey(String value) {
        String text = hasText(value) ? value.trim() : "mf_" + UUID.randomUUID().toString().replace("-", "");
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "-")
                .replaceAll("-+", "-");
        if (!hasText(normalized)) {
            normalized = "mf_" + UUID.randomUUID().toString().replace("-", "");
        }
        return trimToLimit(normalized, 96);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeConsentStatusOrNull(String status) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("OPTIN".equals(normalized)) {
            return MarketingConsentDO.OPT_IN;
        }
        if ("OPTOUT".equals(normalized)) {
            return MarketingConsentDO.OPT_OUT;
        }
        if (MarketingConsentDO.OPT_IN.equals(normalized) || MarketingConsentDO.OPT_OUT.equals(normalized)) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return normalized;
        }
        throw new IllegalArgumentException("unsupported consent status: " + status);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param channel channel 参数，用于 normalizeChannelOrNull 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeChannelOrNull(String channel) {
        if (!hasText(channel)) {
            return null;
        }
        return channel.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param email email 参数，用于 normalizeEmail 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeEmail(String email) {
        return hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 firstRaw 流程中的校验、计算或对象转换。
     * @param map map 参数，用于 firstRaw 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstRaw 流程中的校验、计算或对象转换。
     * @return 返回 firstRaw 流程生成的业务结果。
     */
    private Object firstRaw(Map<String, Object> map, String... keys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (map == null || map.isEmpty()) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            for (String key : keys) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @param map map 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstText 流程中的校验、计算或对象转换。
     * @return 返回 first text 生成的文本或业务键。
     */
    private String firstText(Map<String, Object> map, String... keys) {
        return cleanText(firstRaw(map, keys));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 clean text 生成的文本或业务键。
     */
    private String cleanText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
    private Long longValue(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = cleanText(value);
        if (!hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 truthy 的布尔判断结果。
     */
    private boolean truthy(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = cleanText(value);
        if (!hasText(text)) {
            return false;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y", "on", "opt_in" -> true;
            default -> false;
        };
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToLimit(String value, int limit) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * ConsentDecision 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record ConsentDecision(List<String> channels, String status) {
    }

    /**
     * FormDefinitionCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record FormDefinitionCommand(
            String publicKey,
            String name,
            String description,
            String fieldSchemaJson,
            String submitActionJson,
            String successMessage,
            Boolean active,
            String createdBy) {
    }

    /**
     * PublicSubmitCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PublicSubmitCommand(
            Map<String, Object> response,
            Map<String, Object> utm,
            String anonymousId,
            String idempotencyKey,
            String consentChannel,
            String consentStatus,
            String userAgent,
            String submitIpHash) {
    }

    /**
     * FormDefinitionView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record FormDefinitionView(
            Long id,
            String publicKey,
            String name,
            String description,
            String status,
            String fieldSchemaJson,
            String submitActionJson,
            String successMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /**
     * PublicFormView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PublicFormView(
            String publicKey,
            String name,
            String description,
            String status,
            String fieldSchemaJson,
            String successMessage) {
    }

    /**
     * SubmissionView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SubmissionView(
            Long id,
            Long formId,
            String publicKey,
            String userId,
            String anonymousId,
            String responseJson,
            String utmJson,
            String consentChannel,
            String consentStatus,
            String idempotencyKey,
            String triggerEventCode,
            LocalDateTime createdAt) {
    }

    /**
     * SubmitResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SubmitResult(
            Long submissionId,
            String userId,
            String successMessage,
            String triggerEventCode,
            boolean triggered) {
    }
}
