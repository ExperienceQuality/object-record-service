# Routine Service

Spring Boot service for user-owned workout routines, training days, exercises, and immutable exercise snapshots.

## Prerequisites

- Java 21
- Docker with the Docker Compose plugin
- PostgreSQL 18 (provided by Compose)

## Local development

Copy the environment template and choose a password:

```shell
cp .env.example .env
```

Start the service and its database:

```shell
./gradlew bootRun
```

Spring Boot starts PostgreSQL through Compose and Flyway applies the routine schema. No `init.sql` is needed. To manage the database separately:

```shell
docker compose up -d
docker compose down
```

Set `POSTGRES_PORT=0` to let Docker choose a free host port. Spring Boot discovers that port automatically; inspect it with `docker compose port postgres 5432`.

Without Compose integration, configure:

```shell
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/routine_service
SPRING_DATASOURCE_USERNAME=routine_service
SPRING_DATASOURCE_PASSWORD=replace-me
```

## API

Every domain request requires the trusted `X-User-Id` UUID header.

- `POST|GET /api/v1/routines`
- `GET|PUT /api/v1/routines/{routineId}`
- `POST|GET /api/v1/routines/{routineId}/snapshots`
- `GET /api/v1/routines/{routineId}/snapshots/{snapshotId}`

Routine writes treat days and exercises as one aggregate. `PUT` replaces the aggregate transactionally. Snapshot creation copies current exercise prescriptions, so later routine changes do not alter historical snapshots.

The OpenAPI 3.1 contract is [routine-service.yaml](src/main/resources/static/openapi/routine-service.yaml) and is served at `/openapi/routine-service.yaml`.

## Verification

Unit and integration tests use PostgreSQL 18 through Testcontainers. E2E tests target a running service:

```shell
./gradlew ci
XQORB_BASE_URI=http://localhost:8080 ./gradlew e2e
docker compose config
```

See [test coverage](docs/test-coverage.md) for the scenario matrix and known gaps.

## CI/CD

[CI](.github/workflows/ci.yml) runs for pull requests, pushes to `main`, manual dispatches, and release workflow calls. It validates the Gradle wrapper, invokes the Gradle `ci` task for unit/integration/OpenAPI verification and `packageService` JAR creation, starts PostgreSQL 18 and the packaged service, then invokes the Gradle `e2e` task. Reports, logs, and the JAR are retained as workflow artifacts.

[Release](.github/workflows/release.yml) runs for `v*` tags or manual dispatch. After the same CI gate passes, it publishes `ghcr.io/experiencequality/routine-service` with release and commit-SHA tags. Stable semantic versions also update `latest`. The workflow generates an SPDX SBOM and publishes provenance and SBOM attestations for the image.

Example release:

```shell
git tag v1.0.0
git push origin v1.0.0
```
