package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void genericExceptionMessageDoesNotExposeInternalDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleGeneral(new RuntimeException("db password leaked"));

        assertThat(response.getMessage()).isEqualTo("系统错误，请联系管理员");
        assertThat(response.getMessage()).doesNotContain("db password leaked");
    }
}
