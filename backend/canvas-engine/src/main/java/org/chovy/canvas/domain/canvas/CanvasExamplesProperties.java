package org.chovy.canvas.domain.canvas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "canvas.examples")
public class CanvasExamplesProperties {

    /**
     * true: import and show official example canvases.
     * false: skip import and hide example canvases from normal list results.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
