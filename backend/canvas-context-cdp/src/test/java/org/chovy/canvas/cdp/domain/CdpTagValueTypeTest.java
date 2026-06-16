package org.chovy.canvas.cdp.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 CdpTagValueType 的核心行为。
 */
class CdpTagValueTypeTest {

    /**
     * 执行 booleanValuesAreRestrictedAndLowercased 对应的 CDP 业务操作。
     */
    @Test
    void booleanValuesAreRestrictedAndLowercased() {
        assertThat(CdpTagValueType.from("BOOLEAN").normalize("TRUE")).isEqualTo("true");
        assertThat(CdpTagValueType.from("BOOLEAN").normalize("false")).isEqualTo("false");

        assertThatThrownBy(() -> CdpTagValueType.from("BOOLEAN").normalize("yes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOOLEAN");
    }

    /**
     * 执行 numberValuesMustParseAndStringJsonValuesPassThrough 对应的 CDP 业务操作。
     */
    @Test
    void numberValuesMustParseAndStringJsonValuesPassThrough() {
        assertThat(CdpTagValueType.from("NUMBER").normalize("12.50")).isEqualTo("12.50");
        assertThat(CdpTagValueType.from("STRING").normalize("vip")).isEqualTo("vip");
        assertThat(CdpTagValueType.from("JSON").normalize("{\"tier\":\"vip\"}"))
                .isEqualTo("{\"tier\":\"vip\"}");

        assertThatThrownBy(() -> CdpTagValueType.from("NUMBER").normalize("high"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NUMBER");
    }

    /**
     * 执行 unsupportedValueTypeIsRejected 对应的 CDP 业务操作。
     */
    @Test
    void unsupportedValueTypeIsRejected() {
        assertThatThrownBy(() -> CdpTagValueType.from("ARRAY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported tag value type");
    }
}
