package org.chovy.canvas.cdp.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CdpTagValueTypeTest {

    @Test
    void booleanValuesAreRestrictedAndLowercased() {
        assertThat(CdpTagValueType.from("BOOLEAN").normalize("TRUE")).isEqualTo("true");
        assertThat(CdpTagValueType.from("BOOLEAN").normalize("false")).isEqualTo("false");

        assertThatThrownBy(() -> CdpTagValueType.from("BOOLEAN").normalize("yes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOOLEAN");
    }

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

    @Test
    void unsupportedValueTypeIsRejected() {
        assertThatThrownBy(() -> CdpTagValueType.from("ARRAY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported tag value type");
    }
}
