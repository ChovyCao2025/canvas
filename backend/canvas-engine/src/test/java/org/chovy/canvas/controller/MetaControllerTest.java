package org.chovy.canvas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.meta.AbExperiment;
import org.chovy.canvas.domain.meta.AbExperimentGroupService;
import org.chovy.canvas.domain.meta.AbExperimentMapper;
import org.chovy.canvas.domain.meta.ApiDefinitionMapper;
import org.chovy.canvas.domain.meta.EventDefinitionMapper;
import org.chovy.canvas.domain.meta.MetaService;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.chovy.canvas.domain.meta.StubOption;
import org.chovy.canvas.domain.meta.SystemOptionService;
import org.chovy.canvas.domain.meta.TagDefinitionMapper;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetaControllerTest {

    @Test
    void getOptionsReturnsSystemOptions() {
        SystemOptionService options = mock(SystemOptionService.class);
        when(options.activeOptions("http_method"))
                .thenReturn(List.of(new StubOption("POST", "POST")));
        MetaController controller = MetaControllerTestFactory.withSystemOptions(options);

        StepVerifier.create(controller.getOptions("http_method"))
                .assertNext(result -> {
                    assertThat(result.getCode()).isEqualTo(0);
                    assertThat(result.getData()).extracting(StubOption::getKey).containsExactly("POST");
                })
                .verifyComplete();
    }

    @Test
    void getOptionBatchReturnsEachCategory() {
        SystemOptionService options = mock(SystemOptionService.class);
        when(options.activeOptions("http_method"))
                .thenReturn(List.of(new StubOption("POST", "POST")));
        when(options.activeOptions("param_type"))
                .thenReturn(List.of(new StubOption("STRING", "字符型")));
        MetaController controller = MetaControllerTestFactory.withSystemOptions(options);

        StepVerifier.create(controller.getOptionsBatch(List.of("http_method", "param_type", "http_method")))
                .assertNext(result -> {
                    assertThat(result.getData().keySet()).containsExactlyInAnyOrder("http_method", "param_type");
                    assertThat(result.getData().get("param_type")).extracting(StubOption::getKey).containsExactly("STRING");
                })
                .verifyComplete();
    }

    @Test
    void abExperimentGroupsResolveExperimentAndUseGroupService() {
        AbExperimentMapper experimentMapper = mock(AbExperimentMapper.class);
        AbExperiment experiment = new AbExperiment();
        experiment.setId(12L);
        when(experimentMapper.selectOne(any())).thenReturn(experiment);
        AbExperimentGroupService groups = mock(AbExperimentGroupService.class);
        when(groups.activeGroupOptions(12L)).thenReturn(List.of(new StubOption("A", "A组")));
        MetaController controller = MetaControllerTestFactory.create(mock(SystemOptionService.class), experimentMapper, groups);

        StepVerifier.create(controller.getAbExperimentGroups("exp_demo"))
                .assertNext(result -> assertThat(result.getData()).extracting(StubOption::getKey).containsExactly("A"))
                .verifyComplete();
    }

    private static final class MetaControllerTestFactory {
        static MetaController withSystemOptions(SystemOptionService systemOptions) {
            return create(systemOptions, mock(AbExperimentMapper.class), mock(AbExperimentGroupService.class));
        }

        static MetaController create(
                SystemOptionService systemOptions,
                AbExperimentMapper abExperimentMapper,
                AbExperimentGroupService groups) {
            return new MetaController(
                    mock(MetaService.class),
                    mock(ApiDefinitionMapper.class),
                    abExperimentMapper,
                    mock(TagDefinitionMapper.class),
                    mock(MqMessageDefinitionMapper.class),
                    mock(EventDefinitionMapper.class),
                    new ObjectMapper(),
                    systemOptions,
                    groups);
        }
    }
}
