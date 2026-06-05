package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.dag.DagParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanvasPrePublishCheckServiceTest {

    private final CanvasVersionMapper canvasVersionMapper = mock(CanvasVersionMapper.class);
    private final CanvasPrePublishCheckService service = new CanvasPrePublishCheckService(
            canvasVersionMapper,
            new DagParser(new ObjectMapper()));

    @Test
    void invalidGraphProducesBlockingError() {
        when(canvasVersionMapper.selectOne(any())).thenReturn(version("{"));

        CanvasPrePublishCheckService.Result result = service.check(10L);

        assertThat(result.blocking()).isTrue();
        assertThat(result.items()).anyMatch(item ->
                item.code().equals("GRAPH_JSON_INVALID") && item.severity().equals("ERROR"));
    }

    @Test
    void graphWithoutTriggerEntryNodeProducesBlockingError() {
        when(canvasVersionMapper.selectOne(any())).thenReturn(version("""
                {"nodes":[{"id":"n1","type":"SEND_MESSAGE"}],"edges":[]}
                """));

        CanvasPrePublishCheckService.Result result = service.check(10L);

        assertThat(result.blocking()).isTrue();
        assertThat(result.items()).anyMatch(item ->
                item.code().equals("NO_ENTRY_NODE") && item.severity().equals("ERROR"));
    }

    @Test
    void validEventTriggerGraphIsPublishableWithWarningsOnly() {
        when(canvasVersionMapper.selectOne(any())).thenReturn(version("""
                {"nodes":[{"id":"event","type":"EVENT_TRIGGER","config":{"eventCode":"ORDER_PAID"}}],"edges":[]}
                """));

        CanvasPrePublishCheckService.Result result = service.check(10L);

        assertThat(result.blocking()).isFalse();
        assertThat(result.items()).noneMatch(item -> item.severity().equals("ERROR"));
        assertThat(result.items()).anyMatch(item -> item.code().equals("NO_TEST_SEND"));
    }

    private CanvasVersionDO version(String graphJson) {
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(99L);
        version.setCanvasId(10L);
        version.setVersion(1);
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setGraphJson(graphJson);
        return version;
    }
}
