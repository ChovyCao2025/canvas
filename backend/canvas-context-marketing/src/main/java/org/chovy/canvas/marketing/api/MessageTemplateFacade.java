package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 定义MessageTemplateFacade的营销上下文访问契约。
 */
public interface MessageTemplateFacade {

    /**
     * 执行search业务操作。
     */
    List<TemplateView> search(Long tenantId, String keyword, String channel);

    /**
     * 创建业务对象。
     */
    TemplateView create(Long tenantId, String actor, TemplateDraft draft);

    /**
     * 执行preview业务操作。
     */
    PreviewView preview(Long tenantId, String templateCode, Map<String, Object> context);

    /**
     * 表示TemplateDraft的数据结构。
     */
    static final class TemplateDraft {

        /**
         * templateCode 字段值。
         */
        private final String templateCode;

        /**
         * displayName 字段值。
         */
        private final String displayName;

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * body 字段值。
         */
        private final String body;

        /**
         * 创建TemplateDraft实例。
         */
        public TemplateDraft(String templateCode, String displayName, String channel, String body) {
            this.templateCode = templateCode;
            this.displayName = displayName;
            this.channel = channel;
            this.body = body;
        }

        /**
         * 返回templateCode 字段值。
         */
        public String templateCode() {
            return templateCode;
        }

        /**
         * 返回displayName 字段值。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回body 字段值。
         */
        public String body() {
            return body;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TemplateDraft that = (TemplateDraft) o;
            return                     Objects.equals(templateCode, that.templateCode) &&
                    Objects.equals(displayName, that.displayName) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(body, that.body);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(templateCode, displayName, channel, body);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "TemplateDraft[templateCode=" + templateCode + ", displayName=" + displayName + ", channel=" + channel + ", body=" + body + "]";
        }
    }

    /**
     * 承载TemplateView返回给调用方的只读视图。
     */
    static final class TemplateView {

        /**
         * 所属租户标识。
         */
        private final Long tenantId;

        /**
         * templateCode 字段值。
         */
        private final String templateCode;

        /**
         * displayName 字段值。
         */
        private final String displayName;

        /**
         * 渠道标识。
         */
        private final String channel;

        /**
         * body 字段值。
         */
        private final String body;

        /**
         * variables 字段值。
         */
        private final List<String> variables;

        /**
         * 当前业务状态。
         */
        private final String status;

        /**
         * 创建人标识。
         */
        private final String createdBy;

        /**
         * 创建TemplateView实例。
         */
        public TemplateView(Long tenantId, String templateCode, String displayName, String channel, String body, List<String> variables, String status, String createdBy) {
            this.tenantId = tenantId;
            this.templateCode = templateCode;
            this.displayName = displayName;
            this.channel = channel;
            this.body = body;
            this.variables = variables;
            this.status = status;
            this.createdBy = createdBy;
        }

        /**
         * 返回所属租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回templateCode 字段值。
         */
        public String templateCode() {
            return templateCode;
        }

        /**
         * 返回displayName 字段值。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回渠道标识。
         */
        public String channel() {
            return channel;
        }

        /**
         * 返回body 字段值。
         */
        public String body() {
            return body;
        }

        /**
         * 返回variables 字段值。
         */
        public List<String> variables() {
            return variables;
        }

        /**
         * 返回当前业务状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回创建人标识。
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TemplateView that = (TemplateView) o;
            return                     Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(templateCode, that.templateCode) &&
                    Objects.equals(displayName, that.displayName) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(body, that.body) &&
                    Objects.equals(variables, that.variables) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(createdBy, that.createdBy);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, templateCode, displayName, channel, body, variables, status, createdBy);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "TemplateView[tenantId=" + tenantId + ", templateCode=" + templateCode + ", displayName=" + displayName + ", channel=" + channel + ", body=" + body + ", variables=" + variables + ", status=" + status + ", createdBy=" + createdBy + "]";
        }
    }

    /**
     * 承载PreviewView返回给调用方的只读视图。
     */
    static final class PreviewView {

        /**
         * renderedBody 字段值。
         */
        private final String renderedBody;

        /**
         * missingVariables 字段值。
         */
        private final List<String> missingVariables;

        /**
         * 创建PreviewView实例。
         */
        public PreviewView(String renderedBody, List<String> missingVariables) {
            this.renderedBody = renderedBody;
            this.missingVariables = missingVariables;
        }

        /**
         * 返回renderedBody 字段值。
         */
        public String renderedBody() {
            return renderedBody;
        }

        /**
         * 返回missingVariables 字段值。
         */
        public List<String> missingVariables() {
            return missingVariables;
        }

        /**
         * 比较两个实例的组件值是否一致。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PreviewView that = (PreviewView) o;
            return                     Objects.equals(renderedBody, that.renderedBody) &&
                    Objects.equals(missingVariables, that.missingVariables);
        }

        /**
         * 根据组件值计算哈希值。
         */
        @Override
        public int hashCode() {
            return Objects.hash(renderedBody, missingVariables);
        }

        /**
         * 返回与记录类型一致的组件展示文本。
         */
        @Override
        public String toString() {
            return "PreviewView[renderedBody=" + renderedBody + ", missingVariables=" + missingVariables + "]";
        }
    }
}
