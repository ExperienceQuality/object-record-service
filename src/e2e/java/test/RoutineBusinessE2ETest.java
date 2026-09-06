package test;

import com.xq.jvmtestkit.junit.Xq;
import com.xq.jvmtestkit.junit.XqTest;
import com.xq.jvmtestkit.rest.RestApiConfig;
import com.xq.jvmtestkit.rest.RestRequest;
import com.xq.jvmtestkit.rest.RestResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@XqTest
@Tag("e2e")
class RoutineBusinessE2ETest {
    @BeforeEach
    void configureTestKit() {
        Xq.rest(RestApiConfig.at(URI.create(baseUri())));
    }

    @Test
    void createsListsAndReadsUserRoutine() {
        Map<String, String> headers = headers(UUID.randomUUID().toString());
        RestResponse created = createRoutine(headers, "Bench press", 100);
        String routineId = jsonString(created.bodyUtf8(), "id");

        Xq.rest().get("/api/v1/routines", RestRequest.builder().headers(headers).build())
                .should().status(200)
                .matchJson("[{\"id\":\"%s\",\"name\":\"Strength A\"}]".formatted(routineId));
        Xq.rest().get("/api/v1/routines/" + routineId,
                        RestRequest.builder().headers(headers).build())
                .should().status(200)
                .matchJson("{\"id\":\"%s\",\"days\":[{\"dayNumber\":1,\"name\":\"Push\",\"exercises\":[{\"name\":\"Bench press\",\"sets\":5,\"reps\":5,\"weightKg\":100.00,\"sortOrder\":0}]}]}".formatted(routineId));
    }

    @Test
    void isolatesUsersAndReplacesRoutineAggregate() {
        Map<String, String> headers = headers(UUID.randomUUID().toString());
        String routineId = jsonString(createRoutine(headers, "Squat", 120).bodyUtf8(), "id");

        Xq.rest().get("/api/v1/routines/" + routineId,
                        RestRequest.builder().headers(headers(UUID.randomUUID().toString())).build())
                .should().status(404).matchJson("{\"code\":\"ROUTINE_NOT_FOUND\"}");

        Xq.rest().put("/api/v1/routines/" + routineId,
                        RestRequest.builder().headers(headers)
                                .jsonBody(body("Incline press", 80)).build())
                .should().status(200)
                .matchJson("{\"id\":\"%s\",\"days\":[{\"exercises\":[{\"name\":\"Incline press\",\"weightKg\":80.00}]}]}".formatted(routineId));
    }

    @Test
    void snapshotRemainsStableAfterRoutineChanges() {
        Map<String, String> headers = headers(UUID.randomUUID().toString());
        String routineId = jsonString(createRoutine(headers, "Deadlift", 150).bodyUtf8(), "id");

        RestResponse snapshot = Xq.rest().post("/api/v1/routines/" + routineId + "/snapshots",
                RestRequest.builder().headers(headers).build());
        snapshot.should().status(201)
                .matchJson("{\"exercises\":[{\"name\":\"Deadlift\",\"weightKg\":150.00}]}" );
        String snapshotId = jsonString(snapshot.bodyUtf8(), "id");

        Xq.rest().put("/api/v1/routines/" + routineId,
                        RestRequest.builder().headers(headers)
                                .jsonBody(body("Deadlift", 160)).build())
                .should().status(200);

        Xq.rest().get("/api/v1/routines/" + routineId + "/snapshots/" + snapshotId,
                        RestRequest.builder().headers(headers).build())
                .should().status(200)
                .matchJson("{\"exercises\":[{\"name\":\"Deadlift\",\"weightKg\":150.00}]}" );
        Xq.rest().get("/api/v1/routines/" + routineId + "/snapshots",
                        RestRequest.builder().headers(headers).build())
                .should().status(200)
                .matchJson("[{\"id\":\"%s\"}]".formatted(snapshotId));
    }

    private RestResponse createRoutine(Map<String, String> headers, String exercise, int weight) {
        RestResponse response = Xq.rest().post("/api/v1/routines",
                RestRequest.builder().headers(headers).jsonBody(body(exercise, weight)).build());
        response.should().status(201)
                .matchJson("{\"name\":\"Strength A\",\"days\":[{\"dayNumber\":1}]}" );
        return response;
    }

    private Map<String, String> headers(String user) {
        return Map.of("X-User-Id", user);
    }

    private Map<String, ?> body(String exercise, int weight) {
        return Map.of(
                "name", "Strength A",
                "notes", "Monday",
                "days", List.of(Map.of(
                        "dayNumber", 1,
                        "name", "Push",
                        "exercises", List.of(Map.of(
                                "name", exercise,
                                "sets", 5,
                                "reps", 5,
                                "weightKg", weight,
                                "sortOrder", 0)))));
    }

    private String baseUri() {
        return System.getProperty("xqorb.base-uri",
                System.getenv().getOrDefault("XQORB_BASE_URI", "http://localhost:8080"));
    }

    private String jsonString(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) throw new AssertionError("Missing JSON field: " + field);
        int valueStart = start + marker.length();
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0) throw new AssertionError("Unterminated JSON field: " + field);
        return json.substring(valueStart, valueEnd);
    }
}
