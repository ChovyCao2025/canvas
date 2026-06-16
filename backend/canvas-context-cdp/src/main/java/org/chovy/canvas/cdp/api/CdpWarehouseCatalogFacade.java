package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseCatalogFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseCatalogFacade {

    /**
     * status)。
     */
    List<Map<String, Object>> listDatasets(Long tenantId, String layer, String status);

    /**
     * command)。
     */
    Map<String, Object> upsertDataset(Long tenantId, DatasetCommand command);

    /**
     * command)。
     */
    Map<String, Object> createLineageEdge(Long tenantId, LineageCommand command);

    /**
     * direction)。
     */
    Map<String, Object> lineage(Long tenantId, String datasetKey, Direction direction);

    /**
     * max Depth)。
     */
    Map<String, Object> transitiveLineage(Long tenantId, String datasetKey, Direction direction, Integer maxDepth);

    /**
     * 枚举 Direction 支持的取值。
     */
    enum Direction {
        /**
         * UPSTREAM 枚举值。
         */
        UPSTREAM,
        /**
         * DOWNSTREAM 枚举值。
         */
        DOWNSTREAM,
        /**
         * BOTH 枚举值。
         */
        BOTH
    }

    /**
     * 表示 DatasetCommand 的业务数据或处理组件。
     */
    final class DatasetCommand {

        /**
         * dataset Key。
         */
        private final String datasetKey;

        /**
         * layer。
         */
        private final String layer;

        /**
         * physical Name。
         */
        private final String physicalName;

        /**
         * 展示名称。
         */
        private final String displayName;

        /**
         * subject Area。
         */
        private final String subjectArea;

        /**
         * source System。
         */
        private final String sourceSystem;

        /**
         * owner Name。
         */
        private final String ownerName;

        /**
         * 描述。
         */
        private final String description;

        /**
         * freshness Sla Minutes。
         */
        private final Integer freshnessSlaMinutes;

        /**
         * pii Level。
         */
        private final String piiLevel;

        /**
         * 状态。
         */
        private final String status;

        /**
         * schema Json。
         */
        private final String schemaJson;

        /**
         * 使用记录字段创建 DatasetCommand。
         */
        public DatasetCommand(
                String datasetKey,
                String layer,
                String physicalName,
                String displayName,
                String subjectArea,
                String sourceSystem,
                String ownerName,
                String description,
                Integer freshnessSlaMinutes,
                String piiLevel,
                String status,
                String schemaJson) {
            this.datasetKey = datasetKey;
            this.layer = layer;
            this.physicalName = physicalName;
            this.displayName = displayName;
            this.subjectArea = subjectArea;
            this.sourceSystem = sourceSystem;
            this.ownerName = ownerName;
            this.description = description;
            this.freshnessSlaMinutes = freshnessSlaMinutes;
            this.piiLevel = piiLevel;
            this.status = status;
            this.schemaJson = schemaJson;
        }

        /**
         * 返回dataset Key。
         */
        public String datasetKey() {
            return datasetKey;
        }

        /**
         * 返回layer。
         */
        public String layer() {
            return layer;
        }

        /**
         * 返回physical Name。
         */
        public String physicalName() {
            return physicalName;
        }

        /**
         * 返回展示名称。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回subject Area。
         */
        public String subjectArea() {
            return subjectArea;
        }

        /**
         * 返回source System。
         */
        public String sourceSystem() {
            return sourceSystem;
        }

        /**
         * 返回owner Name。
         */
        public String ownerName() {
            return ownerName;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
        }

        /**
         * 返回freshness Sla Minutes。
         */
        public Integer freshnessSlaMinutes() {
            return freshnessSlaMinutes;
        }

        /**
         * 返回pii Level。
         */
        public String piiLevel() {
            return piiLevel;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回schema Json。
         */
        public String schemaJson() {
            return schemaJson;
        }

        /**
         * 按所有字段比较 DatasetCommand。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DatasetCommand that = (DatasetCommand) o;
            return java.util.Objects.equals(datasetKey, that.datasetKey)
                    && java.util.Objects.equals(layer, that.layer)
                    && java.util.Objects.equals(physicalName, that.physicalName)
                    && java.util.Objects.equals(displayName, that.displayName)
                    && java.util.Objects.equals(subjectArea, that.subjectArea)
                    && java.util.Objects.equals(sourceSystem, that.sourceSystem)
                    && java.util.Objects.equals(ownerName, that.ownerName)
                    && java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(freshnessSlaMinutes, that.freshnessSlaMinutes)
                    && java.util.Objects.equals(piiLevel, that.piiLevel)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(schemaJson, that.schemaJson);
        }

        /**
         * 根据所有字段计算 DatasetCommand 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(datasetKey, layer, physicalName, displayName, subjectArea, sourceSystem, ownerName, description, freshnessSlaMinutes, piiLevel, status, schemaJson);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "DatasetCommand[" + "datasetKey=" + datasetKey + ", layer=" + layer + ", physicalName=" + physicalName + ", displayName=" + displayName + ", subjectArea=" + subjectArea + ", sourceSystem=" + sourceSystem + ", ownerName=" + ownerName + ", description=" + description + ", freshnessSlaMinutes=" + freshnessSlaMinutes + ", piiLevel=" + piiLevel + ", status=" + status + ", schemaJson=" + schemaJson + "]";
        }
    }

    /**
     * 表示 LineageCommand 的业务数据或处理组件。
     */
    final class LineageCommand {

        /**
         * upstream Dataset Key。
         */
        private final String upstreamDatasetKey;

        /**
         * downstream Dataset Key。
         */
        private final String downstreamDatasetKey;

        /**
         * transform Type。
         */
        private final String transformType;

        /**
         * transform Ref。
         */
        private final String transformRef;

        /**
         * dependency Type。
         */
        private final String dependencyType;

        /**
         * 描述。
         */
        private final String description;

        /**
         * active。
         */
        private final Boolean active;

        /**
         * 使用记录字段创建 LineageCommand。
         */
        public LineageCommand(
                String upstreamDatasetKey,
                String downstreamDatasetKey,
                String transformType,
                String transformRef,
                String dependencyType,
                String description,
                Boolean active) {
            this.upstreamDatasetKey = upstreamDatasetKey;
            this.downstreamDatasetKey = downstreamDatasetKey;
            this.transformType = transformType;
            this.transformRef = transformRef;
            this.dependencyType = dependencyType;
            this.description = description;
            this.active = active;
        }

        /**
         * 返回upstream Dataset Key。
         */
        public String upstreamDatasetKey() {
            return upstreamDatasetKey;
        }

        /**
         * 返回downstream Dataset Key。
         */
        public String downstreamDatasetKey() {
            return downstreamDatasetKey;
        }

        /**
         * 返回transform Type。
         */
        public String transformType() {
            return transformType;
        }

        /**
         * 返回transform Ref。
         */
        public String transformRef() {
            return transformRef;
        }

        /**
         * 返回dependency Type。
         */
        public String dependencyType() {
            return dependencyType;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
        }

        /**
         * 返回active。
         */
        public Boolean active() {
            return active;
        }

        /**
         * 按所有字段比较 LineageCommand。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LineageCommand that = (LineageCommand) o;
            return java.util.Objects.equals(upstreamDatasetKey, that.upstreamDatasetKey)
                    && java.util.Objects.equals(downstreamDatasetKey, that.downstreamDatasetKey)
                    && java.util.Objects.equals(transformType, that.transformType)
                    && java.util.Objects.equals(transformRef, that.transformRef)
                    && java.util.Objects.equals(dependencyType, that.dependencyType)
                    && java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(active, that.active);
        }

        /**
         * 根据所有字段计算 LineageCommand 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(upstreamDatasetKey, downstreamDatasetKey, transformType, transformRef, dependencyType, description, active);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "LineageCommand[" + "upstreamDatasetKey=" + upstreamDatasetKey + ", downstreamDatasetKey=" + downstreamDatasetKey + ", transformType=" + transformType + ", transformRef=" + transformRef + ", dependencyType=" + dependencyType + ", description=" + description + ", active=" + active + "]";
        }
    }
}
