package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.config.CanvasRuntimeMetrics;
import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.web.risk.RiskListAuditSink;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 风控名单治理服务，负责租户名单、主体哈希、展示脱敏和条目变更审计。
 */
public class RiskListService {

    private final RiskListAuditSink auditSink;
    private final RiskListSubjectHasher hasher;
    private final Clock clock;
    private final CanvasRuntimeMetrics metrics;
    private final StateStore store;

    /**
     * 创建名单治理服务，未传入指标组件时不记录导入指标。
     */
    public RiskListService(RiskListAuditSink auditSink, RiskListSubjectHasher hasher, Clock clock) {
        this(auditSink, hasher, clock, null, new InMemoryStateStore());
    }

    /**
     * 创建名单治理服务，并注入审计、哈希、时钟和可选指标组件。
     */
    public RiskListService(RiskListAuditSink auditSink,
                           RiskListSubjectHasher hasher,
                           Clock clock,
                           CanvasRuntimeMetrics metrics) {
        this(auditSink, hasher, clock, metrics, new InMemoryStateStore());
    }

    /**
     * 创建名单治理服务，并注入可替换状态仓储。
     */
    public RiskListService(RiskListAuditSink auditSink,
                           RiskListSubjectHasher hasher,
                           Clock clock,
                           CanvasRuntimeMetrics metrics,
                           StateStore store) {
        this.auditSink = auditSink;
        this.hasher = hasher;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.metrics = metrics;
        this.store = store == null ? new InMemoryStateStore() : store;
    }

    /**
     * 创建租户级风控名单，并记录创建审计事件。
     */
    public RiskListView createList(Long tenantId, RiskListCommand command, String actor) {
        RiskListView view = new RiskListView(tenantId, command.listKey(), command.listType(),
                command.subjectType(), "ACTIVE", command.requiresApproval(), actor);
        store.saveList(view);
        auditSink.record(tenantId, "LIST_CREATED", command.listKey(), command.listKey(), actor);
        return view;
    }

    /**
     * 查询租户下风控名单列表。
     */
    public List<RiskListView> listLists(Long tenantId) {
        return store.listLists(tenantId).stream()
                .filter(list -> Objects.equals(list.tenantId(), tenantId))
                .toList();
    }

    /**
     * 向指定名单添加一条主体记录，存储哈希值并仅返回脱敏展示值。
     */
    public RiskListEntryView addEntry(Long tenantId, String listKey, RiskListEntryCommand command, String actor) {
        RiskListView list = requireList(tenantId, listKey);
        validateSubjectType(list, command);
        Instant effectiveFrom = command.effectiveFrom() == null ? clock.instant() : command.effectiveFrom();
        RiskListEntryView view = store.saveEntry(new RiskListEntryView(
                0,
                tenantId,
                listKey,
                // 原始主体不离开治理边界；匹配使用哈希，界面输出使用脱敏值。
                hasher.hash(command.rawSubject()),
                mask(command.rawSubject(), command.subjectType()),
                command.reason(),
                command.source(),
                effectiveFrom,
                command.expiresAt(),
                actor));
        auditSink.record(tenantId, "ENTRY_ADDED", listKey, String.valueOf(view.id()), actor);
        return view;
    }

    /**
     * 批量导入名单条目，逐行接收合法数据并汇总拒绝原因。
     */
    public RiskListImportResult importEntries(Long tenantId, String listKey, RiskListImportCommand command, String actor) {
        requireList(tenantId, listKey);
        List<String> rowErrors = new ArrayList<>();
        int accepted = 0;
        int index = 1;
        for (RiskListEntryCommand row : command.rows()) {
            try {
                // 导入采用部分成功语义，避免单行错误阻断应急名单更新。
                addEntry(tenantId, listKey, row, actor);
                accepted++;
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (IllegalArgumentException error) {
                rowErrors.add("row " + index + ": subject type mismatch");
            }
            index++;
        }
        String auditId = auditSink.record(tenantId, "ENTRIES_IMPORTED", listKey, String.valueOf(command.rows().size()), actor);
        if (metrics != null) {
            metrics.recordRiskListImport(listKey, "accepted", accepted);
            metrics.recordRiskListImport(listKey, "rejected", rowErrors.size());
        }
        return new RiskListImportResult(command.rows().size(), accepted, rowErrors.size(), rowErrors, auditId);
    }

    /**
     * 查询指定名单当前生效的条目。
     */
    public List<RiskListEntryView> entries(Long tenantId, String listKey) {
        requireList(tenantId, listKey);
        return store.entries(tenantId, listKey).stream()
                .filter(this::effectiveNow)
                .toList();
    }

    /**
     * 按原始主体查找当前生效的名单条目。
     */
    public Optional<RiskListEntryView> lookup(Long tenantId, String listKey, String rawSubject) {
        String subjectHash = hasher.hash(rawSubject);
        return entries(tenantId, listKey).stream()
                .filter(entry -> entry.subjectHash().equals(subjectHash))
                .findFirst();
    }

    /**
     * 删除名单条目，并记录删除审计事件。
     */
    public void removeEntry(Long tenantId, String listKey, long entryId, String actor) {
        requireList(tenantId, listKey);
        store.removeEntry(tenantId, listKey, entryId);
        auditSink.record(tenantId, "ENTRY_REMOVED", listKey, String.valueOf(entryId), actor);
    }

    /**
     * 记录一次名单命中视图，供审计或解释链路关联决策运行编号。
     */
    public RiskListHitView recordHit(Long tenantId, String listKey, String rawSubject, String decisionRunId) {
        String subjectHash = hasher.hash(rawSubject);
        String masked = mask(rawSubject, requireList(tenantId, listKey).subjectType());
        return new RiskListHitView(tenantId, listKey, subjectHash, masked, decisionRunId);
    }

    /**
     * 获取名单，不存在时抛出业务异常。
     */
    private RiskListView requireList(Long tenantId, String listKey) {
        return store.findList(tenantId, listKey)
                .orElseThrow(() -> new IllegalArgumentException("risk list not found: " + listKey));
    }

    /**
     * 校验名单条目的主体类型与名单声明一致。
     */
    private void validateSubjectType(RiskListView list, RiskListEntryCommand command) {
        if (command.subjectType() != list.subjectType()) {
            throw new IllegalArgumentException("subject type mismatch");
        }
    }

    /**
     * 判断名单条目当前是否处于生效时间窗口内。
     */
    private boolean effectiveNow(RiskListEntryView entry) {
        Instant now = clock.instant();
        // 查询只返回当前生效行；过期行仍保留给审计和历史路径使用。
        boolean started = entry.effectiveFrom() == null || !entry.effectiveFrom().isAfter(now);
        boolean notExpired = entry.expiresAt() == null || entry.expiresAt().isAfter(now);
        return started && notExpired;
    }

    /**
     * 按主体类型生成面向运营人员的脱敏展示值。
     */
    private String mask(String rawSubject, RiskSubjectType subjectType) {
        if (rawSubject == null || rawSubject.isBlank()) {
            return "***";
        }
        // 脱敏值只保留识别形状，不暴露完整主体标识。
        return switch (subjectType) {
            case USER_ID, DEVICE_ID, GENERIC -> edgeMask(rawSubject);
            case EMAIL -> maskEmail(rawSubject);
            case PHONE, CARD -> rawSubject.length() <= 4 ? "***" : "***" + rawSubject.substring(rawSubject.length() - 4);
            case IP -> "***";
        };
    }

    /**
     * 对通用文本保留首尾字符并隐藏中间内容。
     */
    private String edgeMask(String text) {
        if (text.length() <= 2) {
            return "***";
        }
        return text.charAt(0) + "***" + text.charAt(text.length() - 1);
    }

    /**
     * 对邮箱保留首字符和域名并隐藏本地部分。
     */
    private String maskEmail(String text) {
        int at = text.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return text.charAt(0) + "***" + text.substring(at);
    }

    /**
     * 名单内存索引键，租户和名单键共同决定唯一名单。
     */
    private record Key(Long tenantId, String listKey) {
    }

    /**
     * 名单状态仓储，生产环境使用 JDBC，测试环境默认使用内存实现。
     */
    public interface StateStore {
        Optional<RiskListView> findList(Long tenantId, String listKey);

        List<RiskListView> listLists(Long tenantId);

        void saveList(RiskListView list);

        List<RiskListEntryView> entries(Long tenantId, String listKey);

        RiskListEntryView saveEntry(RiskListEntryView entry);

        void removeEntry(Long tenantId, String listKey, long entryId);
    }

    /**
     * 名单内存仓储。
     */
    public static final class InMemoryStateStore implements StateStore {
        private final Map<Key, RiskListView> lists = new LinkedHashMap<>();
        private final Map<Key, List<RiskListEntryView>> entries = new LinkedHashMap<>();
        private long nextEntryId = 1;

        @Override
        public Optional<RiskListView> findList(Long tenantId, String listKey) {
            return Optional.ofNullable(lists.get(new Key(tenantId, listKey)));
        }

        @Override
        public List<RiskListView> listLists(Long tenantId) {
            return lists.values().stream()
                    .filter(list -> Objects.equals(list.tenantId(), tenantId))
                    .toList();
        }

        @Override
        public void saveList(RiskListView list) {
            Key key = new Key(list.tenantId(), list.listKey());
            lists.put(key, list);
            entries.putIfAbsent(key, new ArrayList<>());
        }

        @Override
        public List<RiskListEntryView> entries(Long tenantId, String listKey) {
            return entries.getOrDefault(new Key(tenantId, listKey), List.of());
        }

        @Override
        public RiskListEntryView saveEntry(RiskListEntryView entry) {
            RiskListEntryView saved = new RiskListEntryView(nextEntryId++, entry.tenantId(), entry.listKey(),
                    entry.subjectHash(), entry.subjectMasked(), entry.reason(), entry.source(),
                    entry.effectiveFrom(), entry.expiresAt(), entry.createdBy());
            entries.computeIfAbsent(new Key(entry.tenantId(), entry.listKey()), ignored -> new ArrayList<>())
                    .add(saved);
            return saved;
        }

        @Override
        public void removeEntry(Long tenantId, String listKey, long entryId) {
            entries.getOrDefault(new Key(tenantId, listKey), List.of()).removeIf(entry -> entry.id() == entryId);
        }
    }
}
