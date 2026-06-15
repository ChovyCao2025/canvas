package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.EventDefinitionFacade;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionCommand;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionListQuery;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionView;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.PageView;
import org.chovy.canvas.canvas.domain.EventDefinitionCatalog;
import org.junit.jupiter.api.Test;

class EventDefinitionApplicationServiceTest {

    @Test
    void listsByEnabledFilterAndAscendingIdWithPaging() {
        EventDefinitionFacade service = new EventDefinitionApplicationService();

        EventDefinitionView first = service.create(command("signup", "USER_SIGNED_UP", 1));
        service.create(command("disabled", "DISABLED_EVENT", 0));
        EventDefinitionView third = service.create(command("purchase", "ORDER_PAID", 1));

        PageView<EventDefinitionView> page = service.list(new EventDefinitionListQuery(1, 2, 1));

        assertThat(page.total()).isEqualTo(2L);
        assertThat(page.list())
                .extracting(EventDefinitionView::id)
                .containsExactly(first.id(), third.id());
    }

    @Test
    void updateAndDeleteInvalidateOldAndNewEventCodes() {
        List<String> invalidated = new ArrayList<>();
        EventDefinitionFacade service = new EventDefinitionApplicationService(new EventDefinitionCatalog(invalidated::add));
        EventDefinitionView created = service.create(command("purchase", "ORDER_PAID", 1));
        invalidated.clear();

        EventDefinitionView updated = service.update(created.id(), command("purchase-v2", "ORDER_COMPLETED", 1));

        assertThat(updated)
                .returns(created.id(), EventDefinitionView::id)
                .returns("ORDER_COMPLETED", EventDefinitionView::eventCode);
        assertThat(invalidated).containsExactly("ORDER_PAID", "ORDER_COMPLETED");

        invalidated.clear();
        service.delete(created.id());

        assertThat(service.list(new EventDefinitionListQuery(1, 20, null)).total()).isZero();
        assertThat(invalidated).containsExactly("ORDER_COMPLETED");
    }

    private static EventDefinitionCommand command(String name, String eventCode, Integer enabled) {
        return new EventDefinitionCommand(
                name,
                eventCode,
                "[{\"name\":\"userId\",\"type\":\"STRING\"}]",
                name + " event",
                0,
                "REJECT_UNKNOWN",
                enabled,
                "operator-1");
    }
}
