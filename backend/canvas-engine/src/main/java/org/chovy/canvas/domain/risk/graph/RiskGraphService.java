package org.chovy.canvas.domain.risk.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控解释图谱服务，将主体与相邻主体及有效名单命中关联成解释视图。
 */
public class RiskGraphService {

    /**
     * 汇总指定主体的关联标识、相邻主体连接和名单命中。
     */
    public RiskGraphSummary summarize(Long tenantId, String subjectId,
                                      List<RiskGraphSubjectSnapshot> snapshots,
                                      List<RiskGraphListSubject> listSubjects) {
        RiskGraphSubjectSnapshot target = snapshots.stream()
                .filter(snapshot -> tenantId.equals(snapshot.tenantId()))
                .filter(snapshot -> subjectId.equals(snapshot.subjectId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("risk graph subject not found: " + subjectId));
        Map<String, String> targetIdentifiers = cleanIdentifiers(target.identifiers());
        Map<String, Integer> associationCounts = new LinkedHashMap<>();
        List<RiskGraphConnection> connections = new ArrayList<>();

        // 只在同租户内通过共享的标准化标识推断主体连接。
        for (RiskGraphSubjectSnapshot snapshot : snapshots) {
            if (!tenantId.equals(snapshot.tenantId()) || subjectId.equals(snapshot.subjectId())) {
                continue;
            }
            Map<String, String> shared = sharedIdentifiers(targetIdentifiers, cleanIdentifiers(snapshot.identifiers()));
            if (shared.isEmpty()) {
                continue;
            }
            shared.keySet().forEach(key -> associationCounts.merge(key, 1, Integer::sum));
            connections.add(new RiskGraphConnection(snapshot.subjectId(), snapshot.decisionRunId(), Map.copyOf(shared)));
        }

        return new RiskGraphSummary(
                subjectId,
                Map.copyOf(associationCounts),
                List.copyOf(connections),
                listHits(tenantId, targetIdentifiers, listSubjects));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param identifiers identifiers 参数，用于 listHits 流程中的校验、计算或对象转换。
     * @param listSubjects list subjects 参数，用于 listHits 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<RiskGraphListHit> listHits(Long tenantId, Map<String, String> identifiers,
                                            List<RiskGraphListSubject> listSubjects) {
        Map<String, RiskGraphListHit> hits = new LinkedHashMap<>();
        for (RiskGraphListSubject listSubject : listSubjects) {
            if (!tenantId.equals(listSubject.tenantId())) {
                continue;
            }
            String subjectValue = identifiers.get(listSubject.subjectType());
            if (subjectValue == null || !subjectValue.equals(listSubject.subjectValue())) {
                continue;
            }
            String key = listSubject.listKey() + ":" + listSubject.subjectType() + ":" + listSubject.subjectValue();
            // 多条名单记录可能描述同一主体值，对外按名单和主体元组去重。
            hits.putIfAbsent(key, new RiskGraphListHit(
                    listSubject.listKey(),
                    listSubject.subjectType(),
                    listSubject.subjectMasked(),
                    listSubject.reason()));
        }
        return List.copyOf(hits.values());
    }

    /**
     * 计算目标主体和候选主体之间共享的标准化标识。
     */
    private Map<String, String> sharedIdentifiers(Map<String, String> target, Map<String, String> candidate) {
        Map<String, String> shared = new LinkedHashMap<>();
        target.forEach((key, value) -> {
            if (value.equals(candidate.get(key))) {
                shared.put(key, value);
            }
        });
        return shared;
    }

    /**
     * 清理主体标识映射，移除空键和空值。
     */
    private Map<String, String> cleanIdentifiers(Map<String, String> identifiers) {
        Map<String, String> clean = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (identifiers == null) {
            return clean;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        identifiers.forEach((key, value) -> {
            if (key != null && value != null && !value.isBlank()) {
                clean.put(key, value);
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return clean;
    }
}
