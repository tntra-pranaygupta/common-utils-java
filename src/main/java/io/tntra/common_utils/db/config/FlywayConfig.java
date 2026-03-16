package io.tntra.common_utils.db.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Production-grade Flyway configuration for the common-utils platform library.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li>We expose a custom {@link FlywayMigrationStrategy} bean so that each
 *       consuming service can override it if it needs repair, dry-run diff, or
 *       baseline behaviour without altering the shared library.</li>
 *   <li>Migration failures are logged at ERROR level <em>before</em> the
 *       exception propagates, which guarantees the correlation between the
 *       failed migration version and the startup log is captured even if the
 *       application log appender is not yet fully initialised.</li>
 *   <li>PCI/DSS note: migration scripts must never embed raw PAN or PII values.
 *       Use placeholder references to environment-specific secrets instead.</li>
 * </ul>
 *
 * <h2>Auto-configuration interaction</h2>
 * Spring Boot's {@code FlywayAutoConfiguration} picks up this bean and uses it
 * as the migration strategy, so no additional wiring is required in consuming
 * services.
 */
@Slf4j
@Configuration
public class FlywayConfig {


    /**
     * Default migration strategy: run pending migrations and log each version
     * that is applied.
     *
     * <p>Consuming services that need a different strategy (e.g., repair before
     * migrate, or a no-op in read-only replicas) should declare their own
     * {@link FlywayMigrationStrategy} bean — Spring Boot will prefer it over
     * this library-provided default.</p>
     *
     * @return a {@link FlywayMigrationStrategy} that logs before migrating
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Starting Flyway migration — pending scripts will be applied now");
            try {
                var result = flyway.migrate();
                if (result.migrationsExecuted == 0) {
                    log.info("Flyway: schema is already up-to-date, no migrations applied");
                } else {
                    log.info(
                            "Flyway: applied {} migration(s), schema now at version {}",
                            result.migrationsExecuted,
                            result.targetSchemaVersion
                    );
                }
            } catch (Exception ex) {
                log.error(
                        "Flyway migration FAILED — application will not start. "
                                + "Check db/migration scripts and datasource connectivity. Error: {}",
                        ex.getMessage(),
                        ex
                );
                throw ex;   // re-throw so Spring Boot aborts startup (fail-fast)
            }
        };
    }
}
