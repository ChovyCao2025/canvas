package org.chovy.canvas.domain.monitoring;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SandboxMarketingMonitorPollClient 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class SandboxMarketingMonitorPollClient implements MarketingMonitorPollClient {

    /**
     * 判断监控源是否应走沙箱轮询实现。
     *
     * @param sourceType 监控源类型
     * @return {@code true} 表示源类型为 SANDBOX 或 MOCK，大小写不敏感
     */
    @Override
    public boolean supports(String sourceType) {
        return sourceType != null && ("SANDBOX".equalsIgnoreCase(sourceType) || "MOCK".equalsIgnoreCase(sourceType));
    }

    /**
     * 从请求元数据中构造沙箱监控轮询结果。
     *
     * <p>方法读取 {@code pollItems} 列表并转换为统一监控条目，按请求的 maxItems 截断，
     * 返回模拟游标和 client=sandbox 元数据；不会访问外部平台。</p>
     *
     * @param request 沙箱轮询请求，可在 sourceMetadata.pollItems 中携带模拟条目
     * @return 沙箱轮询响应，包含模拟条目、游标和标识元数据
     */
    @Override
    public MarketingMonitorPollResponse fetch(MarketingMonitorPollRequest request) {
        List<MarketingMonitorPollItem> items = new ArrayList<>();
        Object rawItems = request == null ? null : request.sourceMetadata().get("pollItems");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rawItems instanceof List<?> list) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (Object value : list) {
                if (value instanceof Map<?, ?> map) {
                    items.add(toItem(map));
                }
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorPollResponse(
                items.stream().limit(request == null ? 100 : request.maxItems()).toList(),
                cursor(request, items.size()),
                Map.of("client", "sandbox"));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param map map 参数，用于 toItem 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorPollItem toItem(Map<?, ?> map) {
        return new MarketingMonitorPollItem(
                string(map.get("externalItemId")),
                string(map.get("sourceUrl")),
                string(map.get("authorKey")),
                string(map.get("brandKey")),
                string(map.get("text")),
                string(map.get("language")),
                time(map.get("publishedAt")),
                Map.of("sandbox", true, "raw", map));
    }

    /**
     * 执行 cursor 流程，围绕 cursor 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回 cursor 生成的文本或业务键。
     */
    private String cursor(MarketingMonitorPollRequest request, int size) {
        String base = request == null || request.cursor() == null || request.cursor().isBlank()
                ? "sandbox"
                : request.cursor();
        return base + ":" + size;
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 time 流程，围绕 time 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 time 流程生成的业务结果。
     */
    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof String text && !text.isBlank()) {
            return LocalDateTime.parse(text);
        }
        return null;
    }
}
