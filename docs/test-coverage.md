# Routine Service Test Coverage

## Automated coverage

| Layer | Covered behavior | Test location |
|---|---|---|
| Flyway/schema integration | PostgreSQL 18 starts from empty state; Flyway creates only the six routine-domain tables, three indexes, and schema history | `RoutineServiceApplicationTests` |
| OpenAPI contract | OpenAPI 3.1 parses and resolves without errors; all 4 paths, 7 operations, unique operation IDs, responses, and core schemas exist | `OpenApiContractTests` |
| HTTP integration | Create and replace a complete routine aggregate; create/read immutable snapshots; snapshot survives later routine changes | `RoutineApiIntegrationTests` |
| Identity isolation | Missing `X-User-Id` is `401`; malformed UUID is `400`; another user receives `404` | `RoutineApiIntegrationTests`, `RoutineBusinessE2ETest` |
| Validation | Duplicate day numbers, missing identity, and malformed identity are exercised; field constraints are declared with Bean Validation | `RoutineApiIntegrationTests`, `RoutineApi` |
| E2E business flow | Create/list/read routine; user isolation; aggregate replacement; create/list/read immutable snapshot | `src/e2e/java/test/RoutineBusinessE2ETest.java` using JVM Test Kit v2 |
| Runtime profiles | Production requires external datasource secrets; packaged e2e uses local fallbacks; integration tests use a separate Testcontainers-owned profile; all controlled profiles disable Docker Compose | `RenderBlueprintTests`, Spring integration tests |
| Render deployment | Blueprint parsing, pinned private image, explicit `prod` profile, free Singapore service, health check, external Neon secrets, and absence of a Render database | `RenderBlueprintTests` via `validateDeployment` |

## Acceptance commands

```shell
./gradlew ci
XQORB_BASE_URI=http://localhost:8080 ./gradlew e2e
docker compose config
```

E2E tests use `com.xq:jvm-test-kit:2.0.0` from GitHub Packages. Local runs
need `GITHUB_ACTOR` and `GITHUB_TOKEN` with package-read access. CI supplies
the same access through the workflow token.

## Deliberate boundaries and gaps

- No delete API is defined or tested.
- Lists are unpaginated in the initial implementation.
- Boundary cases for every individual request-field constraint are not yet exercised independently.
- The supplied schema has no version column, so concurrent replacement and optimistic-lock behavior are not part of this contract.
- Snapshots preserve exercise identity, name, sets, reps, and weight. They cannot preserve day assignment or sort order because those fields are absent from `snapshot_exercises`.
- Gateway authenticity is deployment infrastructure responsibility; the service validates only presence and UUID shape of `X-User-Id`.
- Database cascade behavior is constrained by PostgreSQL but does not have a public API scenario because deletion is not exposed.
