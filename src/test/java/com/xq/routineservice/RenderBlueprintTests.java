package com.xq.routineservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@Tag("deployment")
class RenderBlueprintTests {

    private static final Path BLUEPRINT_PATH = Path.of("render.yaml");
    private static final Path PROD_CONFIG_PATH = Path.of("src/main/resources/application-prod.yml");
    private static final Path E2E_CONFIG_PATH = Path.of("src/main/resources/application-e2e.yml");
    private static final Path INTEGRATION_CONFIG_PATH =
            Path.of("src/main/resources/application-integration.yml");

    @Test
    void definesFreeSingaporeServiceUsingPinnedPrivateImage() throws IOException {
        Map<String, Object> blueprint = loadBlueprint();

        assertThat(blueprint.get("databases")).isNull();

        Map<String, Object> service = onlyMap(blueprint, "services");
        assertThat(service)
                .containsEntry("type", "web")
                .containsEntry("name", "routine-service")
                .containsEntry("runtime", "image")
                .containsEntry("region", "singapore")
                .containsEntry("plan", "free")
                .containsEntry("healthCheckPath", "/actuator/health");

        Map<String, Object> image = mapValue(service, "image");
        assertThat(image).containsEntry("url", "ghcr.io/experiencequality/routine-service:0.1.0");
        assertThat(mapValue(mapValue(image, "creds"), "fromRegistryCreds"))
                .containsEntry("name", "xq-ghcr");
    }

    @Test
    void keepsRuntimeConfigurationAndDatabaseCredentialsSafe() throws IOException {
        Map<String, Object> service = onlyMap(loadBlueprint(), "services");
        Map<String, Map<String, Object>> envVars = listOfMaps(service, "envVars").stream()
                .collect(Collectors.toMap(value -> (String) value.get("key"), Function.identity()));

        assertThat(envVars.get("SPRING_DOCKER_COMPOSE_ENABLED"))
                .containsEntry("value", "false")
                .doesNotContainKey("sync");
        assertThat(envVars.get("SPRING_PROFILES_ACTIVE"))
                .containsEntry("value", "prod")
                .doesNotContainKey("sync");

        for (String secret : List.of(
                "SPRING_DATASOURCE_URL",
                "SPRING_DATASOURCE_USERNAME",
                "SPRING_DATASOURCE_PASSWORD")) {
            assertThat(envVars.get(secret))
                    .as(secret)
                    .containsEntry("sync", false)
                    .doesNotContainKey("value");
        }
    }

    @Test
    void productionProfileRequiresExternalDatasourceSecrets() throws IOException {
        Map<String, Object> spring = mapValue(loadYaml(PROD_CONFIG_PATH), "spring");
        Map<String, Object> datasource = mapValue(spring, "datasource");

        assertThat(datasource)
                .containsEntry("url", "${SPRING_DATASOURCE_URL}")
                .containsEntry("username", "${SPRING_DATASOURCE_USERNAME}")
                .containsEntry("password", "${SPRING_DATASOURCE_PASSWORD}");
        assertThat(mapValue(mapValue(spring, "docker"), "compose"))
                .containsEntry("enabled", false);

        String productionConfig = Files.readString(PROD_CONFIG_PATH);
        assertThat(productionConfig)
                .doesNotContain("routine_service_pw")
                .doesNotContain("ci-test-password")
                .doesNotContain("replace-me");
    }

    @Test
    void e2eProfileUsesOnlyLocalFallbacksAndDisablesCompose() throws IOException {
        Map<String, Object> spring = mapValue(loadYaml(E2E_CONFIG_PATH), "spring");
        Map<String, Object> datasource = mapValue(spring, "datasource");

        assertThat(datasource)
                .containsEntry("url", "${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/routine_service}")
                .containsEntry("username", "${SPRING_DATASOURCE_USERNAME:routine_service}")
                .containsEntry("password", "${SPRING_DATASOURCE_PASSWORD:routine_service_pw}");
        assertThat(mapValue(mapValue(spring, "docker"), "compose"))
                .containsEntry("enabled", false);
    }

    @Test
    void integrationProfileLeavesDatasourceOwnershipToTestcontainers() throws IOException {
        Map<String, Object> spring = mapValue(loadYaml(INTEGRATION_CONFIG_PATH), "spring");

        assertThat(spring).doesNotContainKey("datasource");
        assertThat(mapValue(mapValue(spring, "docker"), "compose"))
                .containsEntry("enabled", false);
    }

    private static Map<String, Object> loadBlueprint() throws IOException {
        return loadYaml(BLUEPRINT_PATH);
    }

    private static Map<String, Object> loadYaml(Path path) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        try (InputStream input = Files.newInputStream(path)) {
            return yaml.load(input);
        }
    }

    private static Map<String, Object> onlyMap(Map<String, Object> source, String key) {
        List<Map<String, Object>> values = listOfMaps(source, key);
        assertThat(values).hasSize(1);
        return values.getFirst();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Map<String, Object> source, String key) {
        return (List<Map<String, Object>>) source.get(key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Map<String, Object> source, String key) {
        return (Map<String, Object>) source.get(key);
    }
}
