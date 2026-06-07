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
public class ContentTemplateService {

    private static final Set<String> CHANNELS = Set.of("EMAIL", "SMS", "PUSH", "WECHAT", "IN_APP", "WEB", "VIDEO");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED", "ARCHIVED");

    private final MarketingContentTemplateMapper templateMapper;
    private final MarketingContentTemplateVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public ContentTemplateService(MarketingContentTemplateMapper templateMapper,
                                  MarketingContentTemplateVersionMapper versionMapper,
                                  ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public List<TemplateView> list(TenantContext tenant, String keyword, String channel, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentTemplateDO> query = new LambdaQueryWrapper<MarketingContentTemplateDO>()
                .eq(MarketingContentTemplateDO::getTenantId, tenantId)
                .orderByDesc(MarketingContentTemplateDO::getUpdatedAt)
                .orderByDesc(MarketingContentTemplateDO::getId);
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
        return templateMapper.selectList(query).stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
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
        boolean insert = row == null;
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
        return toView(row);
    }

    public PreviewResult preview(TenantContext tenant, String templateKey, Map<String, Object> context) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        MarketingContentTemplateDO row = require(tenantId, templateKey);
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        String subject = MarketingContentSupport.render(row.getSubject(), context, missing);
        String body = MarketingContentSupport.render(row.getBody(), context, missing);
        return new PreviewResult(subject, body, List.copyOf(missing));
    }

    @Transactional(rollbackFor = Exception.class)
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

    private MarketingContentTemplateDO require(Long tenantId, String templateKey) {
        MarketingContentTemplateDO row = find(tenantId, MarketingContentSupport.normalizeKey(templateKey, "templateKey"));
        if (row == null) {
            throw new IllegalArgumentException("template not found: " + templateKey);
        }
        return row;
    }

    private MarketingContentTemplateDO find(Long tenantId, String templateKey) {
        return templateMapper.selectOne(new LambdaQueryWrapper<MarketingContentTemplateDO>()
                .eq(MarketingContentTemplateDO::getTenantId, tenantId)
                .eq(MarketingContentTemplateDO::getTemplateKey, templateKey)
                .last("LIMIT 1"));
    }

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

    private int nextVersion(Long tenantId, String templateKey) {
        MarketingContentTemplateVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<MarketingContentTemplateVersionDO>()
                .eq(MarketingContentTemplateVersionDO::getTenantId, tenantId)
                .eq(MarketingContentTemplateVersionDO::getTemplateKey, templateKey)
                .orderByDesc(MarketingContentTemplateVersionDO::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

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

    public record TemplateStatusCommand(String status, String reviewNotes) {
    }

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

    public record PreviewResult(String renderedSubject, String renderedBody, List<String> missingVariables) {
    }
}
