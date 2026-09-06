package com.xq.routineservice.routine;

import static com.xq.routineservice.routine.RoutineApi.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.xq.routineservice.shared.ApiException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineService {
    private final JdbcClient jdbc;

    public RoutineService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional
    public RoutineResponse create(UUID userId, RoutineWriteRequest request) {
        validateDays(request.days());
        ensureUser(userId);
        UUID routineId = UUID.randomUUID();
        jdbc.sql("""
                insert into routines(id, user_id, name, notes)
                values (:id, :userId, :name, :notes)
                """)
                .param("id", routineId).param("userId", userId)
                .param("name", request.name().trim()).param("notes", request.notes())
                .update();
        writeDays(routineId, request.days());
        return get(userId, routineId);
    }

    @Transactional(readOnly = true)
    public List<RoutineSummaryResponse> list(UUID userId) {
        return jdbc.sql("""
                select id, name, notes, created_at, updated_at
                from routines where user_id = :userId
                order by updated_at desc, id desc
                """)
                .param("userId", userId)
                .query((rs, row) -> new RoutineSummaryResponse(
                        rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("notes"),
                        instant(rs, "created_at"), instant(rs, "updated_at")))
                .list();
    }

    @Transactional(readOnly = true)
    public RoutineResponse get(UUID userId, UUID routineId) {
        RoutineRow routine = requireRoutine(userId, routineId);
        List<RoutineDayResponse> days = jdbc.sql("""
                select id, day_number, name from routine_days
                where routine_id = :routineId order by day_number
                """)
                .param("routineId", routineId)
                .query((rs, row) -> new RoutineDayResponse(
                        rs.getObject("id", UUID.class), rs.getInt("day_number"), rs.getString("name"),
                        exercises(rs.getObject("id", UUID.class))))
                .list();
        return new RoutineResponse(routine.id(), routine.name(), routine.notes(),
                routine.createdAt(), routine.updatedAt(), days);
    }

    @Transactional
    public RoutineResponse replace(UUID userId, UUID routineId, RoutineWriteRequest request) {
        validateDays(request.days());
        requireRoutine(userId, routineId);
        jdbc.sql("""
                update routines set name = :name, notes = :notes, updated_at = now()
                where id = :id and user_id = :userId
                """)
                .param("name", request.name().trim()).param("notes", request.notes())
                .param("id", routineId).param("userId", userId).update();
        jdbc.sql("delete from routine_days where routine_id = :routineId")
                .param("routineId", routineId).update();
        writeDays(routineId, request.days());
        return get(userId, routineId);
    }

    @Transactional
    public RoutineSnapshotResponse createSnapshot(UUID userId, UUID routineId) {
        requireRoutine(userId, routineId);
        UUID snapshotId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        jdbc.sql("insert into routine_snapshots(id, routine_id, created_at) values (:id, :routineId, :createdAt)")
                .param("id", snapshotId).param("routineId", routineId)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC)).update();
        jdbc.sql("""
                insert into snapshot_exercises(id, snapshot_id, exercise_key, name, sets, reps, weight_kg)
                select gen_random_uuid(), :snapshotId, exercise.id::text,
                       exercise.name, exercise.sets, exercise.reps, exercise.weight_kg
                from routine_exercises exercise
                join routine_days day on day.id = exercise.routine_day_id
                where day.routine_id = :routineId
                """)
                .param("snapshotId", snapshotId).param("routineId", routineId).update();
        return getSnapshot(userId, routineId, snapshotId);
    }

    @Transactional(readOnly = true)
    public List<RoutineSnapshotSummaryResponse> listSnapshots(UUID userId, UUID routineId) {
        requireRoutine(userId, routineId);
        return jdbc.sql("""
                select snapshot.id, snapshot.routine_id, snapshot.created_at
                from routine_snapshots snapshot
                where snapshot.routine_id = :routineId
                order by snapshot.created_at desc, snapshot.id desc
                """)
                .param("routineId", routineId)
                .query((rs, row) -> new RoutineSnapshotSummaryResponse(
                        rs.getObject("id", UUID.class), rs.getObject("routine_id", UUID.class),
                        instant(rs, "created_at")))
                .list();
    }

    @Transactional(readOnly = true)
    public RoutineSnapshotResponse getSnapshot(UUID userId, UUID routineId, UUID snapshotId) {
        requireRoutine(userId, routineId);
        SnapshotRow snapshot = jdbc.sql("""
                select id, routine_id, created_at from routine_snapshots
                where id = :snapshotId and routine_id = :routineId
                """)
                .param("snapshotId", snapshotId).param("routineId", routineId)
                .query((rs, row) -> new SnapshotRow(
                        rs.getObject("id", UUID.class), rs.getObject("routine_id", UUID.class),
                        instant(rs, "created_at")))
                .optional()
                .orElseThrow(() -> ApiException.notFound("SNAPSHOT_NOT_FOUND", "Routine snapshot was not found"));
        List<SnapshotExerciseResponse> exercises = jdbc.sql("""
                select id, exercise_key, name, sets, reps, weight_kg
                from snapshot_exercises where snapshot_id = :snapshotId
                order by exercise_key
                """)
                .param("snapshotId", snapshotId)
                .query((rs, row) -> new SnapshotExerciseResponse(
                        rs.getObject("id", UUID.class), rs.getString("exercise_key"), rs.getString("name"),
                        rs.getInt("sets"), rs.getInt("reps"), rs.getBigDecimal("weight_kg")))
                .list();
        return new RoutineSnapshotResponse(snapshot.id(), snapshot.routineId(), snapshot.createdAt(), exercises);
    }

    private void ensureUser(UUID userId) {
        jdbc.sql("insert into users(id) values (:id) on conflict (id) do nothing")
                .param("id", userId).update();
    }

    private RoutineRow requireRoutine(UUID userId, UUID routineId) {
        return jdbc.sql("""
                select id, name, notes, created_at, updated_at from routines
                where id = :id and user_id = :userId
                """)
                .param("id", routineId).param("userId", userId)
                .query((rs, row) -> new RoutineRow(
                        rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("notes"),
                        instant(rs, "created_at"), instant(rs, "updated_at")))
                .optional()
                .orElseThrow(() -> ApiException.notFound("ROUTINE_NOT_FOUND", "Routine was not found"));
    }

    private void writeDays(UUID routineId, List<RoutineDayWriteRequest> days) {
        for (RoutineDayWriteRequest day : days) {
            UUID dayId = UUID.randomUUID();
            jdbc.sql("insert into routine_days(id, routine_id, day_number, name) values (:id, :routineId, :dayNumber, :name)")
                    .param("id", dayId).param("routineId", routineId)
                    .param("dayNumber", day.dayNumber()).param("name", day.name().trim()).update();
            for (RoutineExerciseWriteRequest exercise : day.exercises()) {
                jdbc.sql("""
                        insert into routine_exercises(
                            id, routine_day_id, name, sets, reps, weight_kg, sort_order)
                        values (:id, :dayId, :name, :sets, :reps, :weightKg, :sortOrder)
                        """)
                        .param("id", UUID.randomUUID()).param("dayId", dayId)
                        .param("name", exercise.name().trim()).param("sets", exercise.sets())
                        .param("reps", exercise.reps()).param("weightKg", exercise.weightKg())
                        .param("sortOrder", exercise.sortOrder()).update();
            }
        }
    }

    private List<RoutineExerciseResponse> exercises(UUID dayId) {
        return jdbc.sql("""
                select id, name, sets, reps, weight_kg, sort_order, created_at, updated_at
                from routine_exercises where routine_day_id = :dayId
                order by sort_order, id
                """)
                .param("dayId", dayId)
                .query((rs, row) -> new RoutineExerciseResponse(
                        rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("sets"),
                        rs.getInt("reps"), rs.getBigDecimal("weight_kg"), rs.getInt("sort_order"),
                        instant(rs, "created_at"), instant(rs, "updated_at")))
                .list();
    }

    private void validateDays(List<RoutineDayWriteRequest> days) {
        Set<Integer> numbers = new HashSet<>();
        for (RoutineDayWriteRequest day : days) {
            if (!numbers.add(day.dayNumber())) {
                throw ApiException.badRequest("DUPLICATE_DAY", "dayNumber must be unique within a routine");
            }
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    private record RoutineRow(UUID id, String name, String notes, Instant createdAt, Instant updatedAt) { }
    private record SnapshotRow(UUID id, UUID routineId, Instant createdAt) { }
}
