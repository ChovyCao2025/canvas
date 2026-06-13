package org.chovy.canvas.canvas.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

class UserInputPersistenceMappingTest {

    @Test
    void canvasContextOwnsUserInputFormAndResponseRowsOnly() {
        assertThat(UserInputFormDO.class.getAnnotation(TableName.class).value()).isEqualTo("user_input_form");
        assertThat(UserInputResponseDO.class.getAnnotation(TableName.class).value()).isEqualTo("user_input_response");
    }
}
