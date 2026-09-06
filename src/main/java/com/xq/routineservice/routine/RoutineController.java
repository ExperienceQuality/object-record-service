package com.xq.routineservice.routine;

import static com.xq.routineservice.routine.RoutineApi.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.xq.routineservice.shared.GatewayHeaders;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {
    private final RoutineService service;

    public RoutineController(RoutineService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<RoutineResponse> create(
            @RequestHeader(value = GatewayHeaders.USER, required = false) String user,
            @Valid @RequestBody RoutineWriteRequest request) {
        RoutineResponse response = service.create(GatewayHeaders.requireUserId(user), request);
        return ResponseEntity.created(URI.create("/api/v1/routines/" + response.id())).body(response);
    }

    @GetMapping
    public List<RoutineSummaryResponse> list(
            @RequestHeader(value = GatewayHeaders.USER, required = false) String user) {
        return service.list(GatewayHeaders.requireUserId(user));
    }

    @GetMapping("/{routineId}")
    public RoutineResponse get(
            @RequestHeader(value = GatewayHeaders.USER, required = false) String user,
            @PathVariable UUID routineId) {
        return service.get(GatewayHeaders.requireUserId(user), routineId);
    }

    @PutMapping("/{routineId}")
    public RoutineResponse replace(
            @RequestHeader(value = GatewayHeaders.USER, required = false) String user,
            @PathVariable UUID routineId,
            @Valid @RequestBody RoutineWriteRequest request) {
        return service.replace(GatewayHeaders.requireUserId(user), routineId, request);
    }

    @PostMapping("/{routineId}/snapshots")
    public ResponseEntity<RoutineSnapshotResponse> createSnapshot(
            @RequestHeader(value = GatewayHeaders.USER, required = false) String user,
            @PathVariable UUID routineId) {
        RoutineSnapshotResponse response = service.createSnapshot(GatewayHeaders.requireUserId(user), routineId);
        return ResponseEntity.created(URI.create(
                "/api/v1/routines/" + routineId + "/snapshots/" + response.id())).body(response);
    }

    @GetMapping("/{routineId}/snapshots")
    public List<RoutineSnapshotSummaryResponse> snapshots(
            @RequestHeader(value = GatewayHeaders.USER, required = false) String user,
            @PathVariable UUID routineId) {
        return service.listSnapshots(GatewayHeaders.requireUserId(user), routineId);
    }

    @GetMapping("/{routineId}/snapshots/{snapshotId}")
    public RoutineSnapshotResponse snapshot(
            @RequestHeader(value = GatewayHeaders.USER, required = false) String user,
            @PathVariable UUID routineId,
            @PathVariable UUID snapshotId) {
        return service.getSnapshot(GatewayHeaders.requireUserId(user), routineId, snapshotId);
    }
}
