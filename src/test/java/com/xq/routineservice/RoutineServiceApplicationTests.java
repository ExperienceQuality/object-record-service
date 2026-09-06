package com.xq.routineservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
class RoutineServiceApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-bookworm"));

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void flywayCreatesRoutineSchema() {
        List<String> tableNames = jdbcClient.sql("""
                        SELECT tablename
                        FROM pg_catalog.pg_tables
                        WHERE schemaname = 'public'
                        ORDER BY tablename
                        """)
                .query(String.class)
                .list();

        assertThat(tableNames).containsExactlyInAnyOrder(
                "flyway_schema_history",
                "users",
                "routines",
                "routine_days",
                "routine_exercises",
                "routine_snapshots",
                "snapshot_exercises");

        List<String> indexNames = jdbcClient.sql("""
                        SELECT indexname
                        FROM pg_catalog.pg_indexes
                        WHERE schemaname = 'public'
                        """)
                .query(String.class)
                .list();

        assertThat(indexNames).contains(
                "routines_user_idx",
                "snapshots_routine_created_idx",
                "exercises_day_order_idx");
    }
}
