package org.chovy.canvas.domain.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryDO;
import org.chovy.canvas.dal.dataobject.MarketingContentEntryVersionDO;
import org.chovy.canvas.dal.mapper.MarketingContentEntryMapper;
import org.chovy.canvas.dal.mapper.MarketingContentEntryVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * ContentEntryService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class ContentEntryService {

    private final MarketingContentEntryMapper entryMapper;
    private final MarketingContentEntryVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 ContentEntryService 实例。
     *
     * @param entryMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ContentEntryService(MarketingContentEntryMapper entryMapper,
                               MarketingContentEntryVersionMapper versionMapper,
                               ObjectMapper objectMapper) {
        this.entryMapper = entryMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenant tenant 参数，用于 list 流程中的校验、计算或对象转换。
     * @param keyword keyword 参数，用于 list 流程中的校验、计算或对象转换。
     * @param contentType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<EntryView> list(TenantContext tenant, String keyword, String contentType, String status) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        LambdaQueryWrapper<MarketingContentEntryDO> query = new LambdaQueryWrapper<MarketingContentEntryDO>()
                .eq(MarketingContentEntryDO::getTenantId, tenantId)
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                .orderByDesc(MarketingContentEntryDO::getUpdatedAt)
                .orderByDesc(MarketingContentEntryDO::getId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (MarketingContentSupport.hasText(keyword)) {
            String pattern = "%" + keyword.trim() + "%";
            query.and(w -> w.like(MarketingContentEntryDO::getEntryKey, pattern)
                    .or()
                    .like(MarketingContentEntryDO::getTitle, pattern));
        }
        if (MarketingContentSupport.hasText(contentType)) {
            query.eq(MarketingContentEntryDO::getContentType, contentType.trim().toUpperCase());
        }
        if (MarketingContentSupport.hasText(status)) {
            query.eq(MarketingContentEntryDO::getStatus, status.trim().toUpperCase());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return entryMapper.selectList(query).stream().map(this::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenant tenant 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public EntryView saveDraft(TenantContext tenant, EntryCommand command) {
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String entryKey = MarketingContentSupport.normalizeKey(command.entryKey(), "entryKey");
        String bodyJson = MarketingContentSupport.normalizeJsonObject(objectMapper, command.bodyJson(), "{}", "bodyJson");
        String seoJson = MarketingContentSupport.normalizeJsonObject(objectMapper, command.seoJson(), "{}", "seoJson");
        String assetRefsJson = MarketingContentSupport.normalizeJsonArray(objectMapper, command.assetRefsJson(), "[]", "assetRefsJson");
        String actor = MarketingContentSupport.operator(tenant, command.createdBy());

        MarketingContentEntryDO row = find(tenantId, entryKey);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        boolean insert = row == null;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (insert) {
            row = new MarketingContentEntryDO();
            row.setTenantId(tenantId);
            row.setEntryKey(entryKey);
            row.setCreatedBy(actor);
        }
        row.setContentType(MarketingContentSupport.requireText(command.contentType(), "contentType").toUpperCase());
        row.setTitle(MarketingContentSupport.requireText(command.title(), "entry title"));
        row.setSlug(MarketingContentSupport.normalizeSlug(command.slug()));
        row.setLocale(MarketingContentSupport.trimToNull(command.locale()));
        row.setSummary(MarketingContentSupport.trimToNull(command.summary()));
        row.setBodyJson(bodyJson);
        row.setSeoJson(seoJson);
        row.setAssetRefsJson(assetRefsJson);
        row.setStatus("DRAFT");
        row.setUpdatedBy(actor);
        row.setPublishedAt(null);
        if (insert) {
            entryMapper.insert(row);
        } else {
            entryMapper.updateById(row);
        }
        writeVersion(row, actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenant tenant 参数，用于 publish 流程中的校验、计算或对象转换。
     * @param entryKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public EntryView publish(TenantContext tenant, String entryKey, EntryStatusCommand command) {
        return transition(tenant, entryKey, command, "PUBLISHED");
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenant tenant 参数，用于 archive 流程中的校验、计算或对象转换。
     * @param entryKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 archive 流程生成的业务结果。
     */
    public EntryView archive(TenantContext tenant, String entryKey, EntryStatusCommand command) {
        return transition(tenant, entryKey, command, "ARCHIVED");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 transition 流程中的校验、计算或对象转换。
     * @param entryKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 transition 流程生成的业务结果。
     */
    private EntryView transition(TenantContext tenant, String entryKey, EntryStatusCommand command, String status) {
        // 准备本次处理所需的上下文和中间变量。
        Long tenantId = MarketingContentSupport.requireTenantId(tenant);
        String actor = MarketingContentSupport.operator(tenant, command == null ? null : command.actor());
        MarketingContentEntryDO row = require(tenantId, entryKey);
        row.setStatus(status);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(actor);
        row.setPublishedAt("PUBLISHED".equals(status) ? LocalDateTime.now() : row.getPublishedAt());
        entryMapper.updateById(row);
        writeVersion(row, actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param entryKey 业务键，用于在同一租户下定位资源。
     * @return 返回 require 流程生成的业务结果。
     */
    private MarketingContentEntryDO require(Long tenantId, String entryKey) {
        MarketingContentEntryDO row = find(tenantId, MarketingContentSupport.normalizeKey(entryKey, "entryKey"));
        if (row == null) {
            throw new IllegalArgumentException("content entry not found: " + entryKey);
        }
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param entryKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private MarketingContentEntryDO find(Long tenantId, String entryKey) {
        return entryMapper.selectOne(new LambdaQueryWrapper<MarketingContentEntryDO>()
                .eq(MarketingContentEntryDO::getTenantId, tenantId)
                .eq(MarketingContentEntryDO::getEntryKey, entryKey)
                .last("LIMIT 1"));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void writeVersion(MarketingContentEntryDO row, String actor) {
        MarketingContentEntryVersionDO version = new MarketingContentEntryVersionDO();
        version.setTenantId(row.getTenantId());
        version.setEntryKey(row.getEntryKey());
        version.setVersionNo(nextVersion(row.getTenantId(), row.getEntryKey()));
        version.setContentType(row.getContentType());
        version.setTitle(row.getTitle());
        version.setSlug(row.getSlug());
        version.setLocale(row.getLocale());
        version.setSummary(row.getSummary());
        version.setBodyJson(row.getBodyJson());
        version.setSeoJson(row.getSeoJson());
        version.setAssetRefsJson(row.getAssetRefsJson());
        version.setStatus(row.getStatus());
        version.setCreatedBy(actor);
        versionMapper.insert(version);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param entryKey 业务键，用于在同一租户下定位资源。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
    private int nextVersion(Long tenantId, String entryKey) {
        MarketingContentEntryVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<MarketingContentEntryVersionDO>()
                .eq(MarketingContentEntryVersionDO::getTenantId, tenantId)
                .eq(MarketingContentEntryVersionDO::getEntryKey, entryKey)
                .orderByDesc(MarketingContentEntryVersionDO::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private EntryView toView(MarketingContentEntryDO row) {
        return new EntryView(
                row.getEntryKey(),
                row.getContentType(),
                row.getTitle(),
                row.getSlug(),
                row.getLocale(),
                row.getStatus(),
                row.getBodyJson(),
                row.getAssetRefsJson());
    }

    /**
     * EntryCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EntryCommand(
            String entryKey,
            String contentType,
            String title,
            String slug,
            String locale,
            String summary,
            String bodyJson,
            String seoJson,
            String assetRefsJson,
            String createdBy) {
    }

    /**
     * EntryStatusCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EntryStatusCommand(String actor) {
    }

    /**
     * EntryView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EntryView(
            String entryKey,
            String contentType,
            String title,
            String slug,
            String locale,
            String status,
            String bodyJson,
            String assetRefsJson) {
    }
}
