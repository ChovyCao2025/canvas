package org.chovy.canvas.architecture;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

class ModularArchitectureTest {

    private static final List<String> CONTEXT_MODULES = List.of(
            "canvas-context-canvas",
            "canvas-context-execution",
            "canvas-context-marketing",
            "canvas-context-cdp",
            "canvas-context-bi",
            "canvas-context-risk",
            "canvas-context-conversation",
            "canvas-platform");

    private static final List<String> CONTEXT_PACKAGES = List.of(
            "api",
            "application",
            "domain",
            "adapter/persistence",
            "adapter/messaging",
            "adapter/external",
            "config");

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

    @Test
    void bootExecutionMapperResourcesBindFinalExecutionAdapters() throws Exception {
        Path executionMappers = backendRoot().resolve("canvas-boot/src/main/resources/mapper/execution");

        assertMapperResourceTargetsFinalAdapter(executionMappers, "CanvasExecutionMapper.xml");
        assertMapperResourceTargetsFinalAdapter(executionMappers, "CanvasExecutionTraceMapper.xml");
        assertMapperResourceTargetsFinalAdapter(executionMappers, "CanvasExecutionStatsMapper.xml");
        assertMapperResourceTargetsFinalAdapter(executionMappers, "CanvasExecutionDlqMapper.xml");
        assertMapperResourceTargetsFinalAdapter(executionMappers, "CanvasExecutionRequestMapper.xml");
        assertMapperResourceTargetsFinalAdapter(executionMappers, "CanvasMqTriggerRejectedMapper.xml");
        assertMapperResourceTargetsFinalAdapter(executionMappers, "CanvasWaitSubscriptionMapper.xml");

        assertThat(Files.readString(executionMappers.resolve("CanvasExecutionMapper.xml")))
                .contains("resultType=\"org.chovy.canvas.execution.adapter.persistence.CanvasExecutionDO\"")
                .doesNotContain("org.chovy.canvas.dal.dataobject.CanvasExecutionDO");
    }

    @Test
    void bootExecutionMapperXmlParsesWithoutLegacyResultTypes() throws Exception {
        Path executionMappers = backendRoot().resolve("canvas-boot/src/main/resources/mapper/execution");
        Configuration configuration = new Configuration();

        for (String fileName : List.of(
                "CanvasExecutionMapper.xml",
                "CanvasExecutionTraceMapper.xml",
                "CanvasExecutionStatsMapper.xml",
                "CanvasExecutionDlqMapper.xml",
                "CanvasExecutionRequestMapper.xml",
                "CanvasMqTriggerRejectedMapper.xml",
                "CanvasWaitSubscriptionMapper.xml")) {
            Path mapperFile = executionMappers.resolve(fileName);
            try (InputStream input = Files.newInputStream(mapperFile)) {
                new XMLMapperBuilder(input, configuration, mapperFile.toString(), configuration.getSqlFragments()).parse();
            }
        }
    }

    @Test
    void crossContextTriggerServicesUseDistinctSpringBeanNames() {
        Service executionTriggerService = org.chovy.canvas.execution.application.CanvasTriggerApplicationService.class
                .getAnnotation(Service.class);
        Service canvasTriggerService = org.chovy.canvas.canvas.application.CanvasTriggerApplicationService.class
                .getAnnotation(Service.class);

        assertThat(executionTriggerService).isNotNull();
        assertThat(executionTriggerService.value()).isEqualTo("executionCanvasTriggerApplicationService");
        assertThat(canvasTriggerService).isNotNull();
        assertThat(canvasTriggerService.value()).isEmpty();
    }

    private static void assertMapperResourceTargetsFinalAdapter(Path executionMappers, String fileName) throws Exception {
        String mapperName = fileName.replace(".xml", "");
        assertThat(Files.readString(executionMappers.resolve(fileName)))
                .contains("namespace=\"org.chovy.canvas.execution.adapter.persistence." + mapperName + "\"")
                .doesNotContain("namespace=\"org.chovy.canvas.dal.mapper." + mapperName + "\"");
    }

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

    private static String packageRootFor(String module) {
        if ("canvas-platform".equals(module)) {
            return "org/chovy/canvas/platform";
        }
        return "org/chovy/canvas/" + module.substring("canvas-context-".length());
    }

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
