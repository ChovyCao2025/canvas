package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.MqDefinitionFacade;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionCommand;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionListQuery;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionView;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.PageView;
import org.chovy.canvas.canvas.domain.MqDefinitionCatalog;
import org.junit.jupiter.api.Test;

/**
 * 封装MqDefinitionApplicationServiceTest相关的业务逻辑。
 */
class MqDefinitionApplicationServiceTest {

    /**
     * 列出sEnabledDefinitionsByAscendingIdWithLegacyPageShape。
     */
    @Test
    void listsEnabledDefinitionsByAscendingIdWithLegacyPageShape() {
        MqDefinitionFacade service = new MqDefinitionApplicationService();

        MqDefinitionView first = service.create(command("ORDER_PAID", "order-topic", 1));
        service.create(command("DISABLED", "disabled-topic", 0));
        MqDefinitionView third = service.create(command("USER_SIGNED_UP", "user-topic", 1));

        PageView<MqDefinitionView> page = service.list(new MqDefinitionListQuery(1, 2, 1));

        assertThat(page.total()).isEqualTo(2L);
        assertThat(page.list()).extracting(MqDefinitionView::id).containsExactly(first.id(), third.id());
    }

    /**
     * 创建UpdateAndDeleteRebuildRoutesAndPathIdIsAuthoritative。
     */
    @Test
    void createUpdateAndDeleteRebuildRoutesAndPathIdIsAuthoritative() {
        List<Integer> rebuilds = new ArrayList<>();
        MqDefinitionFacade service = new MqDefinitionApplicationService(new MqDefinitionCatalog(rebuilds::add));

        MqDefinitionView created = service.create(command("ORDER_PAID", "order-topic", 1));
        MqDefinitionView updated = service.update(created.id(), new MqDefinitionCommand(
                "ORDER_COMPLETED",
                "order-topic-v2",
                "paid",
                "order-consumer",
                "{}",
                "updated",
                0,
                "operator-2"));

        assertThat(updated)
                .returns(created.id(), MqDefinitionView::id)
                .returns("ORDER_COMPLETED", MqDefinitionView::messageCode)
                .returns("order-topic-v2", MqDefinitionView::topic)
                .returns(0, MqDefinitionView::enabled);

        service.delete(created.id());

        assertThat(rebuilds).containsExactly(1, 2, 3);
        assertThat(service.list(new MqDefinitionListQuery(1, 20, null)).total()).isZero();
    }

    /**
     * 处理validatesRequiredMessageCodeAndTopicAndMissingDelete。
     */
    @Test
    void validatesRequiredMessageCodeAndTopicAndMissingDelete() {
        MqDefinitionFacade service = new MqDefinitionApplicationService();

        assertThatThrownBy(() -> service.create(command(" ", "topic", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("messageCode is required");
        assertThatThrownBy(() -> service.create(command("ORDER_PAID", " ", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic is required");
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("mq definition not found: 99");
    }

    /**
     * 处理命令。
     */
    private static MqDefinitionCommand command(String messageCode, String topic, Integer enabled) {
        return new MqDefinitionCommand(
                messageCode,
                topic,
                "tag-a",
                "consumer-a",
                "{\"type\":\"object\"}",
                "description",
                enabled,
                "operator-1");
    }
}
