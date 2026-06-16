package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MetaOptionFacade;
import org.junit.jupiter.api.Test;

/**
 * 验证MetaOptionApplicationService的关键兼容行为。
 */
class MetaOptionApplicationServiceTest {

    /**
     * 验证 exposes legacy meta dropdown data as tenant scoped options 场景的兼容行为。
     */
    @Test
    void exposesLegacyMetaDropdownDataAsTenantScopedOptions() {
        MetaOptionFacade service = new MetaOptionApplicationService();

        assertThat(service.options(7L, "coupon_type"))
                .extracting("key", "label")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("DISCOUNT", "Discount coupon"),
                        org.assertj.core.groups.Tuple.tuple("CASH", "Cash coupon"));
        assertThat(service.optionsBatch(7L, List.of("coupon_type", "biz_line", "coupon_type")))
                .containsOnlyKeys("coupon_type", "biz_line")
                .satisfies(batch -> assertThat(batch.keySet()).containsExactly("coupon_type", "biz_line"));
        assertThat(service.abExperiments(7L))
                .extracting("key", "label")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("checkout-test", "Checkout test"));
        assertThat(service.abExperimentGroups(7L, "checkout-test"))
                .extracting("key", "label")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("A", "Control"),
                        org.assertj.core.groups.Tuple.tuple("B", "Treatment"));
        assertThat(service.bizLines(7L))
                .extracting("key")
                .containsExactly("retail", "finance");
        assertThat(service.bizLineApis(7L, "retail"))
                .extracting("key")
                .containsExactly("send-coupon", "query-profile");
    }

    /**
     * 验证 handles unknown categories and unknown experiments without breaking config panels 场景的兼容行为。
     */
    @Test
    void handlesUnknownCategoriesAndUnknownExperimentsWithoutBreakingConfigPanels() {
        MetaOptionFacade service = new MetaOptionApplicationService();

        assertThat(service.options(7L, "missing")).isEmpty();
        assertThat(service.optionsBatch(7L, List.of("coupon_type", "coupon_type", "missing")))
                .containsOnlyKeys("coupon_type", "missing");
        assertThat(service.abExperimentGroups(7L, "missing")).isEmpty();
        assertThat(service.bizLineApis(7L, "unknown")).extracting("key")
                .containsExactly("send-coupon", "query-profile");
    }
}
