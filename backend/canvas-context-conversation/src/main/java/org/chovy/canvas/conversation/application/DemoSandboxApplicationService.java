package org.chovy.canvas.conversation.application;

import java.time.Clock;
import java.util.List;

import org.chovy.canvas.conversation.api.DemoSandboxFacade;
import org.chovy.canvas.conversation.domain.DemoSandboxCatalog;
import org.springframework.stereotype.Service;

/**
 * 将演示沙箱门面调用委托给领域内存目录。
 */
@Service
public class DemoSandboxApplicationService implements DemoSandboxFacade {

    /**
     * 演示沙箱领域目录。
     */
    private final DemoSandboxCatalog catalog;

    /**
     * 使用系统时钟创建演示沙箱应用服务。
     */
    public DemoSandboxApplicationService() {
        this(Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟创建演示沙箱应用服务。
     *
     * @param clock 生成业务时间的时钟
     */
    DemoSandboxApplicationService(Clock clock) {
        this(new DemoSandboxCatalog(clock));
    }

    /**
     * 使用指定领域目录创建演示沙箱应用服务。
     *
     * @param catalog 演示沙箱领域目录
     */
    DemoSandboxApplicationService(DemoSandboxCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 安装或覆盖租户演示沙箱。
     *
     * @param command 安装请求
     * @param actor 操作者
     * @return 安装后的沙箱视图
     */
    @Override
    public SandboxView install(InstallCommand command, String actor) {
        return catalog.install(command, actor);
    }

    /**
     * 重置租户演示沙箱。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 重置结果
     */
    @Override
    public ResetResult reset(Long tenantId, String actor) {
        return catalog.reset(tenantId, actor);
    }

    /**
     * 查询过期演示沙箱。
     *
     * @return 过期沙箱列表
     */
    @Override
    public List<SandboxView> expired() {
        return catalog.expired();
    }

    /**
     * 记录演示沙箱会话回复。
     *
     * @param tenantId 租户标识
     * @param command 回复请求
     * @param actor 操作者
     * @return 回复记录结果
     */
    @Override
    public ConversationReplyResult reply(Long tenantId, ConversationReplyCommand command, String actor) {
        return catalog.reply(tenantId, command, actor);
    }
}
