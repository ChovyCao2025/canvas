package org.chovy.canvas.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验模块化重构后的 Maven 模块、包标记和运行时资源边界。
 */
class ModularArchitectureTest {

    /**
     * 新上下文模块必须持续存在的模块清单。
     */
    private static final List<String> CONTEXT_MODULES = List.of(
            "canvas-context-canvas",
            "canvas-context-execution",
            "canvas-context-marketing",
            "canvas-context-cdp",
            "canvas-context-bi",
            "canvas-context-risk",
            "canvas-context-conversation",
            "canvas-platform");

    /**
     * 每个上下文模块应显式声明的 DDD 分层包路径。
     */
    private static final List<String> CONTEXT_PACKAGES = List.of(
            "api",
            "application",
            "domain",
            "adapter/persistence",
            "adapter/messaging",
            "adapter/external",
            "config");

    /**
     * 验证新模块和包级 {@code package-info.java} 标记完整存在。
     */
    @Test
    void dddFoundationModulesExistWithPackageMarkers() {
        Path backendRoot = backendRoot();

        assertThat(backendRoot.resolve("canvas-common/pom.xml")).exists();
        assertThat(backendRoot.resolve("canvas-web/pom.xml")).exists();
        assertThat(backendRoot.resolve("canvas-boot/pom.xml")).exists();

        for (String module : CONTEXT_MODULES) {
            assertThat(backendRoot.resolve(module).resolve("pom.xml"))
                    .as("%s has a module pom", module)
                    .exists();
            for (String packagePath : CONTEXT_PACKAGES) {
                assertThat(backendRoot.resolve(module)
                        .resolve("src/main/java")
                        .resolve(packageRootFor(module))
                        .resolve(packagePath)
                        .resolve("package-info.java"))
                        .as("%s declares %s package marker", module, packagePath)
                        .exists();
            }
        }
    }

    /**
     * 验证新模块不会反向依赖遗留 {@code canvas-engine}。
     *
     * @throws Exception 读取模块 POM 失败时抛出
     */
    @Test
    void newModulePomsDoNotDependOnLegacyEngine() throws Exception {
        Path backendRoot = backendRoot();

        for (String module : allNewModules()) {
            Path pom = backendRoot.resolve(module).resolve("pom.xml");
            assertThat(pom).as("%s has a module pom", module).exists();
            assertThat(Files.readString(pom))
                    .as("%s must not depend on canvas-engine", module)
                    .doesNotContain("<artifactId>canvas-engine</artifactId>");
        }
    }

    /**
     * 验证 Boot 运行时资源在引擎移除前与遗留引擎保持镜像一致。
     *
     * @throws Exception 遍历资源目录失败时抛出
     */
    @Test
    void bootRuntimeResourcesMirrorLegacyEngineUntilEngineRemoval() throws Exception {
        Path backendRoot = backendRoot();
        Path engineResources = backendRoot.resolve("canvas-engine/src/main/resources");
        Path bootResources = backendRoot.resolve("canvas-boot/src/main/resources");

        List<String> engineResourceFiles = listRelativeFiles(engineResources);
        List<String> bootResourceFiles = listRelativeFiles(bootResources);

        assertThat(bootResources.resolve("application.yml")).exists();
        assertThat(bootResources.resolve("application-prod.yml")).exists();
        assertThat(bootResources.resolve("application-staging.yml")).exists();
        assertThat(bootResources.resolve("db/migration"))
                .as("canvas-boot must package Flyway migrations for the cutover runtime")
                .isDirectory();
        assertThat(bootResourceFiles)
                .as("canvas-boot resources must keep the runtime/Flyway surface available while canvas-engine remains the source of truth")
                .containsExactlyElementsOf(engineResourceFiles);
    }

    /**
     * 返回模块化重构后不应依赖遗留引擎的新模块列表。
     *
     * @return 新模块名称列表
     */
    private static List<String> allNewModules() {
        return List.of(
                "canvas-common",
                "canvas-context-canvas",
                "canvas-context-execution",
                "canvas-context-marketing",
                "canvas-context-cdp",
                "canvas-context-bi",
                "canvas-context-risk",
                "canvas-context-conversation",
                "canvas-platform",
                "canvas-web",
                "canvas-boot");
    }

    /**
     * 解析上下文模块对应的 Java 包根目录。
     *
     * @param module Maven 模块名
     * @return 模块内源代码包根路径
     */
    private static String packageRootFor(String module) {
        if ("canvas-platform".equals(module)) {
            return "org/chovy/canvas/platform";
        }
        return "org/chovy/canvas/" + module.substring("canvas-context-".length());
    }

    /**
     * 根据测试启动目录定位后端根目录。
     *
     * @return 后端 Maven 聚合项目根目录
     */
    private static Path backendRoot() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (userDir.endsWith("canvas-boot")) {
            return userDir.getParent();
        }
        if (userDir.endsWith("backend")) {
            return userDir;
        }
        return userDir.resolve("backend");
    }

    /**
     * 列出指定资源目录下的全部相对文件路径。
     *
     * @param root 待遍历的资源目录
     * @return 统一为正斜杠并排序后的相对路径列表
     * @throws Exception 遍历资源目录失败时抛出
     */
    private static List<String> listRelativeFiles(Path root) throws Exception {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }
}
