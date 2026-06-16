package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.chovy.canvas.canvas.api.ApiDefinitionFacade;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionCommand;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionListQuery;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionView;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.PageView;
import org.junit.jupiter.api.Test;

/**
 * 封装ApiDefinitionApplicationServiceTest相关的业务逻辑。
 */
class ApiDefinitionApplicationServiceTest {

    /**
     * 创建sWithLegacyDefaultsAndListsByEnabledDescendingIdWithPaging。
     */
    @Test
    void createsWithLegacyDefaultsAndListsByEnabledDescendingIdWithPaging() {
        ApiDefinitionFacade service = new ApiDefinitionApplicationService();

        ApiDefinitionView disabled = service.create(command("disabled-api", "https://example.com/disabled", 0, null, false));
        ApiDefinitionView enabledOne = service.create(command("enabled-api-1", "https://example.com/one", null, null, false));
        ApiDefinitionView enabledTwo = service.create(command("enabled-api-2", "https://example.com/two", 1, 5, true));

        assertThat(disabled.id()).isEqualTo(1L);
        assertThat(enabledOne)
                .returns(1, ApiDefinitionView::enabled)
                .returns(0, ApiDefinitionView::includeContextPayload)
                .returns(0, ApiDefinitionView::receiptEnabled)
                .returns(1440, ApiDefinitionView::receiptExpireMinutes)
                .returns("[]", ApiDefinitionView::receiptStatuses);
        assertThat(enabledTwo.rateLimitPerSec()).isEqualTo(5);

        PageView<ApiDefinitionView> page = service.list(new ApiDefinitionListQuery(1, 1, 1));

        assertThat(page.total()).isEqualTo(2L);
        assertThat(page.records()).singleElement()
                .returns(enabledTwo.id(), ApiDefinitionView::id)
                .returns("enabled-api-2", ApiDefinitionView::apiKey);
    }

    /**
     * 更新DistinguishesOmittedAndExplicitNullRateLimitThenDeleteRemovesRow。
     */
    @Test
    void updateDistinguishesOmittedAndExplicitNullRateLimitThenDeleteRemovesRow() {
        ApiDefinitionFacade service = new ApiDefinitionApplicationService();
        ApiDefinitionView created = service.create(command("orders-api", "https://example.com/orders", null, 10, true));

        ApiDefinitionView kept = service.update(created.id(), new ApiDefinitionCommand(
                null,
                "https://example.com/orders-v2",
                null,
                null,
                null,
                null,
                null,
                null,
                false));

        assertThat(kept)
                .returns(created.id(), ApiDefinitionView::id)
                .returns("orders-api", ApiDefinitionView::apiKey)
                .returns("https://example.com/orders-v2", ApiDefinitionView::url)
                .returns(10, ApiDefinitionView::rateLimitPerSec);

        ApiDefinitionView cleared = service.update(created.id(), new ApiDefinitionCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true));

        assertThat(cleared.rateLimitPerSec()).isNull();

        service.delete(created.id());
        assertThat(service.list(new ApiDefinitionListQuery(1, 20, null)).total()).isZero();
    }

    /**
     * 处理validatesRequiredSafeUrlAndPositiveRateLimit。
     */
    @Test
    void validatesRequiredSafeUrlAndPositiveRateLimit() {
        ApiDefinitionFacade service = new ApiDefinitionApplicationService();

        assertThatThrownBy(() -> service.create(command("blank-url", " ", null, null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
        assertThatThrownBy(() -> service.create(command("local-url", "http://localhost:8080/hook", null, null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
        assertThatThrownBy(() -> service.create(command("bad-rate-limit", "https://example.com/hook", null, 0, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rateLimitPerSec");
    }

    /**
     * 创建测试使用的 API 定义命令。
     */
    private static ApiDefinitionCommand command(String apiKey,
                                                String url,
                                                Integer enabled,
                                                Integer rateLimitPerSec,
                                                boolean rateLimitPerSecPresent) {
        return new ApiDefinitionCommand(
                apiKey,
                url,
                enabled,
                null,
                null,
                null,
                null,
                rateLimitPerSec,
                rateLimitPerSecPresent);
    }
}
