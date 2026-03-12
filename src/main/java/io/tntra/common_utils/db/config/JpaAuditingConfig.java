package io.tntra.common_utils.db.config;

import io.tntra.common_utils.db.auditing.DefaultAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing and provides a default auditor.
 *
 * <p>Services may override the {@link AuditorAware} bean with their own implementation
 * (e.g., extracting a user id from JWT / mTLS identity).</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new DefaultAuditorAware();
    }
}

