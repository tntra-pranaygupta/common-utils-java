package io.tntra.common_utils.db_utilities.config;

import io.tntra.common_utils.db_utilities.auditing.DefaultAuditorAware;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaAuditingConfigTest {
    /**
     * Should create an AuditorAware bean of type DefaultAuditorAware.
     */
    @Test
    void shouldCreateAuditorAwareBeanTest() {

        JpaAuditingConfig config = new JpaAuditingConfig();

        AuditorAware<String> auditorAware = config.auditorAware();

        assertThat(auditorAware).isNotNull();
        assertThat(auditorAware).isInstanceOf(DefaultAuditorAware.class);
    }
}
