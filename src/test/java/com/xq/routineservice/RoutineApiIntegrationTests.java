package com.xq.routineservice;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
class RoutineApiIntegrationTests {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-bookworm"));

    @Autowired ApplicationContext context;
    @Autowired JdbcClient jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
        jdbc.sql("truncate snapshot_exercises, routine_snapshots, routine_exercises, routine_days, routines, users cascade")
                .update();
    }

    @Test
    void createsReplacesAndSnapshotsRoutineWithoutChangingSnapshot() throws Exception {
        UUID user = UUID.randomUUID();
        UUID routine = createRoutine(user, "Bench press", "100.00");

        MvcResult snapshot = mvc.perform(post("/api/v1/routines/{id}/snapshots", routine)
                        .header("X-User-Id", user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].name").value("Bench press"))
                .andReturn();
        String snapshotId = snapshot.getResponse().getHeader("Location");
        snapshotId = snapshotId.substring(snapshotId.lastIndexOf('/') + 1);

        mvc.perform(put("/api/v1/routines/{id}", routine)
                        .header("X-User-Id", user).contentType(APPLICATION_JSON)
                        .content(routineBody("Incline press", "80.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].exercises[0].name").value("Incline press"));

        mvc.perform(get("/api/v1/routines/{routineId}/snapshots/{snapshotId}", routine, snapshotId)
                        .header("X-User-Id", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].name").value("Bench press"))
                .andExpect(jsonPath("$.exercises[0].weightKg").value(100.00));
    }

    @Test
    void scopesRoutineToTrustedUser() throws Exception {
        UUID routine = createRoutine(UUID.randomUUID(), "Squat", "120.00");
        mvc.perform(get("/api/v1/routines/{id}", routine).header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void rejectsMissingInvalidIdentityAndDuplicateDays() throws Exception {
        mvc.perform(get("/api/v1/routines"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/routines").header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER_ID"));
        mvc.perform(post("/api/v1/routines")
                        .header("X-User-Id", UUID.randomUUID()).contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate days","days":[
                                  {"dayNumber":1,"name":"A","exercises":[]},
                                  {"dayNumber":1,"name":"B","exercises":[]}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DAY"));
    }

    private UUID createRoutine(UUID user, String exercise, String weight) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/routines")
                        .header("X-User-Id", user).contentType(APPLICATION_JSON)
                        .content(routineBody(exercise, weight)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.days[0].dayNumber").value(1))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private String routineBody(String exercise, String weight) {
        return """
                {"name":"Strength A","notes":"Monday","days":[
                  {"dayNumber":1,"name":"Push","exercises":[
                    {"name":"%s","sets":5,"reps":5,"weightKg":%s,"sortOrder":0}
                  ]}
                ]}
                """.formatted(exercise, weight);
    }
}
