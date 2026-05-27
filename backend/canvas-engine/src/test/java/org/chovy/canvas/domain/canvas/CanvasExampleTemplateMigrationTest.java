package org.chovy.canvas.domain.canvas;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Canvas Example Template Migration 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasExampleTemplateMigrationTest {

    @Test
    void templateMigrationContainsAllOfficialTemplateKeys() throws Exception {
        String sql = new ClassPathResource("db/migration/V55__canvas_example_templates.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("INSERT INTO canvas_template");
        assertThat(sql.split("template_key = VALUES\\(template_key\\)", -1)).hasSize(2);

        String[] keys = {
                "component_event_if_coupon", "component_mq_validate_route",
                "component_scheduled_audience_push", "component_direct_call_return",
                "component_selector_multi_branch", "component_priority_offer",
                "component_ab_split_compare", "component_hub_wait_all",
                "component_aggregate_kpi", "component_threshold_fast_win",
                "component_logic_relation", "component_manual_approval",
                "component_delay_followup", "component_groovy_transform",
                "component_tagger_offline", "component_tagger_realtime",
                "component_sub_flow_ref", "component_send_mq_receipt",
                "ecommerce_new_user_coupon", "ecommerce_cart_recall",
                "ecommerce_vip_tier_offer", "ecommerce_cross_sell",
                "travel_flight_delay_care", "travel_hotel_bundle",
                "travel_high_value_route", "travel_pre_departure_reminder",
                "fintech_card_activation", "fintech_risk_review",
                "fintech_loan_repay_reminder", "fintech_wealth_cross_sell",
                "saas_trial_nurture", "saas_onboarding_steps",
                "saas_churn_risk_save", "saas_expansion_signal",
                "local_food_coupon", "local_service_reactivation",
                "local_weather_push", "retail_store_lbs",
                "retail_inventory_clearance", "retail_member_anniversary",
                "content_subscription_trial", "content_inactive_reader",
                "gaming_level_reward", "gaming_lost_user_winback",
                "education_course_followup", "education_learning_reminder",
                "b2b_lead_scoring", "logistics_delivery_care"
        };

        for (String key : keys) {
            assertThat(sql).contains("'" + key + "'");
        }
    }
}
