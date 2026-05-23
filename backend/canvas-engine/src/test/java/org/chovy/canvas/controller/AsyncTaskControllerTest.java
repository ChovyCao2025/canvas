package org.chovy.canvas.controller;

import io.jsonwebtoken.Claims;
import org.chovy.canvas.domain.task.AsyncTask;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncTaskControllerTest {

    @Test
    void list_returnsTasksForCurrentUserWhenNoSecurityContextExists() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_1");
        task.setTaskType("AUDIENCE_COMPUTE");
        task.setBizType("AUDIENCE");
        task.setBizId("7");
        task.setTitle("计算人群：VIP 人群");
        task.setStatus("RUNNING");
        task.setProgress(5);
        when(service.list("AUDIENCE_COMPUTE", "AUDIENCE", List.of("7"), List.of("RUNNING"), "system", false))
                .thenReturn(List.of(task));

        AsyncTaskController controller = new AsyncTaskController(service);

        var response = controller.list("AUDIENCE_COMPUTE", "AUDIENCE", "7", "RUNNING").block();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().taskId()).isEqualTo("task_1");
    }

    @Test
    void list_allowsAdminToQueryAcrossUsersFromSecurityContext() {
        AsyncTaskService service = mock(AsyncTaskService.class);
        AsyncTask task = new AsyncTask();
        task.setTaskId("task_admin");
        task.setStatus("QUEUED");
        when(service.list(null, null, List.of("7", "8"), List.of("QUEUED"), "alice", true))
                .thenReturn(List.of(task));
        AsyncTaskController controller = new AsyncTaskController(service);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("alice");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        var auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.list(null, null, "7, 8, ", "QUEUED")
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().taskId()).isEqualTo("task_admin");
    }
}
