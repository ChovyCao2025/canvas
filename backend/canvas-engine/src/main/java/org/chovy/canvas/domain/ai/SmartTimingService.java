package org.chovy.canvas.domain.ai;

import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SmartTimingService 编排 domain.ai 场景的领域业务规则。
 */
@Service
public class SmartTimingService {

    private final ChurnFeatureSnapshotService featureSnapshotService;
    private final AiPredictionProperties properties;

    /**
     * 创建 SmartTimingService 实例并注入 domain.ai 场景依赖。
     * @param featureSnapshotService 依赖组件，用于完成数据访问或外部能力调用。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     */
    public SmartTimingService(ChurnFeatureSnapshotService featureSnapshotService,
                              AiPredictionProperties properties) {
        this.featureSnapshotService = featureSnapshotService;
        this.properties = properties;
    }

    /**
     * 根据用户近期事件活跃小时推断最佳发送小时。
     * 当历史事件不足时返回配置的默认小时；返回值会被归一化到 0-23，供预测快照和运营触达使用。
     */
    public int bestSendHour(String userId, LocalDate runDate) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<EventLogDO> events = featureSnapshotService.recentEvents(userId, runDate).stream()
                .filter(event -> event.getCreatedAt() != null)
                .toList();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (events.size() < Math.max(1, properties.getSparseHistoryMinEvents())) {
            return normalizedHour(properties.getDefaultBestSendHour());
        }
        Map<Integer, Long> byHour = events.stream()
                .collect(Collectors.groupingBy(event -> normalizedHour(event.getCreatedAt().getHour()), Collectors.counting()));
        return byHour.entrySet().stream()
                .max(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(entry -> -entry.getKey()))
                .map(Map.Entry::getKey)
                .orElse(normalizedHour(properties.getDefaultBestSendHour()));
    }

    /**
     * 规范化输入值。
     *
     * @param hour hour 参数，用于 normalizedHour 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizedHour(int hour) {
        if (hour < 0) {
            return 0;
        }
        if (hour > 23) {
            return 23;
        }
        return hour;
    }
}
