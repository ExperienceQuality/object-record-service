package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lib.XQOrb;
import lib.XQRequest;
import lib.XQResponse;
import lib.XQTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@XQTest
@Tag("e2e")
class RoutineBusinessE2ETest {
    @Test
    void createsListsAndReadsUserRoutine(XQOrb orb) {
        String user = UUID.randomUUID().toString();
        Map<String, ?> headers = headers(user);
        XQResponse created = createRoutine(orb, headers, "Bench press", 100);
        String routineId = created.raw().jsonPath().getString("id");

        orb.rest().get("/api/v1/routines", XQRequest.withHeaders(headers))
                .shouldHaveStatus(200)
                .shouldMatch("[{\"id\":\"%s\",\"name\":\"Strength A\"}]".formatted(routineId));
        orb.rest().get("/api/v1/routines/" + routineId, XQRequest.withHeaders(headers))
                .shouldHaveStatus(200)
                .shouldMatch("""
                        {
                        "id":"%s",
                        "days":[{"dayNumber":1,"name":"Push","exercises":[
                          {"name":"Bench press","sets":5,"reps":5,"weightKg":100.00,"sortOrder":0}
                        ]}]}
                        """.formatted(routineId));
    }

    @Test
    void isolatesUsersAndReplacesRoutineAggregate(XQOrb orb) {
        String user = UUID.randomUUID().toString();
        Map<String, ?> headers = headers(user);
        String routineId = createRoutine(orb, headers, "Squat", 120)
                .raw().jsonPath().getString("id");

        orb.rest().get("/api/v1/routines/" + routineId,
                        XQRequest.withHeaders(headers(UUID.randomUUID().toString())))
                .shouldHaveStatus(404)
                .shouldMatch("{\"code\":\"ROUTINE_NOT_FOUND\"}");

        orb.rest().put("/api/v1/routines/" + routineId,
                        XQRequest.withBodyAndHeaders(body("Incline press", 80), headers))
                .shouldHaveStatus(200)
                .shouldMatch("""
                        {"id":"%s","days":[{"exercises":[
                          {"name":"Incline press","weightKg":80.00}
                        ]}]}
                        """.formatted(routineId));
    }

    @Test
    void snapshotRemainsStableAfterRoutineChanges(XQOrb orb) {
        String user = UUID.randomUUID().toString();
        Map<String, ?> headers = headers(user);
        String routineId = createRoutine(orb, headers, "Deadlift", 150)
                .raw().jsonPath().getString("id");

        XQResponse snapshot = orb.rest().post("/api/v1/routines/" + routineId + "/snapshots",
                        XQRequest.withHeaders(headers))
                .shouldHaveStatus(201)
                .shouldMatch("{\"exercises\":[{\"name\":\"Deadlift\",\"weightKg\":150.00}]}");
        String snapshotId = snapshot.raw().jsonPath().getString("id");

        orb.rest().put("/api/v1/routines/" + routineId,
                        XQRequest.withBodyAndHeaders(body("Deadlift", 160), headers))
                .shouldHaveStatus(200);

        orb.rest().get("/api/v1/routines/" + routineId + "/snapshots/" + snapshotId,
                        XQRequest.withHeaders(headers))
                .shouldHaveStatus(200)
                .shouldMatch("{\"exercises\":[{\"name\":\"Deadlift\",\"weightKg\":150.00}]}");
        List<String> snapshots = orb.rest().get("/api/v1/routines/" + routineId + "/snapshots",
                        XQRequest.withHeaders(headers))
                .shouldHaveStatus(200).raw().jsonPath().getList("id", String.class);
        assertThat(snapshots).containsExactly(snapshotId);
    }

    private XQResponse createRoutine(XQOrb orb, Map<String, ?> headers, String exercise, int weight) {
        return orb.rest().post("/api/v1/routines", XQRequest.withBodyAndHeaders(body(exercise, weight), headers))
                .shouldHaveStatus(201)
                .shouldMatch("{\"name\":\"Strength A\",\"days\":[{\"dayNumber\":1}]}");
    }

    private Map<String, ?> headers(String user) { return Map.of("X-User-Id", user); }

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
}
