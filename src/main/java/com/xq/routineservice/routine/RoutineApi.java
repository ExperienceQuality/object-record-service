package com.xq.routineservice.routine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class RoutineApi {
    private RoutineApi() { }

    public record RoutineWriteRequest(
            @NotBlank String name,
            String notes,
            @NotNull List<@Valid RoutineDayWriteRequest> days) { }

    public record RoutineDayWriteRequest(
            @Min(1) @Max(7) int dayNumber,
            @NotBlank String name,
            @NotNull List<@Valid RoutineExerciseWriteRequest> exercises) { }

    public record RoutineExerciseWriteRequest(
            @NotBlank String name,
            @Positive int sets,
            @Positive int reps,
            @NotNull @DecimalMin("0.00") BigDecimal weightKg,
            @PositiveOrZero int sortOrder) { }

    public record RoutineResponse(
            UUID id, String name, String notes, Instant createdAt, Instant updatedAt,
            List<RoutineDayResponse> days) { }

    public record RoutineSummaryResponse(
            UUID id, String name, String notes, Instant createdAt, Instant updatedAt) { }

    public record RoutineDayResponse(
            UUID id, int dayNumber, String name, List<RoutineExerciseResponse> exercises) { }

    public record RoutineExerciseResponse(
            UUID id, String name, int sets, int reps, BigDecimal weightKg, int sortOrder,
            Instant createdAt, Instant updatedAt) { }

    public record RoutineSnapshotResponse(
            UUID id, UUID routineId, Instant createdAt, List<SnapshotExerciseResponse> exercises) { }

    public record RoutineSnapshotSummaryResponse(UUID id, UUID routineId, Instant createdAt) { }

    public record SnapshotExerciseResponse(
            UUID id, String exerciseKey, String name, int sets, int reps, BigDecimal weightKg) { }
}
