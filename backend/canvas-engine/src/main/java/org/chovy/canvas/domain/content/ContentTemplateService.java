package org.chovy.canvas.domain.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingContentTemplateDO;
import org.chovy.canvas.dal.dataobject.MarketingContentTemplateVersionDO;
import org.chovy.canvas.dal.mapper.MarketingContentTemplateMapper;
import org.chovy.canvas.dal.mapper.MarketingContentTemplateVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
/**
 * ContentTemplateService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class ContentTemplateService {

    private static final Set<String> CHANNELS = Set.of("EMAIL", "SMS", "PUSH", "WECHAT", "IN_APP", "WEB", "VIDEO");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "ARCHIVED");

    private final MarketingContentTemplateMapper templateMapper;
    private final MarketingContentTemplateVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 ContentTemplateService 实例。
     *
     * @param templateMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ContentTemplateService(MarketingContentTemplateMapper templateMapper,
                                  MarketingContentTemplateVersionMapper versionMapper,
                                  ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenant tenant 参数，用于 list 流程中的校验、计算或对象转换。
     * @param keyword keyword 参数，用于 list 流程中的校验、计算或对象转换。
     * @param channel channel 参数，用于 list 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<TemplateView> list(TenantContext tenant, String keyword, String channel, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentTemplateDO> query = new LambdaQueryWrapper<MarketingContentTemplateDO>()
                .eq(MarketingContentTemplateDO::getTenantId, tenantId)
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                .orderByDesc(MarketingContentTemplateDO::getUpdatedAt)
                .orderByDesc(MarketingContentTemplateDO::getId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (MarketingContentSupport.hasText(keyword)) {
            String pattern = "%" + keyword.trim() + "%";
            query.and(w -> w.like(MarketingContentTemplateDO::getTemplateKey, pattern)
                    .or()
                    .like(MarketingContentTemplateDO::getDisplayName, pattern));
        }
        if (MarketingContentSupport.hasText(channel)) {
            query.eq(MarketingContentTemplateDO::getChannel,
                    MarketingContentSupport.normalizeUpper(channel, "EMAIL", CHANNELS, "template channel"));
        }
        if (MarketingContentSupport.hasText(status)) {
            query.eq(MarketingContentTemplateDO::getStatus,
                    MarketingContentSupport.normalizeUpper(status, "DRAFT", STATUSES, "template status"));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return templateMapper.selectList(query).stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenant tenant 参数，用于 save 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public TemplateView save(TenantContext tenant, TemplateCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String templateKey = MarketingContentSupport.normalizeKey(command.templateKey(), "templateKey");
        String channel = MarketingContentSupport.normalizeUpper(command.channel(), "EMAIL", CHANNELS, "template channel");
        String status = MarketingContentSupport.normalizeUpper(command.status(), "DRAFT", STATUSES, "template status");
        String body = MarketingContentSupport.requireText(command.body(), "template body");
        String designJson = MarketingContentSupport.normalizeJsonObject(objectMapper, command.designJson(), "{}", "designJson");
        String assetRefsJson = MarketingContentSupport.normalizeJsonArray(objectMapper, command.assetRefsJson(), "[]", "assetRefsJson");
        List<String> variables = MarketingContentSupport.variables(command.subject(), body);

        MarketingContentTemplateDO row = find(tenantId, templateKey);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        boolean insert = row == null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (insert) {
            row = new MarketingContentTemplateDO();
            row.setTenantId(tenantId);
            row.setTemplateKey(templateKey);
            row.setCreatedBy(MarketingContentSupport.operator(tenant, command.createdBy()));
        }
        row.setDisplayName(MarketingContentSupport.requireText(command.displayName(), "template displayName"));
        row.setChannel(channel);
        row.setSubject(MarketingContentSupport.trimToNull(command.subject()));
        row.setBody(body);
        row.setDesignJson(designJson);
        row.setAssetRefsJson(assetRefsJson);
        row.setVariablesJson(MarketingContentSupport.variablesJson(objectMapper, variables));
        row.setStatus(status);
        row.setReviewNotes(MarketingContentSupport.trimToNull(command.reviewNotes()));
        if (insert) {
            templateMapper.insert(row);
        } else {
            templateMapper.updateById(row);
        }
        writeVersion(row, MarketingContentSupport.operator(tenant, command.createdBy()));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 preview 流程中的校验、计算或对象转换。
     * @param templateKey 业务键，用于在同一租户下定位资源。
     * @param MapString map string 参数，用于 preview 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 preview 流程生成的业务结果。
     */
    public PreviewResult preview(TenantContext tenant, String templateKey, Map<String, Object> context) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        MarketingContentTemplateDO row = require(tenantId, templateKey);
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        String subject = MarketingContentSupport.render(row.getSubject(), context, missing);
        String body = MarketingContentSupport.render(row.getBody(), context, missing);
        return new PreviewResult(subject, body, List.copyOf(missing));
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 setStatus 流程中的校验、计算或对象转换。
     * @param templateKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 setStatus 流程生成的业务结果。
     */
    public TemplateView setStatus(TenantContext tenant, String templateKey, TemplateStatusCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String status = MarketingContentSupport.normalizeUpper(command.status(), "DRAFT", STATUSES, "template status");
        MarketingContentTemplateDO row = require(tenantId, templateKey);
        row.setStatus(status);
        row.setReviewNotes(MarketingContentSupport.trimToNull(command.reviewNotes()));
        templateMapper.updateById(row);
        writeVersion(row, MarketingContentSupport.operator(tenant, null));
        return toView(row);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param templateKey 业务键，用于在同一租户下定位资源。
     * @return 返回 require 流程生成的业务结果。
     */
    private MarketingContentTemplateDO require(Long tenantId, String templateKey) {
        MarketingContentTemplateDO row = find(tenantId, MarketingContentSupport.normalizeKey(templateKey, "templateKey"));
        if (row == null) {
            throw new IllegalArgumentException("template not found: " + templateKey);
        }
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param templateKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private MarketingContentTemplateDO find(Long tenantId, String templateKey) {
        return templateMapper.selectOne(new LambdaQueryWrapper<MarketingContentTemplateDO>()
                .eq(MarketingContentTemplateDO::getTenantId, tenantId)
                .eq(MarketingContentTemplateDO::getTemplateKey, templateKey)
                .last("LIMIT 1"));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void writeVersion(MarketingContentTemplateDO row, String actor) {
        MarketingContentTemplateVersionDO version = new MarketingContentTemplateVersionDO();
        version.setTenantId(row.getTenantId());
        version.setTemplateKey(row.getTemplateKey());
        version.setVersionNo(nextVersion(row.getTenantId(), row.getTemplateKey()));
        version.setDisplayName(row.getDisplayName());
        version.setChannel(row.getChannel());
        version.setSubject(row.getSubject());
        version.setBody(row.getBody());
        version.setDesignJson(row.getDesignJson());
        version.setAssetRefsJson(row.getAssetRefsJson());
        version.setVariablesJson(row.getVariablesJson());
        version.setStatus(row.getStatus());
        version.setCreatedBy(actor);
        versionMapper.insert(version);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param templateKey 业务键，用于在同一租户下定位资源。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
    private int nextVersion(Long tenantId, String templateKey) {
        MarketingContentTemplateVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<MarketingContentTemplateVersionDO>()
                .eq(MarketingContentTemplateVersionDO::getTenantId, tenantId)
                .eq(MarketingContentTemplateVersionDO::getTemplateKey, templateKey)
                .orderByDesc(MarketingContentTemplateVersionDO::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private TemplateView toView(MarketingContentTemplateDO row) {
        return new TemplateView(
                row.getTemplateKey(),
                row.getDisplayName(),
                row.getChannel(),
                row.getSubject(),
                row.getBody(),
                row.getDesignJson(),
                row.getAssetRefsJson(),
                MarketingContentSupport.variablesFromJson(objectMapper, row.getVariablesJson()),
                row.getStatus());
    }

    /**
     * TemplateCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TemplateCommand(
            String templateKey,
            String displayName,
            String channel,
            String subject,
            String body,
            String designJson,
            String assetRefsJson,
            String status,
            String reviewNotes,
            String createdBy) {
    }

    /**
     * TemplateStatusCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TemplateStatusCommand(String status, String reviewNotes) {
    }

    /**
     * TemplateView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TemplateView(
            String templateKey,
            String displayName,
            String channel,
            String subject,
            String body,
            String designJson,
            String assetRefsJson,
            List<String> variables,
            String status) {
    }

    /**
     * PreviewResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreviewResult(String renderedSubject, String renderedBody, List<String> missingVariables) {
    }
}
