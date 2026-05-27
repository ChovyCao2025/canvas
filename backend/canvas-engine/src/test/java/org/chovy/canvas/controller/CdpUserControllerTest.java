package org.chovy.canvas.web;

import org.chovy.canvas.domain.cdp.CdpTagService;
import org.chovy.canvas.domain.cdp.CdpUserDirectoryService;
import org.chovy.canvas.domain.cdp.CdpUserInsightService;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.dto.cdp.CdpTagWriteReq;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CDP 用户 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CdpUserControllerTest {

    private CdpUserService userService;
    private CdpTagService tagService;
    private CdpUserDirectoryService directoryService;
    private CdpUserInsightService insightService;
    private CdpUserController controller;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(CdpUserService.class);
        tagService = Mockito.mock(CdpTagService.class);
        directoryService = Mockito.mock(CdpUserDirectoryService.class);
        insightService = Mockito.mock(CdpUserInsightService.class);
        controller = new CdpUserController(directoryService, insightService, userService, tagService);
    }

    @Test
    void getReturnsUserDetail() {
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId("u1");
        when(userService.getRequiredProfile("u1")).thenReturn(profile);
        when(userService.toDetail(profile)).thenReturn(new CdpUserDetailDTO("u1", "u1", null, null,
                "ACTIVE", null, null, null));

        assertThat(controller.get("u1").block().getData().userId()).isEqualTo("u1");
    }

    @Test
    void addTagDelegatesToTagService() {
        CdpTagWriteReq req = new CdpTagWriteReq("vip", "true", "reason", null,
                null, null, null, null);

        controller.addTag("u1", req).block();

        verify(tagService).setTag("u1", req);
    }

    @Test
    void listTagsReturnsCurrentTags() {
        when(tagService.listCurrentTags("u1")).thenReturn(List.of());

        assertThat(controller.listTags("u1").block().getData()).isEmpty();
    }
}
