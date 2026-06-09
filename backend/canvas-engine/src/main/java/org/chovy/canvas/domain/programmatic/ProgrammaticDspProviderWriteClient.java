package org.chovy.canvas.domain.programmatic;

/**
 * ProgrammaticDspProviderWriteClient 定义 domain.programmatic 场景中的扩展契约。
 */
public interface ProgrammaticDspProviderWriteClient {

    /**
     * 判断业务条件是否成立。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 supports 的布尔判断结果。
     */
    boolean supports(ProgrammaticDspMutationRequest request);

    /**
     * 执行核心业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    ProgrammaticDspMutationResult execute(ProgrammaticDspMutationRequest request);
}
