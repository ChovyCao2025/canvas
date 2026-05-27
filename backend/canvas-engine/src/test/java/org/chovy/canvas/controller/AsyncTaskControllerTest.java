package org.chovy.canvas.web;

import io.jsonwebtoken.Claims;
import org.chovy.canvas.dal.dataobject.AsyncTaskDO;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 异步任务 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class AsyncTaskControllerTest {

    @Test
    void list_returnsTasksForCurrentUserWhenNoSecurityContextExists() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        AsyncTaskDO task = new AsyncTaskDO();
        task.setTaskId("task_1");
        task.setTaskType("AUDIENCE_COMPUTE");
        task.setBizType("AUDIENCE");
        task.setBizId("7");
        task.setTitle("计算人群：VIP 人群");
        task.setStatus("RUNNING");
        task.setProgress(5);
        when(service.list("AUDIENCE_COMPUTE", "AUDIENCE", List.of("7"), List.of("RUNNING"), "system", false, 1, 100))
                .thenReturn(List.of(task));

        AsyncTaskController controller = new AsyncTaskController(service);

        var response = controller.list("AUDIENCE_COMPUTE", "AUDIENCE", "7", "RUNNING", 1, 100).block();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().taskId()).isEqualTo("task_1");
    }

    @Test
    void list_allowsAdminToQueryAcrossUsersFromSecurityContext() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        AsyncTaskDO task = new AsyncTaskDO();
        task.setTaskId("task_admin");
        task.setStatus("QUEUED");
        when(service.list(null, null, List.of("7", "8"), List.of("QUEUED"), "alice", true, 1, 100))
                .thenReturn(List.of(task));
        AsyncTaskController controller = new AsyncTaskController(service);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("alice");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        var auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.list(null, null, "7, 8, ", "QUEUED", 1, 100)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().taskId()).isEqualTo("task_admin");
    }

    @Test
    void list_usesOperatorClaimsAsNonAdminCurrentUser() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        when(service.list(null, null, List.of(), List.of(), "bob", false, 1, 100))
                .thenReturn(List.of());
        AsyncTaskController controller = new AsyncTaskController(service);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("bob");
        when(claims.get("role", String.class)).thenReturn("OPERATOR");
        var auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));

        controller.list(null, null, null, null, 1, 100)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        verify(service).list(null, null, List.of(), List.of(), "bob", false, 1, 100);
    }

    @Test
    void list_fallsBackWhenClaimsValuesAreMissing() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        when(service.list(null, null, List.of(), List.of(), "system", false, 1, 100))
                .thenReturn(List.of());
        AsyncTaskController controller = new AsyncTaskController(service);
        Claims claims = mock(Claims.class);
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        controller.list(null, null, null, null, 1, 100)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        verify(service).list(null, null, List.of(), List.of(), "system", false, 1, 100);
    }

    @Test
    void list_capsLargeSizeAtTwoHundred() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        when(service.list(null, null, List.of(), List.of(), "system", false, 1, 200))
                .thenReturn(List.of());
        AsyncTaskController controller = new AsyncTaskController(service);

        controller.list(null, null, null, null, -3, 999).block();

        verify(service).list(null, null, List.of(), List.of(), "system", false, 1, 200);
    }

    @Test
    void get_returnsTaskWhenCurrentUserSubscribed() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        AsyncTaskDO task = new AsyncTaskDO();
        task.setTaskId("task_1");
        task.setTaskType("AUDIENCE_COMPUTE");
        task.setBizType("AUDIENCE");
        task.setBizId("7");
        task.setTitle("计算人群：VIP 人群");
        task.setStatus("RUNNING");
        task.setProgress(5);
        task.setCreatedBy("alice");
        when(service.getByTaskId("task_1")).thenReturn(task);
        when(service.subscribers("task_1")).thenReturn(List.of("bob"));
        AsyncTaskController controller = new AsyncTaskController(service);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("bob");
        when(claims.get("role", String.class)).thenReturn("OPERATOR");
        var auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));

        var response = controller.get("task_1")
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData().taskId()).isEqualTo("task_1");
        assertThat(response.getData().bizId()).isEqualTo("7");
    }
}
