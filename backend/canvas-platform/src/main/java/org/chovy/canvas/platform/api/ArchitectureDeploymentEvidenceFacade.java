package org.chovy.canvas.platform.api;

/**
 * 提供架构部署证据登记和审批的应用入口。
 */
public interface ArchitectureDeploymentEvidenceFacade {

    /**
     * 登记一条新的架构部署证据。
     *
     * @param request 架构部署证据请求
     * @return 登记后的架构部署证据视图
     */
    ArchitectureDeploymentEvidenceView register(ArchitectureDeploymentEvidenceRequest request);

    /**
     * 审批指定候选架构并关联子规格。
     *
     * @param candidateKey 候选架构方案的稳定键
     * @param reviewerId 执行审批的评审人标识
     * @param childSpec 审批后关联的子规格内容或路径
     */
    void approve(String candidateKey, String reviewerId, String childSpec);
}
