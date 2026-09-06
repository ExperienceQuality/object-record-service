package com.xq.routineservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;

class OpenApiContractTests {
    private static final String CONTRACT = "static/openapi/routine-service.yaml";

    @Test
    void contractParsesAndCoversAllDomainOperations() throws URISyntaxException {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        String location = getClass().getClassLoader().getResource(CONTRACT).toURI().toString();
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(location, null, options);
        assertThat(result.getMessages()).isEmpty();
        OpenAPI api = result.getOpenAPI();
        assertThat(api.getOpenapi()).isEqualTo("3.1.0");
        assertThat(api.getPaths()).hasSize(4);
        List<Operation> operations = api.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream()).toList();
        assertThat(operations).hasSize(7);
        assertThat(operations).allSatisfy(operation -> {
            assertThat(operation.getOperationId()).isNotBlank();
            assertThat(operation.getResponses()).isNotEmpty();
        });
        Set<String> operationIds = new HashSet<>();
        operations.forEach(operation -> operationIds.add(operation.getOperationId()));
        assertThat(operationIds).hasSize(operations.size());
        assertThat(api.getComponents().getSchemas())
                .containsKeys("Routine", "RoutineSnapshot", "RoutineWriteRequest", "Problem");
    }
}
