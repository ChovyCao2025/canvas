package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 元数据读取服务。
 *
 * 职责边界：
 * 1) 提供节点类型、上下文字段等配置元数据查询；
 * 2) 提供当前阶段 stub 选项数据；
 * 3) 不负责执行引擎运行时决策。
 */
@Service
@RequiredArgsConstructor
public class MetaService {

    /** 节点类型注册表 Mapper。 */
    private final NodeTypeRegistryMapper nodeTypeRegistryMapper;

    /** 上下文字段定义 Mapper。 */
    private final ContextFieldMapper contextFieldMapper;

    /** 查询全部启用节点类型（按分类排序）。 */
    public List<NodeTypeRegistry> getAllNodeTypes() {
        return nodeTypeRegistryMapper.selectList(
                new LambdaQueryWrapper<NodeTypeRegistry>()
                        .eq(NodeTypeRegistry::getEnabled, 1)
                        .orderByAsc(NodeTypeRegistry::getCategory)
        );
    }

    /** 查询指定节点类型的 schema 配置。 */
    public NodeTypeRegistry getNodeTypeSchema(String typeKey) {
        return nodeTypeRegistryMapper.selectById(typeKey);
    }

    /** 查询全部上下文字段定义。 */
    public List<ContextField> getAllContextFields() {
        return contextFieldMapper.selectList(null);
    }

    // ── Stub 元数据（Phase 6 对接真实外部系统）───────────────────
    // 说明：
    // - 这些数据是“前端配置可选项”的临时实现；
    // - 接入真实平台后可迁移为 DB/API 数据源，调用方无需改协议。

    /** MQ 主题选项（本地 stub 数据）。 */
    public List<StubOption> getMqTopics() {
        return List.of(
                new StubOption("flight_order_status_change", "机票订单状态变化"),
                new StubOption("hotel_order_status_change", "酒店订单状态变化"),
                new StubOption("train_order_status_change", "火车票订单状态变化")
        );
    }

    /** 券类型选项（本地 stub 数据）。 */
    public List<StubOption> getCouponTypes() {
        return List.of(
                new StubOption("flight_coupon", "机票代金券"),
                new StubOption("hotel_coupon", "酒店代金券"),
                new StubOption("train_coupon", "火车票代金券")
        );
    }

    /** 触达场景选项（本地 stub 数据）。 */
    public List<StubOption> getReachScenes() {
        return List.of(
                new StubOption("quick_booking_push", "急速预订Push"),
                new StubOption("hotel_recommend_push", "酒店推荐Push"),
                new StubOption("coupon_reminder_sms", "领券提醒短信")
        );
    }

    /** AB 实验选项（本地 stub 数据）。 */
    public List<StubOption> getAbExperiments() {
        return List.of(
                new StubOption("exp_new_user_coupon", "新用户发券实验"),
                new StubOption("exp_hotel_recommend", "酒店推荐实验")
        );
    }

    /** AB 实验分组选项（本地 stub 数据）。 */
    public List<StubOption> getAbExperimentGroups(String experimentKey) {
        // 当前按固定 A/B 返回；未来可按 experimentKey 动态查询实验平台
        return List.of(
                new StubOption("A", "A组"),
                new StubOption("B", "B组")
        );
    }

    /** Tagger 标签选项（离线/实时）。 */
    public List<StubOption> getTaggerTags(String type) {
        if ("offline".equals(type)) {
            return List.of(
                    new StubOption("tag_offline_high_value", "高价值用户"),
                    new StubOption("tag_offline_vip", "VIP用户")
            );
        }
        return List.of(
                new StubOption("tag_vip_user", "VIP实时标签"),
                new StubOption("tag_new_user", "新用户实时标签")
        );
    }

    /** 业务线选项。 */
    public List<StubOption> getBizLines() {
        return List.of(
                new StubOption("FLIGHT", "机票"),
                new StubOption("HOTEL", "酒店"),
                new StubOption("TRAIN_TICKET", "火车票")
        );
    }

    /** 业务线对应 API 选项。 */
    public List<StubOption> getBizLineApis(String bizLineKey) {
        // 当前未按 bizLineKey 细分返回，后续接真实 API 目录后再细化
        return List.of(
                new StubOption("check_good_seat", "查询好坐席"),
                new StubOption("query_user_info", "查询用户信息"),
                new StubOption("query_order_detail", "查询订单详情")
        );
    }

    /** 行为策略类型选项。 */
    public List<StubOption> getBehaviorStrategyTypes() {
        return List.of(
                new StubOption("BROWSE_DURATION", "浏览时长"),
                new StubOption("BROWSE_COUNT", "浏览次数"),
                new StubOption("CLICK_COUNT", "点击次数")
        );
    }

    /** 消息编码选项（MQ / Reach）。 */
    public List<StubOption> getMessageCodes(String type) {
        if ("MQ".equals(type)) {
            return List.of(
                    new StubOption("ivr_project", "IVR项目消息"),
                    new StubOption("reward_notify", "奖励通知消息")
            );
        }
        return List.of(
                new StubOption("international_hotel_coupon_popup", "国际酒店领券弹窗"),
                new StubOption("flight_coupon_banner", "机票优惠Banner")
        );
    }
}
