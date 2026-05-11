package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetaService {

    private final NodeTypeRegistryMapper nodeTypeRegistryMapper;
    private final ContextFieldMapper contextFieldMapper;

    public List<NodeTypeRegistry> getAllNodeTypes() {
        return nodeTypeRegistryMapper.selectList(
                new LambdaQueryWrapper<NodeTypeRegistry>()
                        .eq(NodeTypeRegistry::getEnabled, 1)
                        .orderByAsc(NodeTypeRegistry::getCategory)
        );
    }

    public NodeTypeRegistry getNodeTypeSchema(String typeKey) {
        return nodeTypeRegistryMapper.selectById(typeKey);
    }

    public List<ContextField> getAllContextFields() {
        return contextFieldMapper.selectList(null);
    }

    // ── Stub 元数据（Phase 6 对接真实外部系统）───────────────────

    public List<StubOption> getMqTopics() {
        return List.of(
                new StubOption("flight_order_status_change", "机票订单状态变化"),
                new StubOption("hotel_order_status_change", "酒店订单状态变化"),
                new StubOption("train_order_status_change", "火车票订单状态变化")
        );
    }

    public List<StubOption> getCouponTypes() {
        return List.of(
                new StubOption("flight_coupon", "机票代金券"),
                new StubOption("hotel_coupon", "酒店代金券"),
                new StubOption("train_coupon", "火车票代金券")
        );
    }

    public List<StubOption> getReachScenes() {
        return List.of(
                new StubOption("quick_booking_push", "急速预订Push"),
                new StubOption("hotel_recommend_push", "酒店推荐Push"),
                new StubOption("coupon_reminder_sms", "领券提醒短信")
        );
    }

    public List<StubOption> getAbExperiments() {
        return List.of(
                new StubOption("exp_new_user_coupon", "新用户发券实验"),
                new StubOption("exp_hotel_recommend", "酒店推荐实验")
        );
    }

    public List<StubOption> getAbExperimentGroups(String experimentKey) {
        return List.of(
                new StubOption("A", "A组"),
                new StubOption("B", "B组")
        );
    }

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

    public List<StubOption> getBizLines() {
        return List.of(
                new StubOption("FLIGHT", "机票"),
                new StubOption("HOTEL", "酒店"),
                new StubOption("TRAIN_TICKET", "火车票")
        );
    }

    public List<StubOption> getBizLineApis(String bizLineKey) {
        return List.of(
                new StubOption("check_good_seat", "查询好坐席"),
                new StubOption("query_user_info", "查询用户信息"),
                new StubOption("query_order_detail", "查询订单详情")
        );
    }

    public List<StubOption> getBehaviorStrategyTypes() {
        return List.of(
                new StubOption("BROWSE_DURATION", "浏览时长"),
                new StubOption("BROWSE_COUNT", "浏览次数"),
                new StubOption("CLICK_COUNT", "点击次数")
        );
    }

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
