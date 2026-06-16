package org.chovy.canvas.platform.domain;

/**
 * 持久化架构部署证据和评审结果的仓储接口。
 */
public interface ArchitectureDeploymentEvidenceRepository {

    /**
     * 新增一条架构部署证据记录。
     *
     * @param record 待持久化的架构部署证据
     */
    void insert(ArchitectureDeploymentEvidence record);

    /**
     * 将指定候选架构标记为已审批，并记录评审人与子规格。
     *
     * @param candidateKey 候选架构方案的稳定键
     * @param reviewerId 执行审批的评审人标识
     * @param childSpec 审批后关联的子规格内容或路径
     */
    void approve(String candidateKey, String reviewerId, String childSpec);
}
