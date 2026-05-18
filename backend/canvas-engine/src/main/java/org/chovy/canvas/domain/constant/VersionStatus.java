package org.chovy.canvas.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** canvas_version.status */
@Getter
@AllArgsConstructor
public enum VersionStatus {

    DRAFT(0),
    PUBLISHED(1);

    private final int code;
}
