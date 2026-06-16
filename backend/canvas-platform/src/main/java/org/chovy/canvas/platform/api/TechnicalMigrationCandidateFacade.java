package org.chovy.canvas.platform.api;

/**
 * 提供技术迁移候选项证据登记和准入判断的应用入口。
 */
public interface TechnicalMigrationCandidateFacade {

    /**
     * 为指定操作者登记技术迁移证据。
     *
     * @param actor 提交证据的操作者
     * @param request 技术迁移证据请求
     * @return 登记后的技术迁移证据视图
     */
    TechnicalMigrationEvidenceView register(PlatformActor actor, TechnicalMigrationEvidenceRequest request);

    /**
     * 判断指定候选项是否可以开始迁移。
     *
     * @param actor 发起判断的操作者
     * @param candidateKey 迁移候选项的稳定键
     * @return 可以开始迁移时返回 true
     */
    boolean canStartMigration(PlatformActor actor, String candidateKey);
}
