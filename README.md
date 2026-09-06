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

The HTTP port defaults to `8080` locally. Hosting platforms can override it with the `PORT` environment variable.

## Configuration profiles

The executable artifact contains four deliberately separated Spring configurations:

- `application.yml` contains shared settings plus local-development datasource defaults and Docker Compose lifecycle management.
- `application-prod.yml` disables Docker Compose and requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. It contains no credential fallback values.
- `application-e2e.yml` disables Docker Compose and provides local-only datasource fallbacks for the packaged-service acceptance environment.
- `application-integration.yml` disables Docker Compose but defines no datasource. Spring integration tests activate it and let their `@ServiceConnection` Testcontainers instances supply all connection details.

Render sets `SPRING_PROFILES_ACTIVE=prod`. CI starts the packaged service with `SPRING_PROFILES_ACTIVE=e2e`. In-process Spring integration tests use `integration`, never `e2e`. Production credentials remain runtime environment variables and are never packaged in the application image or committed configuration.

## API

Every domain request requires the trusted `X-User-Id` UUID header.

- `POST|GET /api/v1/routines`
- `GET|PUT /api/v1/routines/{routineId}`
- `POST|GET /api/v1/routines/{routineId}/snapshots`
- `GET /api/v1/routines/{routineId}/snapshots/{snapshotId}`

Routine writes treat days and exercises as one aggregate. `PUT` replaces the aggregate transactionally. Snapshot creation copies current exercise prescriptions, so later routine changes do not alter historical snapshots.

The OpenAPI 3.1 contract is [routine-service.yaml](src/main/resources/static/openapi/routine-service.yaml) and is served at `/openapi/routine-service.yaml`.

## Verification

Unit and integration tests use PostgreSQL 18 through Testcontainers. E2E tests use JVM Test Kit v2 and target a running service:

```shell
./gradlew ci
XQORB_BASE_URI=http://localhost:8080 ./gradlew e2e
docker compose config
```

JVM Test Kit v2 is resolved from GitHub Packages. Local E2E dependency
resolution requires `GITHUB_ACTOR` and `GITHUB_TOKEN` with package-read access.

See [test coverage](docs/test-coverage.md) for the scenario matrix and known gaps.

## Render deployment

[`render.yaml`](render.yaml) defines a free Render web service in Singapore. It pulls the private, immutable image `ghcr.io/experiencequality/routine-service:0.1.0`, activates the `prod` profile, and connects to an existing Neon PostgreSQL 18 database; it does not provision a Render database.

Before creating the Blueprint:

1. Publish `v0.1.0` and verify that GHCR contains the `0.1.0` image.
2. In Render, create a private registry credential named `xq-ghcr` using a GitHub token with `read:packages` access.
3. Confirm the Neon project is in Singapore, uses PostgreSQL 18, and is empty or already managed by this service's Flyway history.
4. Select `render.yaml` when creating the Render Blueprint.
5. Enter the prompted `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` values. Use Neon's direct Java/JDBC connection URL with TLS rather than the pooled endpoint, because Flyway runs during application startup.
6. Wait for `/actuator/health` to pass, then exercise a create/read routine smoke test with `X-User-Id`.

The Render and Neon free tiers have a base cost of $0 while usage remains inside their allowances. Render spins the service down after 15 minutes without inbound traffic and documents a wake-up time of about one minute. Neon also scales idle compute to zero, so clients should allow up to 90 seconds for the first request after inactivity. These free tiers are intended for evaluation, not availability-sensitive production use.

Future releases are manual: publish a new semantic image tag, update the pinned image in `render.yaml`, run `./gradlew validateDeployment`, and sync the Blueprint. Do not deploy `latest`; the pinned tag keeps promotion and rollback explicit.

## CI/CD

[CI](.github/workflows/ci.yml) runs for pull requests, pushes to `main`, manual dispatches, and release workflow calls. It validates the Gradle wrapper, invokes the Gradle `ci` task for unit/integration/OpenAPI verification and `packageService` creation of the stable `build/service/routine-service.jar` artifact, starts PostgreSQL 18 and that exact packaged service, then invokes the Gradle `e2e` task. Reports, logs, and the packaged JAR are retained as workflow artifacts.

[Release](.github/workflows/release.yml) runs for `v*` tags or manual dispatch. After the same CI gate passes, it publishes `ghcr.io/experiencequality/routine-service` with release and commit-SHA tags. Stable semantic versions also update `latest`. The workflow generates an SPDX SBOM and publishes provenance and SBOM attestations for the image.

Example release:

```shell
git tag v0.1.0
git push origin v0.1.0
```
