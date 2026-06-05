package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import io.jsonwebtoken.Claims;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionReplayRateLimiter;
import org.chovy.canvas.engine.request.CanvasExecutionRequestStatus;
import org.chovy.canvas.web.CanvasExecutionRequestManagementController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestManagementControllerTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                CanvasExecutionRequestDO.class);
    }

    @Test
    void listForTenantAdminAddsTenantPredicate() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        when(mapper.selectPage(any(), any())).thenReturn(new Page<>());
        CanvasExecutionRequestManagementController controller = controller(mapper, 7L, RoleNames.TENANT_ADMIN);

        controller.list(null, null, null, null, 1, 20).block();

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<LambdaQueryWrapper<CanvasExecutionRequestDO>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("tenant_id");
    }

    @Test
    void listClampsPageSizeToOneHundred() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptor = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptor);
        Page<CanvasExecutionRequestDO> page = new Page<>(1, 100);
        page.setTotal(0);
        when(mapper.selectPage(any(), any())).thenReturn(page);

        controller.list(null, null, null, null, 1, 500).block();

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Page<CanvasExecutionRequestDO>> pageCaptor =
                ArgumentCaptor.forClass(Page.class);
        verify(mapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    void batchReplayCapsLimitAtFiveHundred() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptor = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptor);
        when(mapper.selectList(any())).thenReturn(List.of());
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("operator");

        controller.replayBatch(null, CanvasExecutionRequestStatus.FAILED, null, null, 900, "retry", false)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                        new UsernamePasswordAuthenticationToken(claims, null, List.of())))
                .block();

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<LambdaQueryWrapper<CanvasExecutionRequestDO>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("LIMIT 500");
    }

    @Test
    void replayRejectsCrossTenantRequest() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setId("req-1");
        request.setTenantId(8L);
        request.setStatus(CanvasExecutionRequestStatus.FAILED);
        when(mapper.selectById("req-1")).thenReturn(request);
        CanvasExecutionRequestManagementController controller = controller(mapper, 7L, RoleNames.TENANT_ADMIN);

        assertThatThrownBy(() -> controller.replay("req-1", null, false).block())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("跨租户");
        verify(mapper, never()).markPendingForReplay(
                eq("req-1"), any(LocalDateTime.class), any(String.class), any(String.class));
    }

    private CanvasExecutionRequestManagementController controller(CanvasExecutionRequestMapper mapper,
                                                                  Long tenantId,
                                                                  String role) {
        TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(tenantId, role, "alice")));
        return new CanvasExecutionRequestManagementController(
                mapper,
                mock(CanvasDisruptorService.class),
                new CanvasExecutionReplayRateLimiter(0, 0),
                tenantResolver);
    }
}
