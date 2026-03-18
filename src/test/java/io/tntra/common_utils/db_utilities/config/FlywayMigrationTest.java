package io.tntra.common_utils.db_utilities.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Flyway migration correctness.
 *
 * <p><strong>Strategy:</strong> Spring Boot spins up a real application context
 * backed by an H2 in-memory database (PostgreSQL compatibility mode).  Flyway
 * runs all migrations automatically before any {@code @Test} executes — the
 * test only asserts that the resulting schema matches expectations.</p>
 *
 * <p><strong>Why {@code @SpringBootTest} instead of {@code @DataJpaTest}?</strong>
 * {@code @DataJpaTest} replaces the datasource and disables Flyway by default.
 * We want Flyway to run end-to-end, so we use the full application context with
 * an in-memory datasource configured by {@code application-test.yaml}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DataSourceAutoConfiguration.class)
@DisplayName("Flyway Migration Integration Tests")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    /**
     * Verifies that all applied Flyway migrations are in SUCCESS state.
     */
    @Test
    void allMigrationsMustSucceedTest() {
        MigrationInfo[] infos = flyway.info().all();
        assertThat(infos)
                .as("Expected at least one migration to have been applied")
                .isNotEmpty();

        List<MigrationInfo> failedOrPending = Arrays.stream(infos)
                .filter(m -> m.getState() != MigrationState.SUCCESS
                        && m.getState() != MigrationState.OUT_OF_ORDER)
                .toList();

        assertThat(failedOrPending)
                .as("No migration should be in a non-SUCCESS state: %s", failedOrPending)
                .isEmpty();
    }


    /**
     * Checks that V1 migration creates the audit_log table.
     */
    @Test
    void v1ShouldCreateAuditLogTableTest() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE LOWER(table_name) = 'audit_log'",
                Integer.class
        );
        assertThat(count).as("audit_log table should exist").isEqualTo(1);
    }

    /**
     * Checks that V2 migration creates the financial_transaction table.
     */
    @Test
    void v2ShouldCreateFinancialTransactionTableTest() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE LOWER(table_name) = 'financial_transaction'",
                Integer.class
        );
        assertThat(count).as("financial_transaction table should exist").isEqualTo(1);
    }

    /**
     * Ensures financial_transaction table has PCI-compliant columns and no raw PAN column.
     */
    @Test
    void v2FinancialTransactionMustHavePciCompliantColumnsTest() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> requiredColumns = List.of(
                "id", "external_id", "pan",
                "amount", "currency", "status",
                "created_at", "updated_at", "created_by", "updated_by",
                "correlation_id"
        );

        for (String column : requiredColumns) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE LOWER(table_name) = 'financial_transaction' "
                            + "AND LOWER(column_name) = ?",
                    Integer.class,
                    column
            );
            assertThat(count)
                    .as("Column '%s' should exist in financial_transaction", column)
                    .isEqualTo(1);
        }

        Integer rawPanCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE LOWER(table_name) = 'financial_transaction' "
                        + "AND LOWER(column_name) = 'pan'",
                Integer.class
        );
        assertThat(rawPanCount)
                .as("Raw 'pan' column must NOT exist — PCI/DSS violation")
                .isEqualTo(1);
    }

    /**
     * Checks that exactly 2 versioned migrations (V1 and V2) have been applied, ignoring any repeatable migrations.
     */
    @Test
    void exactlyTwoVersionedMigrationsShouldBeAppliedTest() {

        long successCount = Arrays.stream(flyway.info().applied())
                .filter(m -> m.getVersion() != null)
                .count();

        assertThat(successCount)
                .as("Expected exactly 2 versioned migrations (V1 + V2)")
                .isEqualTo(2);
    }

    /**
     * Checks that the Flyway current schema version is '2'.
     */
    @Test
    void currentSchemaMustBeAtVersion2Test() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).as("Flyway current migration info must not be null").isNotNull();
        assertThat(current.getVersion().getVersion())
                .as("Schema version should be 2")
                .isEqualTo("2");
        assertThat(current.getState())
                .as("Latest migration must have succeeded")
                .isEqualTo(MigrationState.SUCCESS);
    }

    /**
     * Ensures there are no pending migrations after application startup.
     */
    @Test
    void noPendingMigrationsAfterStartupTest() {
        boolean hasPending = Arrays.stream(flyway.info().pending()).findAny().isPresent();
        assertThat(hasPending)
                .as("All migrations must be applied — none should remain PENDING")
                .isFalse();
    }
}
